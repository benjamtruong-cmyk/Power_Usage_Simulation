/**
 * AppClient.java
 * The main client class for the Power Usage Simulation System.
 * Provides an interactive menu for users to manage appliances and run simulations.
 *
 * Based on the stub code provided with the assignment.
 * Menu options:
 *   A - Add an appliance manually
 *   D - Delete an appliance by ID
 *   L - List all appliances
 *   F - Read appliances from a CSV file
 *   S - Start the simulation
 *   Q - Quit the program
 *
 * Design rationale:
 *   - All add/delete operations must happen BEFORE the simulation starts (FAQ #7).
 *   - Input validation re-prompts for invalid entries (assignment spec, section 6).
 *   - The warning level is collected at program start and must be > 0 (FAQ #5).
 */

import java.util.Scanner;

public class AppClient {

    private static Scanner scan = new Scanner(System.in);
    private static PowerGrid grid;

    public static void main(String[] args) {

        System.out.println("===========================================");
        System.out.println("  POWER USAGE SIMULATION SYSTEM");
        System.out.println("  CS 1400 - Group Assignment 3");
        System.out.println("===========================================\n");

        // ── Prompt for warning level (must be > 0, FAQ #5) ───────────
        int warningLevel = 0;
        while (warningLevel <= 0) {
            System.out.print("Enter the total allowed wattage (warning level, > 0): ");
            try {
                warningLevel = Integer.parseInt(scan.nextLine().trim());
                if (warningLevel <= 0) {
                    System.out.println("Error: Wattage must be greater than 0. Try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid integer. Try again.");
            }
        }

        grid = new PowerGrid(warningLevel);
        System.out.println("Warning level set to " + warningLevel + "W.\n");

        // ── Main menu loop ───────────────────────────────────────────
        String option;
        while (true) {
            System.out.println("\nSelect an option:");
            System.out.println("Type \"A\" Add an appliance");
            System.out.println("Type \"D\" Delete an appliance");
            System.out.println("Type \"L\" List the appliances");
            System.out.println("Type \"F\" Read Appliances from a file");
            System.out.println("Type \"S\" To Start the simulation");
            System.out.println("Type \"Q\" Quit the program");
            System.out.print(">> ");
            option = scan.nextLine().trim().toUpperCase();

            switch (option) {
                case "A":
                    addApplianceManual();
                    break;
                case "D":
                    deleteAppliance();
                    break;
                case "L":
                    grid.listAppliances();
                    break;
                case "F":
                    readFromFile();
                    break;
                case "S":
                    startSimulation();
                    break;
                case "Q":
                    System.out.println("Exiting the program. Goodbye!");
                    scan.close();
                    return;
                default:
                    System.out.println("Invalid option. Please select A, D, L, F, S, or Q.");
            }
        }
    }

    /**
     * Adds an appliance manually by prompting for each attribute.
     * Validates all inputs per FAQ #8:
     *   - Probabilities must be in [0, 1]
     *   - Wattage must be >= 0
     */
    private static void addApplianceManual() {
        System.out.println("\n--- Add Appliance ---");

        // Location ID
        int locationID = readPositiveInt("Enter 8-digit location ID: ");

        // Appliance name
        System.out.print("Enter appliance name: ");
        String name = scan.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Error: Name cannot be empty.");
            return;
        }

        // ON wattage
        int onWattage = -1;
        while (onWattage < 0) {
            System.out.print("Enter ON wattage (>= 0): ");
            try {
                onWattage = Integer.parseInt(scan.nextLine().trim());
                if (onWattage < 0) {
                    System.out.println("Error: Wattage must be >= 0.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid integer.");
            }
        }

        // Probability of being ON
        double probOn = -1;
        while (probOn < 0 || probOn > 1) {
            System.out.print("Enter probability of being ON (0.0 to 1.0): ");
            try {
                probOn = Double.parseDouble(scan.nextLine().trim());
                if (probOn < 0 || probOn > 1) {
                    System.out.println("Error: Probability must be between 0.0 and 1.0.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid decimal number.");
            }
        }

        // Smart or Regular?
        System.out.print("Is this a smart appliance? (true/false): ");
        boolean isSmart = Boolean.parseBoolean(scan.nextLine().trim());

        Appliance app;
        if (isSmart) {
            double reductionPct = -1;
            while (reductionPct < 0 || reductionPct > 1) {
                System.out.print("Enter power reduction percentage (0.0 to 1.0): ");
                try {
                    reductionPct = Double.parseDouble(scan.nextLine().trim());
                    if (reductionPct < 0 || reductionPct > 1) {
                        System.out.println("Error: Reduction must be between 0.0 and 1.0.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Error: Please enter a valid decimal number.");
                }
            }
            app = new SmartAppliance(locationID, name, onWattage, probOn, reductionPct);
        } else {
            app = new RegularAppliance(locationID, name, onWattage, probOn);
        }

        grid.addAppliance(app);
        System.out.println("Appliance added: " + app);
    }

    /**
     * Deletes an appliance by its unique system-generated ID.
     */
    private static void deleteAppliance() {
        System.out.println("\n--- Delete Appliance ---");
        int id = readPositiveInt("Enter the appliance ID to delete: ");

        Appliance found = grid.findAppliance(id);
        if (found != null) {
            System.out.println("Found: " + found);
            System.out.print("Confirm deletion? (yes/no): ");
            String confirm = scan.nextLine().trim().toLowerCase();
            if (confirm.equals("yes") || confirm.equals("y")) {
                grid.deleteAppliance(id);
                System.out.println("Appliance deleted.");
            } else {
                System.out.println("Deletion cancelled.");
            }
        } else {
            System.out.println("Appliance with ID " + id + " not found.");
        }
    }

    /**
     * Reads appliances from a CSV file specified by the user.
     */
    private static void readFromFile() {
        System.out.print("Enter the path to the appliance CSV file: ");
        String filename = scan.nextLine().trim();
        if (filename.isEmpty()) {
            System.out.println("Error: Filename cannot be empty.");
            return;
        }
        grid.readAppFile(filename);
    }

    /**
     * Starts the simulation after prompting for the number of timesteps.
     * Validates that at least one appliance is loaded and timesteps > 0.
     */
    private static void startSimulation() {
        if (grid.getAllAppliances().isEmpty()) {
            System.out.println("Error: No appliances loaded. Add appliances or read from a file first.");
            return;
        }

        int timesteps = 0;
        while (timesteps <= 0) {
            System.out.print("Enter the number of simulation timesteps (> 0): ");
            try {
                timesteps = Integer.parseInt(scan.nextLine().trim());
                if (timesteps <= 0) {
                    System.out.println("Error: Must be a positive integer.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid integer.");
            }
        }

        Simulator sim = new Simulator(grid, timesteps);
        sim.runSimulation();
    }

    /**
     * Utility: reads a positive integer from the user with re-prompting.
     */
    private static int readPositiveInt(String prompt) {
        int value = -1;
        while (value <= 0) {
            System.out.print(prompt);
            try {
                value = Integer.parseInt(scan.nextLine().trim());
                if (value <= 0) {
                    System.out.println("Error: Must be a positive integer.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid integer.");
            }
        }
        return value;
    }
}
