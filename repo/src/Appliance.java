/**
 * Appliance.java
 * Abstract superclass for all appliances in the Power Usage Simulation System.
 * Defines common attributes shared by both Regular and Smart appliances,
 * and declares abstract/concrete methods for power state management.
 *
 * Design rationale:
 *   - A static counter generates unique system-wide IDs (FAQ #10).
 *   - The 'status' field tracks ON/LOW/OFF state per timestep.
 *   - getPower() is abstract because Regular and Smart appliances
 *     compute their current draw differently (Smart has a LOW state).
 */

public abstract class Appliance {

    // ── Static ID generator ──────────────────────────────────────────
    // Each appliance receives a unique integer ID upon creation.
    // Using a class-level counter ensures uniqueness across all subclasses.
    private static int idCounter = 1;

    // ── Instance fields ──────────────────────────────────────────────
    private int applianceID;       // Unique system-generated ID
    private int locationID;        // 8-digit numeric account number grouping appliances by room/location
    private String type;           // Descriptive name, e.g. "Refrigerator - 17 cu. ft"
    private int onWattage;         // Power draw in the ON state (watts)
    private double probOn;         // Probability [0.0, 1.0] that this appliance is ON in any timestep
    private String status;         // Current state: "ON", "LOW", or "OFF"

    /**
     * Constructs an Appliance with validated inputs.
     *
     * @param locationID 8-digit account number for the appliance's location
     * @param type       descriptive name of the appliance
     * @param onWattage  wattage consumed when in the ON state (must be >= 0)
     * @param probOn     probability of being ON each timestep [0.0, 1.0]
     */
    public Appliance(int locationID, String type, int onWattage, double probOn) {
        this.applianceID = idCounter++;
        this.locationID = locationID;
        this.type = type;
        this.onWattage = onWattage;
        this.probOn = probOn;
        this.status = "OFF"; // All appliances start OFF; the simulation loop activates them
    }

    // ── Accessors ────────────────────────────────────────────────────

    public int getApplianceID()  { return applianceID; }
    public int getLocationID()   { return locationID; }
    public String getType()      { return type; }
    public int getOnWattage()    { return onWattage; }
    public double getProbOn()    { return probOn; }
    public String getStatus()    { return status; }

    // ── Mutators ─────────────────────────────────────────────────────

    public void setStatus(String status) { this.status = status; }

    // ── Behavioral methods ───────────────────────────────────────────

    /**
     * Determines whether this appliance should be ON for the current timestep
     * by comparing a random draw against its on-probability.
     * Called at the start of each simulation step (FAQ #6).
     *
     * @return true if the appliance is ON this timestep
     */
    public boolean determineOnOff() {
        double rand = Math.random();
        if (rand <= probOn) {
            status = "ON";
            return true;
        } else {
            status = "OFF";
            return false;
        }
    }

    /**
     * Returns the current power draw based on the appliance's status.
     * Subclasses override this to account for the LOW state (SmartAppliance)
     * or the simpler two-state model (RegularAppliance).
     *
     * @return current wattage consumed
     */
    public abstract int getPower();

    /**
     * Reports whether this appliance is a smart appliance.
     * Subclasses override to return true (Smart) or false (Regular).
     */
    public abstract boolean isSmart();

    /**
     * Provides a human-readable summary for reports and screen output.
     */
    @Override
    public String toString() {
        return String.format("ID:%d | Loc:%d | %s | %dW | Status:%s | Smart:%s",
                applianceID, locationID, type, onWattage, status, isSmart());
    }
}
