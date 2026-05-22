/**
 * PowerGrid.java
 * Manages the entire collection of locations and their appliances.
 * Provides CRUD operations (add, delete, find, list) and CSV file loading.
 *
 * Design rationale:
 *   - Uses a HashMap<Integer, Location> for O(1) location lookup by ID.
 *   - Appliances are always accessed through their parent Location,
 *     maintaining the location-appliance containment hierarchy.
 *   - File reading parses the CSV format specified in the assignment:
 *     locationID,name,onWattage,probOn,isSmart,reductionPercent
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class PowerGrid {

    private HashMap<Integer, Location> locations;  // Map of locationID -> Location
    private int warningLevel;                      // Max allowed wattage before algorithm kicks in

    /**
     * Constructs an empty PowerGrid.
     *
     * @param warningLevel the total allowed wattage threshold
     */
    public PowerGrid(int warningLevel) {
        this.locations = new HashMap<>();
        this.warningLevel = warningLevel;
    }

    // ── Accessors ────────────────────────────────────────────────────

    public HashMap<Integer, Location> getLocations()   { return locations; }
    public int getWarningLevel()                       { return warningLevel; }
    public void setWarningLevel(int warningLevel)      { this.warningLevel = warningLevel; }

    // ── Appliance CRUD ───────────────────────────────────────────────

    /**
     * Adds an appliance to the grid. If its location doesn't exist yet,
     * a new Location is created automatically.
     *
     * @param app the appliance to add
     */
    public void addAppliance(Appliance app) {
        int locID = app.getLocationID();
        if (!locations.containsKey(locID)) {
            locations.put(locID, new Location(locID));
        }
        locations.get(locID).addAppliance(app);
    }

    /**
     * Deletes an appliance by its unique system-generated ID.
     * Searches all locations to find and remove the appliance.
     * If the location becomes empty after removal, it is also removed.
     *
     * @param applianceID the unique ID of the appliance to delete
     * @return true if the appliance was found and deleted
     */
    public boolean deleteAppliance(int applianceID) {
        for (Integer locID : new ArrayList<>(locations.keySet())) {
            Location loc = locations.get(locID);
            if (loc.removeAppliance(applianceID)) {
                if (loc.getApplianceCount() == 0) {
                    locations.remove(locID);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Finds an appliance by its unique ID across all locations.
     *
     * @param applianceID the unique ID to search for
     * @return the Appliance if found, null otherwise
     */
    public Appliance findAppliance(int applianceID) {
        for (Location loc : locations.values()) {
            Appliance found = loc.findAppliance(applianceID);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Lists all appliances across all locations to the console.
     */
    public void listAppliances() {
        if (locations.isEmpty()) {
            System.out.println("No appliances in the system.");
            return;
        }
        int totalCount = 0;
        for (Location loc : locations.values()) {
            System.out.println("--- " + loc + " ---");
            for (Appliance app : loc.getAppliances()) {
                System.out.println("  " + app);
                totalCount++;
            }
        }
        System.out.println("Total appliances: " + totalCount +
                           " across " + locations.size() + " locations.");
    }

    // ── File loading ─────────────────────────────────────────────────

    /**
     * Reads appliances from a comma-separated text file.
     * Expected format per line:
     *   locationID,applianceName,onWattage,probOn,isSmart,reductionPercent
     *
     * Example:
     *   10000001,Clothes Washer,1200,0.025,true,0.25
     *
     * Lines that don't conform to the expected format are skipped with a warning.
     *
     * @param filename path to the CSV file
     * @return the number of appliances successfully loaded
     */
    public int readAppFile(String filename) {
        int count = 0;
        Scanner scan = null;
        try {
            File myFile = new File(filename);
            scan = new Scanner(myFile);

            while (scan.hasNextLine()) {
                String line = scan.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length < 6) {
                    System.out.println("Warning: Skipping malformed line: " + line);
                    continue;
                }

                try {
                    int locationID = Integer.parseInt(parts[0].trim());
                    String appName = parts[1].trim();
                    int onWattage = Integer.parseInt(parts[2].trim());
                    double probOn = Double.parseDouble(parts[3].trim());
                    boolean isSmart = Boolean.parseBoolean(parts[4].trim());
                    double reductionPct = Double.parseDouble(parts[5].trim());

                    // Input validation (FAQ #8): probabilities [0,1], wattage >= 0
                    if (probOn < 0 || probOn > 1) {
                        System.out.println("Warning: Invalid probability (" + probOn +
                                           ") for " + appName + ". Skipping.");
                        continue;
                    }
                    if (onWattage < 0) {
                        System.out.println("Warning: Negative wattage for " + appName +
                                           ". Skipping.");
                        continue;
                    }

                    Appliance app;
                    if (isSmart) {
                        app = new SmartAppliance(locationID, appName, onWattage,
                                                probOn, reductionPct);
                    } else {
                        app = new RegularAppliance(locationID, appName, onWattage, probOn);
                    }
                    addAppliance(app);
                    count++;

                } catch (NumberFormatException e) {
                    System.out.println("Warning: Number format error in line: " + line);
                }
            }

        } catch (IOException ioe) {
            System.out.println("Error: The file '" + filename + "' could not be read.");
        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
        } finally {
            if (scan != null) {
                scan.close();
            }
        }

        System.out.println("Successfully loaded " + count + " appliances from " + filename);
        return count;
    }

    /**
     * Collects all appliances across all locations into a single list.
     * Useful for the simulation algorithm when iterating over all appliances.
     *
     * @return ArrayList of all appliances in the grid
     */
    public ArrayList<Appliance> getAllAppliances() {
        ArrayList<Appliance> all = new ArrayList<>();
        for (Location loc : locations.values()) {
            all.addAll(loc.getAppliances());
        }
        return all;
    }

    /**
     * Calculates the total current power draw across the entire grid.
     *
     * @return total wattage consumed by all ON/LOW appliances
     */
    public int getTotalPower() {
        int total = 0;
        for (Location loc : locations.values()) {
            total += loc.getTotalPower();
        }
        return total;
    }
}
