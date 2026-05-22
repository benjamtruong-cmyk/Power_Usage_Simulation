/**
 * RegularAppliance.java
 * Represents a non-smart appliance with only two states: ON and OFF.
 *
 * Design rationale:
 *   - Regular appliances cannot be switched to a LOW state.
 *   - They can only be turned OFF through a location-wide brownout (FAQ #22).
 *   - Power draw is either full onWattage (ON) or zero (OFF).
 */

public class RegularAppliance extends Appliance {

    /**
     * Constructs a RegularAppliance.
     *
     * @param locationID 8-digit account number
     * @param type       descriptive name
     * @param onWattage  wattage in ON state
     * @param probOn     probability of being ON each timestep
     */
    public RegularAppliance(int locationID, String type, int onWattage, double probOn) {
        super(locationID, type, onWattage, probOn);
    }

    /**
     * Returns power based on current status.
     * Regular appliances have a binary model: full power or zero.
     *
     * @return onWattage if ON, 0 otherwise
     */
    @Override
    public int getPower() {
        if (getStatus().equals("ON")) {
            return getOnWattage();
        }
        return 0;
    }

    /**
     * Regular appliances are not smart.
     */
    @Override
    public boolean isSmart() {
        return false;
    }
}
