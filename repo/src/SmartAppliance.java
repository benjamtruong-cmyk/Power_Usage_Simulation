/**
 * SmartAppliance.java
 * Represents a smart appliance with three states: ON, LOW, and OFF.
 *
 * Design rationale:
 *   - Smart appliances can sense grid load and reduce their wattage
 *     by switching to LOW state, which consumes (1 - reductionPercent) * onWattage.
 *   - Individual smart appliances can be switched ON -> LOW by the power algorithm.
 *   - They can only go to OFF via a location brownout (FAQ #17).
 *   - The reductionPercent is read from the CSV and remains constant during simulation (FAQ #9).
 */

public class SmartAppliance extends Appliance {

    // The fractional reduction in power when switching from ON to LOW.
    // E.g., 0.25 means LOW draws 75% of the ON wattage.
    private double reductionPercent;

    /**
     * Constructs a SmartAppliance.
     *
     * @param locationID       8-digit account number
     * @param type             descriptive name
     * @param onWattage        wattage in ON state
     * @param probOn           probability of being ON each timestep
     * @param reductionPercent fraction [0.0, 1.0] of power saved in LOW state
     */
    public SmartAppliance(int locationID, String type, int onWattage,
                          double probOn, double reductionPercent) {
        super(locationID, type, onWattage, probOn);
        this.reductionPercent = reductionPercent;
    }

    /**
     * Returns the wattage consumed in the LOW state.
     * Calculated as onWattage * (1 - reductionPercent).
     * E.g., 1200W appliance with 0.25 reduction -> 900W in LOW.
     *
     * @return LOW-state wattage
     */
    public int getLowPower() {
        return (int) Math.round(getOnWattage() * (1.0 - reductionPercent));
    }

    /**
     * Returns power draw based on the three-state model:
     *   ON  -> full onWattage
     *   LOW -> reduced wattage via getLowPower()
     *   OFF -> zero
     *
     * @return current wattage consumed
     */
    @Override
    public int getPower() {
        switch (getStatus()) {
            case "ON":
                return getOnWattage();
            case "LOW":
                return getLowPower();
            default: // OFF
                return 0;
        }
    }

    /**
     * Calculates the wattage saved by switching from ON to LOW.
     * Used by the power algorithm to prioritize which appliances to throttle first.
     *
     * @return wattage savings when switching ON -> LOW
     */
    public int getSavings() {
        return getOnWattage() - getLowPower();
    }

    public double getReductionPercent() {
        return reductionPercent;
    }

    @Override
    public boolean isSmart() {
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" | LowW:%d | Reduction:%.0f%%",
                getLowPower(), reductionPercent * 100);
    }
}
