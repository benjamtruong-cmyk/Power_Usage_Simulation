/**
 * Simulator.java
 * Orchestrates the power usage simulation over a user-defined number of timesteps.
 * Implements the two-phase power control algorithm:
 *   Phase 1: Throttle smart appliances from ON to LOW (greedy, descending wattage)
 *   Phase 2: Brown out locations (descending by current total wattage, last resort)
 *
 * Design rationale:
 *   - Phase 1 sorts ON smart appliances by descending power savings, maximizing
 *     wattage reduction per appliance affected.
 *   - Phase 2 sorts locations by DESCENDING current wattage draw, so the location
 *     consuming the most power is browned out first. This maximizes watts freed per
 *     brownout event, minimizing the total number of locations taken offline.
 *
 *     Why NOT ascending appliance count (previous approach):
 *     ApplianceGenerator assigns 15-20 appliances per location, making counts nearly
 *     identical across all locations. Sorting by count in that scenario is effectively
 *     arbitrary — the difference between 15 and 18 appliances is trivial. Current
 *     wattage, by contrast, reflects actual mix of devices (a location with ACs and
 *     dryers vs. one with clocks and answering machines can differ by thousands of
 *     watts), making it a meaningful and impactful sort key.
 *   - No user interaction during simulation (FAQ #13).
 *   - Detailed report written to a text file; summary printed to console.
 */

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Simulator {

    private PowerGrid grid;
    private int totalTimesteps;

    // Tracking variables for the summary report
    private int[] smartSwitchedPerStep;     // # smart appliances switched to LOW each step
    private int[] locationsBrownedPerStep;  // # locations browned out each step
    private ArrayList<String> detailedLog;  // Detailed log entries for file output

    /**
     * Constructs a Simulator.
     *
     * @param grid           the PowerGrid containing all locations and appliances
     * @param totalTimesteps number of timesteps to simulate
     */
    public Simulator(PowerGrid grid, int totalTimesteps) {
        this.grid = grid;
        this.totalTimesteps = totalTimesteps;
        this.smartSwitchedPerStep = new int[totalTimesteps];
        this.locationsBrownedPerStep = new int[totalTimesteps];
        this.detailedLog = new ArrayList<>();
    }

    /**
     * Runs the full simulation for all timesteps.
     * For each timestep:
     *   1. Reset all locations and randomly activate appliances
     *   2. Check total power against warning level
     *   3. If over, execute the two-phase power control algorithm
     *   4. Record results for both screen and file output
     */
    public void runSimulation() {
        System.out.println("\n========== SIMULATION STARTING ==========");
        System.out.println("Warning Level: " + grid.getWarningLevel() + "W");
        System.out.println("Timesteps: " + totalTimesteps);
        System.out.println("Total appliances: " + grid.getAllAppliances().size());
        System.out.println("Total locations: " + grid.getLocations().size());
        System.out.println("==========================================\n");

        for (int step = 0; step < totalTimesteps; step++) {
            simulateTimestep(step);
        }

        // After all timesteps: print summary and write detailed report
        printSummaryReport();
        writeDetailedReport("simulation_report.txt");
    }

    /**
     * Simulates a single timestep.
     *
     * @param step the zero-based timestep index
     */
    private void simulateTimestep(int step) {
        // ── Step 1: Reset and randomly activate appliances ───────────
        // Reset brownout flags for all locations
        for (Location loc : grid.getLocations().values()) {
            loc.resetBrownout();
        }

        // Determine ON/OFF for each appliance based on probability (FAQ #6)
        for (Appliance app : grid.getAllAppliances()) {
            app.determineOnOff();
        }

        // ── Step 2: Calculate total power ────────────────────────────
        int totalPower = grid.getTotalPower();
        int warningLevel = grid.getWarningLevel();

        StringBuilder stepLog = new StringBuilder();
        stepLog.append("--- Timestep " + (step + 1) + " ---\n");
        stepLog.append("Total power before algorithm: " + totalPower + "W\n");

        // ── Step 3: Run power control if over warning level ─────────
        int smartSwitched = 0;
        int locationsBrowned = 0;
        ArrayList<String> affectedAppliances = new ArrayList<>();
        ArrayList<String> affectedLocations = new ArrayList<>();

        if (totalPower > warningLevel) {
            int excess = totalPower - warningLevel;
            stepLog.append("OVER WARNING LEVEL by " + excess + "W. Running power control...\n");

            // ── Phase 1: Smart appliance throttling ──────────────────
            // Collect all ON smart appliances
            ArrayList<SmartAppliance> onSmartApps = new ArrayList<>();
            for (Appliance app : grid.getAllAppliances()) {
                if (app.isSmart() && app.getStatus().equals("ON")) {
                    onSmartApps.add((SmartAppliance) app);
                }
            }

            // Sort by descending savings (most power saved per switch first)
            Collections.sort(onSmartApps, new Comparator<SmartAppliance>() {
                @Override
                public int compare(SmartAppliance a, SmartAppliance b) {
                    return Integer.compare(b.getSavings(), a.getSavings());
                }
            });

            // Switch smart appliances to LOW until under warning level
            for (SmartAppliance sa : onSmartApps) {
                if (grid.getTotalPower() <= warningLevel) break;

                int savings = sa.getSavings();
                sa.setStatus("LOW");
                smartSwitched++;
                affectedAppliances.add("  THROTTLED: " + sa);
                stepLog.append("  Throttled smart appliance ID:" + sa.getApplianceID() +
                               " (" + sa.getType() + ") saved " + savings + "W\n");
            }

            // ── Phase 2: Location brownout (if still over) ──────────
            if (grid.getTotalPower() > warningLevel) {
                stepLog.append("  Phase 1 insufficient. Initiating brownouts...\n");

                // Collect non-browned-out locations that have ON appliances
                ArrayList<Location> candidateLocations = new ArrayList<>();
                for (Location loc : grid.getLocations().values()) {
                    if (!loc.isBrownedOut() && loc.getTotalPower() > 0) {
                        candidateLocations.add(loc);
                    }
                }

                // Sort by DESCENDING current wattage (maximize power freed per brownout).
                //
                // Rationale: ApplianceGenerator creates locations with 15-20 appliances each,
                // making appliance-count sorting nearly arbitrary (15 vs 18 is trivial).
                // Wattage varies dramatically — a location with ACs and dryers can draw
                // 8,000W+ while one with clocks and fans draws under 200W.
                //
                // By browning out the highest-draw location first, we mirror Phase 1's
                // greedy philosophy: maximize reduction per action, minimizing the total
                // number of locations taken offline to reach the warning level.
                Collections.sort(candidateLocations, new Comparator<Location>() {
                    @Override
                    public int compare(Location a, Location b) {
                        return Integer.compare(b.getTotalPower(), a.getTotalPower());
                    }
                });

                // Brown out locations until under warning level
                for (Location loc : candidateLocations) {
                    if (grid.getTotalPower() <= warningLevel) break;

                    int powerBefore = loc.getTotalPower(); // capture before brownout changes it
                    int freed = loc.brownOut();
                    locationsBrowned++;
                    affectedLocations.add("  BROWNED OUT: Location " + loc.getLocationID() +
                                          " (" + loc.getApplianceCount() + " appliances, " +
                                          powerBefore + "W draw, freed " + freed + "W)");
                    stepLog.append("  Browned out location " + loc.getLocationID() +
                                   " (" + powerBefore + "W freed)\n");
                }
            }
        } else {
            stepLog.append("Power within limits. No action needed.\n");
        }

        // ── Step 4: Record results ───────────────────────────────────
        int finalPower = grid.getTotalPower();
        smartSwitchedPerStep[step] = smartSwitched;
        locationsBrownedPerStep[step] = locationsBrowned;

        // Print per-timestep summary to screen (required output)
        System.out.printf("Timestep %3d: Smart->LOW: %d | Locations browned out: %d | " +
                          "Final power: %dW / %dW%n",
                          step + 1, smartSwitched, locationsBrowned,
                          finalPower, warningLevel);

        // Build detailed log for file output
        stepLog.append("Final power after algorithm: " + finalPower + "W\n");
        for (String s : affectedAppliances) stepLog.append(s + "\n");
        for (String s : affectedLocations)  stepLog.append(s + "\n");
        stepLog.append("\n");
        detailedLog.add(stepLog.toString());
    }

    /**
     * Prints a summary report to the screen after the simulation completes.
     * Includes: total locations affected per interval and max affected location.
     */
    private void printSummaryReport() {
        System.out.println("\n========== SIMULATION SUMMARY ==========");

        int totalSmartSwitched = 0;
        int totalBrownouts = 0;
        int maxBrownoutsInStep = 0;
        int maxBrownoutStep = 0;

        for (int i = 0; i < totalTimesteps; i++) {
            totalSmartSwitched += smartSwitchedPerStep[i];
            totalBrownouts += locationsBrownedPerStep[i];
            if (locationsBrownedPerStep[i] > maxBrownoutsInStep) {
                maxBrownoutsInStep = locationsBrownedPerStep[i];
                maxBrownoutStep = i + 1;
            }
        }

        System.out.println("Total timesteps simulated: " + totalTimesteps);
        System.out.println("Total smart appliances throttled (all steps): " + totalSmartSwitched);
        System.out.println("Total location brownouts (all steps): " + totalBrownouts);
        System.out.println("Max locations browned out in a single step: " + maxBrownoutsInStep +
                           " (at timestep " + maxBrownoutStep + ")");

        // Per-interval breakdown
        System.out.println("\nPer-Timestep Breakdown:");
        System.out.printf("%-10s %-20s %-20s%n", "Step", "Smart->LOW", "Locations Browned");
        System.out.println("--------------------------------------------------");
        for (int i = 0; i < totalTimesteps; i++) {
            if (smartSwitchedPerStep[i] > 0 || locationsBrownedPerStep[i] > 0) {
                System.out.printf("%-10d %-20d %-20d%n",
                                  i + 1, smartSwitchedPerStep[i], locationsBrownedPerStep[i]);
            }
        }
        System.out.println("==========================================");
    }

    /**
     * Writes the detailed report to a text file.
     * Contains the appliances and locations affected during each interval.
     *
     * @param filename the output file name
     */
    private void writeDetailedReport(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("POWER USAGE SIMULATION - DETAILED REPORT");
            writer.println("Warning Level: " + grid.getWarningLevel() + "W");
            writer.println("Total Timesteps: " + totalTimesteps);
            writer.println("Total Appliances: " + grid.getAllAppliances().size());
            writer.println("Total Locations: " + grid.getLocations().size());
            writer.println("========================================\n");

            for (String entry : detailedLog) {
                writer.print(entry);
            }

            writer.println("\n========== END OF REPORT ==========");
            System.out.println("\nDetailed report saved to: " + filename);

        } catch (IOException e) {
            System.out.println("Error writing report: " + e.getMessage());
        }
    }
}
