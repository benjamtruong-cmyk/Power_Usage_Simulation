/**
 * Location.java
 * Represents a physical location (room) in the building, identified by an
 * 8-digit numeric account number. Each location contains a collection of
 * appliances that can be managed collectively (e.g., during a brownout).
 *
 * Design rationale:
 *   - Locations are the unit of brownout: when a location is browned out,
 *     ALL appliances at that location are set to OFF (FAQ #17, #22).
 *   - The class tracks how many times it has been browned out for reporting.
 *   - Appliances are stored in an ArrayList for flexible add/delete operations.
 */

import java.util.ArrayList;

public class Location {

    private int locationID;                    // 8-digit numeric account number
    private ArrayList<Appliance> appliances;   // All appliances at this location
    private boolean brownedOut;                // Whether this location is currently browned out

    /**
     * Constructs an empty Location.
     *
     * @param locationID 8-digit account number identifying this location
     */
    public Location(int locationID) {
        this.locationID = locationID;
        this.appliances = new ArrayList<>();
        this.brownedOut = false;
    }

    // ── Accessors ────────────────────────────────────────────────────

    public int getLocationID()                 { return locationID; }
    public ArrayList<Appliance> getAppliances() { return appliances; }
    public boolean isBrownedOut()              { return brownedOut; }
    public int getApplianceCount()             { return appliances.size(); }

    // ── Appliance management ─────────────────────────────────────────

    /**
     * Adds an appliance to this location.
     */
    public void addAppliance(Appliance app) {
        appliances.add(app);
    }

    /**
     * Removes an appliance from this location by its unique ID.
     *
     * @param applianceID the system-generated ID of the appliance to remove
     * @return true if the appliance was found and removed
     */
    public boolean removeAppliance(int applianceID) {
        for (int i = 0; i < appliances.size(); i++) {
            if (appliances.get(i).getApplianceID() == applianceID) {
                appliances.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Finds an appliance at this location by its unique ID.
     *
     * @param applianceID the system-generated ID
     * @return the Appliance if found, null otherwise
     */
    public Appliance findAppliance(int applianceID) {
        for (Appliance app : appliances) {
            if (app.getApplianceID() == applianceID) {
                return app;
            }
        }
        return null;
    }

    // ── Brownout operations ──────────────────────────────────────────

    /**
     * Executes a brownout on this location: sets ALL appliances to OFF.
     * This is the only mechanism by which Regular appliances can be turned off,
     * and the only way Smart appliances can move from ON/LOW to OFF.
     *
     * @return the total wattage freed by the brownout
     */
    public int brownOut() {
        int freedWattage = 0;
        for (Appliance app : appliances) {
            freedWattage += app.getPower(); // Capture current draw before turning off
            app.setStatus("OFF");
        }
        brownedOut = true;
        return freedWattage;
    }

    /**
     * Resets the brownout flag for a new timestep.
     * Called at the start of each simulation step before appliances are re-activated.
     */
    public void resetBrownout() {
        brownedOut = false;
    }

    /**
     * Calculates the total current power draw of all appliances at this location.
     *
     * @return sum of all appliance power draws
     */
    public int getTotalPower() {
        int total = 0;
        for (Appliance app : appliances) {
            total += app.getPower();
        }
        return total;
    }

    @Override
    public String toString() {
        return String.format("Location %d: %d appliances, Total Power: %dW, Browned Out: %s",
                locationID, appliances.size(), getTotalPower(), brownedOut);
    }
}
