# Code Walkthrough — Line-by-Line Analysis

> This document walks through every class in the Power Usage Simulation System from the ground up. We organized it in dependency order: the classes that do not depend on anything else come first, then the classes that build on top of them, all the way up to `AppClient` (the entry point).

## Table of contents

1. [Appliance.java — the abstract foundation](#1-appliancejava)
2. [RegularAppliance.java — the ON/OFF subclass](#2-regularappliancejava)
3. [SmartAppliance.java — the ON/LOW/OFF subclass](#3-smartappliancejava)
4. [Location.java — room grouping and brownout](#4-locationjava)
5. [PowerGrid.java — the grid manager](#5-powergridjava)
6. [Simulator.java — the simulation engine](#6-simulatorjava)
7. [AppClient.java — main entry point](#7-appclientjava)

---

## 1. Appliance.java

**Role:** Abstract superclass that defines what every appliance in the building has in common. Both `RegularAppliance` and `SmartAppliance` extend this class.

**Why use an abstract class?** A generic "Appliance" class cannot be properly instantiated because the program would not know how to properly calculate its power (Regular and Smart appliances compute power differently). Making this class and its methods abstract forces each subclass to provide its own version. This also means that the program can store both types in the same `ArrayList<Appliance>` and call `getPower()` on any of them without type-checking (through using polymorphism).

### Fields

```java
private static int idCounter = 1;
```
**Line 19:** `idCounter` is a `static` field, meaning it belongs to the class itself over a specific, individual object. Whenever the class creates an appliance (of any subtype), the constructor grabs the current value of `idCounter` and increments it. This guarantees that no two appliances ever share the same ID, even if they're created at different times or from different subclasses, because making an ID is connected to the class. The count starts at 1 to ensure that the IDs are always positive, which makes them easier to display and validate. This is to comply with FAQ #10 which instructs the program to use a static counter.

```java
private int applianceID;
private int locationID;
private String type;
private int onWattage;
private double probOn;
private String status;
```
**Lines 22–27:** These six fields define the state of every appliance:

| Field | Type | What it stores | Constraints |
|-------|------|---------------|-------------|
| `applianceID` | `int` | System-generated unique ID | Auto-assigned, never user-editable |
| `locationID` | `int` | 8-digit account number linking this appliance to a room | Comes from the CSV or manual entry |
| `type` | `String` | Human-readable name like "Refrigerator - 17 cu. ft" | From CSV column 2 |
| `onWattage` | `int` | How many watts this appliance draws when fully ON | Must be ≥ 0 (FAQ #8) |
| `probOn` | `double` | Probability this appliance turns ON each timestep | Must be in [0.0, 1.0] (FAQ #8) |
| `status` | `String` | Current state: "ON", "LOW", or "OFF" | "LOW" is only valid for SmartAppliance |

### Constructor

```java
public Appliance(int locationID, String type, int onWattage, double probOn) {
    this.applianceID = idCounter++;
    this.locationID = locationID;
    this.type = type;
    this.onWattage = onWattage;
    this.probOn = probOn;
    this.status = "OFF";
}
```
**Lines 37–44:** The constructor takes four parameters. `applianceID` is intentionally not one of the parameters because the system assigns it automatically via `idCounter++`. The post-increment operator is important because `this.applianceID = idCounter++` means to "assign the current value of `idCounter` to this object's ID, then bump `idCounter` up by 1 for the next appliance."

Every appliance starts in the "OFF" state. The simulation loop calls `determineOnOff()` when beginning each timestep to randomly activate it based on `probOn`.

### determineOnOff()

```java
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
```
**Lines 68–77:** This is how each appliance "decides" whether it's running during a given timestep. `Math.random()` returns a value in [0.0, 1.0). If that random number is less than or equal to the appliance's `probOn`, it turns ON. Otherwise, it stays OFF.

For example, an appliance with `probOn = 0.1` has a 10% chance of being ON each timestep. An appliance with `probOn = 1.0` will always be ON (which is useful for stress testing). This is called at the start of every timestep before the algorithm runs. FAQ #6 confirmed this is the correct approach — "generate a random number, if random ≤ probOn, the appliance is ON."

### Abstract methods

```java
public abstract int getPower();
public abstract boolean isSmart();
```
**Lines 86, 92:** These two methods have no body because the subclasses must override them. `getPower()` returns the current wattage draw based on the appliance's status. `isSmart()` is a flag that filters smart appliances without using `instanceof` checks. This method is cleaner and more efficient.

### toString()

```java
public String toString() {
    return String.format("ID:%d | Loc:%d | %s | %dW | Status:%s | Smart:%s",
            applianceID, locationID, type, onWattage, status, isSmart());
}
```
**Lines 98–101:** Formats a one-line summary of the appliance for console output. It uses  `String.format()` instead of concatenation because it is cleaner and more readable and automatically formats different data types. The program calls this when it prints the appliance list or logs affected appliances in the simulation report.

---

## 2. RegularAppliance.java

**Role:** Represents a non-smart appliance such as an alarm clock or lamp that does not have grid-awareness built in. These appliances only have an ON and OFF state.

**Key constraint (FAQ #22):** Regular appliances cannot be individually turned off by the algorithm. Only a location-wide brownout can classify these appliances as OFF.

### Constructor

```java
public RegularAppliance(int locationID, String type, int onWattage, double probOn) {
    super(locationID, type, onWattage, probOn);
}
```
**Lines 21–23:** Uses the super keyword to call the parent constructor in the Appliance superclass. `RegularAppliance` does not introduce any new fields as all of the data it needs exists in `Appliance`. The `super()` call passes all four parameters to the `Appliance` constructor, which handles ID assignment and field initialization.

### getPower()

```java
public int getPower() {
    if (getStatus().equals("ON")) {
        return getOnWattage();
    }
    return 0;
}
```
**Lines 32–37:** This is a binary power model as it has either an ON or OFF state. If the appliance is ON, it draws its full rated wattage. If it is rated OFF, it draws zero. There is no LOW state for regular appliances as the LOW state is exclusive for smart appliances. The program uses `getStatus()` and `getOnWattage()` (the accessor methods from the parent class) rather than accessing fields directly because the parent fields are `private` to properly encapsulate the methods in their respective classes.

### isSmart()

```java
public boolean isSmart() { return false; }
```
**Line 43-45:** This method always returns `false` because regular appliances are not smart appliances. The Simulator uses this to filter out regular appliances when building the list of smart appliances to throttle in Phase 1. 

---

## 3. SmartAppliance.java

**Role:** Represents a grid-aware appliance such as a smart thermostat, smart washer, or smart dryer that can sense strain on the power grid and reduce its power consumption by switching to a LOW state. This means that smart appliances, unlike regular appliances, have three power states: ON, LOW, OFF.

**Key constraints:**
- Smart appliances can be individually switched from ON → LOW by the algorithm (Phase 1).
- They can only be turned OFF through a location brownout (this complies with FAQ #17) as the algorithm cannot turn individual appliances off.
- The `reductionPercent` comes from the CSV file and stays constant throughout the simulation (this complies with FAQ #9).

### The reduction field

```java
private double reductionPercent;
```
**Line 17:** This field is exclusive to the SmartAppliance class. It is a decimal number between 0.0 and 1.0 that represents the amount of power the appliance saves in LOW mode. For example, a value of `0.25` indicates that the appliance is saving 25% of its rated wattage (so a 1200W appliance would only draw 900W while in LOW power mode).

### getLowPower()

```java
public int getLowPower() {
    return (int) Math.round(getOnWattage() * (1.0 - reductionPercent));
}
```
**Lines 41–43:** Calculates the wattage that the smart appliance consumes while in LOW state. The formula is: `onWattage × (1 - reductionPercent)`. The program uses `Math.round()` and casts to `int` as wattage should be a whole number. The program does not deal with fractional watts in its calculations. The rounding ensures that the program does not lose precision from truncation.

Example: A dryer rated at 5400W with a 33% reduction → `5400 * (1 - 0.33)` = `5400 * 0.67` = `3618W` while in LOW.

### getPower()

```java
public int getPower() {
    switch (getStatus()) {
        case "ON":  return getOnWattage();
        case "LOW": return getLowPower();
        default:    return 0;
    }
}
```
**Lines 54–63:** Three-state power model that uses a `switch` statement. This overrides the abstract `getPower()` method from the Appliance class. When the Simulator calls `getPower()` on any `Appliance` reference, it uses the correct calculation based on whether it is a `RegularAppliance` (two states) or `SmartAppliance` (three states). The `default` case catches "OFF" and any unexpected status strings. Each case has a return statement to prevent the cases from spilling into one another.

### getSavings()

```java
public int getSavings() {
    return getOnWattage() - getLowPower();
}
```
**Lines 71–73:** Returns the wattage difference between the appliances in ON and LOW modes. This value is the amount of power saved by throttling this appliance. The Simulator sorts smart appliances by this value in descending order during Phase 1. The appliance that saves the most power is throttled first, which aligns with the greedy approach.

---

## 4. Location.java

**Role:** Represents an area in the building. Each location is identified by an 8-digit account number and contains a list of appliances. Locations are the unit of brownout: when power consumption in an area is too high, the program will switch the power states of all appliances in a location to OFF, regardless of whether or not it is regular or smart.

### Fields

```java
private int locationID;
private ArrayList<Appliance> appliances;
private boolean brownedOut;
```
**Lines 18–20:** Three fields:
- `locationID`: The 8-digit account number from the CSV (column 1). Multiple appliances can share the same `locationID`, which groups them into the same room.
- `appliances`: An `ArrayList` that holds all appliance objects at this location. The program uses an `ArrayList` instead of a regular array because the number of appliances per location varies and it is easier to add or remove values with an ArrayList.
- `brownedOut`: A boolean flag that tracks if this location has been browned out during the current timestep. It becomes reset to `false` at the start of each new timestep.

There is also a default constructor which inputs the `LocationID` and instantiates an object, as well as accessor methods for the fields above. 

### brownOut()

```java
public int brownOut() {
    int freedWattage = 0;
    for (Appliance app : appliances) {
        freedWattage += app.getPower();
        app.setStatus("OFF");
    }
    brownedOut = true;
    return freedWattage;
}
```
**Lines 89–97:** This is the brownout operation. It iterates through every appliance in this location, captures how much power each one is currently drawing (via `getPower()`, which works polymorphically for both Regular and Smart appliances as there is an overwritten `getPower()` method in both the Regular and Smart appliance classes), then sets each one to OFF. The method returns the total wattage freed, which the Simulator uses to know how much closer the overall power is to the warning level.

The order of when the methods are called is important: the class calls `getPower()` before `setStatus("OFF")` because it must know how much power the appliance was drawing before it shuts it down. If it was set to OFF first, `getPower()` would return 0 for every iteration, causing a logical error.

### resetBrownout()

```java
public void resetBrownout() {
    brownedOut = false;
}
```
**Lines 103–105:** Called at the start of each timestep to clear the boolean brownout flag. Each timestep is independent as a location that was browned out in one timestep could be okay in another timestep, depending on which appliances randomly turn ON.

### getTotalPower()

```java
public int getTotalPower() {
    int total = 0;
    for (Appliance app : appliances) {
        total += app.getPower();
    }
    return total;
}
```
**Lines 112–118:** Sums the power draw of all appliances at this location. This method uses polymorphism — `app.getPower()` to return the right value of whether the appliance is a Regular (ON → full wattage, OFF → 0) or Smart (ON → full, LOW → reduced, OFF → 0). This is used by the Simulator to identify which locations are drawing the most power and by `PowerGrid.getTotalPower()` to compute the grid-wide total.

---

## 5. PowerGrid.java

**Role:** The central class that manages the entire building's electrical system. It holds all locations in a `HashMap`, provides CRUD operations (create, read, update, delete), and handles parsing the CSV appliance file. This is equivalent to the "database layer" of the application.

### The HashMap

```java
private HashMap<Integer, Location> locations;
private int warningLevel;
```
**Lines 22–23:** The program uses a `HashMap<Integer, Location>` where the key is the `locationID` and the value is the `Location` object. This gives us O(1) average-case lookup by location ID. When the algorithm needs to find a specific location during brownout, it does not have to search through a list. While `HashMap` does not maintain insertion order, ordered iteration is unnecessary for this application.

`warningLevel` is the maximum allowed wattage before the power control algorithm activates.

### addAppliance()

```java
public void addAppliance(Appliance app) {
    int locID = app.getLocationID();
    if (!locations.containsKey(locID)) {
        locations.put(locID, new Location(locID));
    }
    locations.get(locID).addAppliance(app);
}
```
**Lines 49–55:** Adds an appliance to the grid. This auto-creates locations: if the program encounters a previously unseen `locationID`, it creates a new `Location` object for it automatically. This means that it does not need to pre-define locations as they naturally emerge from the data. After ensuring the location exists, the program adds the appliance to its internal list.

### deleteAppliance()

```java
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
```
**Lines 65–76:** Deletes an appliance by identifying its system-generated ID. The program searches through all location because it is only aware of the appliance ID, and not which location it belongs to. The `new ArrayList<>(locations.keySet())` creates a copy of the key set. This is because removing entries from a `HashMap` while iterating over it directly would throw a `ConcurrentModificationException`. After removing the appliance, the program checks if the location is now empty and removes it if there are no values in it to save on space.

### readAppFile()

```java
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
                // validation
                if (probOn < 0 || probOn > 1) { /* skip */ continue; }
                if (onWattage < 0) { /* skip */ continue; }
                Appliance app;
                if (isSmart) {
                    app = new SmartAppliance(locationID, appName, onWattage, probOn, reductionPct);
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
    } finally {
        if (scan != null) scan.close();
    }
    return count;
}
```
**Lines 121–193 (condensed):** This method parses through the CSV. It reads the file line by line, splits each line on commas, and validates the data before creating an appliance. There are several layers of error handling:

1. **Empty lines** are skipped.
2. **Lines with fewer than 6 fields** are flagged as malformed and skipped.
3. **Individual field parsing** is wrapped in its own `try-catch` for `NumberFormatException`. A bad number in one line should not be able to crash the whole file load.
4. **Domain validation** checks that probabilities are in [0, 1] and wattage is non-negative (this follows FAQ #8).
5. **File-level errors** (file not found, I/O errors) are caught and reported.
6. **The `finally` block** ensures the Scanner is closed even if an exception occurs (prevents resource leaks).

After validation, the method uses the `isSmart` boolean to decide whether to create a `SmartAppliance` or `RegularAppliance`, then adds it to the grid via `addAppliance()`.

---

## 6. Simulator.java

**Role:** The simulation  engine which contains the main power control algorithm. It runs a loop over the requested number of timesteps. For each step, it randomly activates appliances, checks total power, and runs the two-phase algorithm if needed. It also generates both the screen summary and the detailed text file report.

### Tracking arrays

```java
private int[] smartSwitchedPerStep;
private int[] locationsBrownedPerStep;
private ArrayList<String> detailedLog;
```
**Lines 39–41:** These three structures accumulate data across all timesteps:
- `smartSwitchedPerStep[i]` stores how many smart appliances were throttled to LOW during timestep `i`.
- `locationsBrownedPerStep[i]` stores how many locations were browned out during timestep `i`.
- `detailedLog` collects verbose text entries for each timestep which are written to the output file.

### simulateTimestep() — the core loop body

This method does four things per timestep:

**Step 1: Reset and activate.** Clear brownout flags for all locations, then call `determineOnOff()` on every appliance. This randomizes the power state of each appliance regardless of what happened last timestep.

**Step 2: Calculate total power.** Sum up the wattage of all appliances across the entire grid.

**Step 3: Run the algorithm if over the limit.**

Phase 1 — Smart throttling:
```java
ArrayList<SmartAppliance> onSmartApps = new ArrayList<>();
for (Appliance app : grid.getAllAppliances()) {
    if (app.isSmart() && app.getStatus().equals("ON")) {
        onSmartApps.add((SmartAppliance) app);
    }
}
Collections.sort(onSmartApps, new Comparator<SmartAppliance>() {
    public int compare(SmartAppliance a, SmartAppliance b) {
        return Integer.compare(b.getSavings(), a.getSavings());
    }
});
for (SmartAppliance sa : onSmartApps) {
    if (grid.getTotalPower() <= warningLevel) break;
    sa.setStatus("LOW");
    smartSwitched++;
}
```

This section does three things in sequence:
1. **Collects** the information of all smart appliances that are currently ON (not OFF, not already LOW).
2. **Sorts** them in descending order of `getSavings()`. The appliance that saves the most power by going to LOW comes first.
3. **Iterates** through the sorted list, switching each one to LOW and rechecking the total power after each switch. The moment the program detects the total power to be under the warning level, it stops using the `break` statement. This is the greedy approach as the program only switches the number of appliances necessary.

Phase 2 — Location brownout (only runs if Phase 1 was not enough):
```java
ArrayList<Location> candidateLocations = new ArrayList<>();
for (Location loc : grid.getLocations().values()) {
    if (!loc.isBrownedOut() && loc.getTotalPower() > 0) {
        candidateLocations.add(loc);
    }
}
Collections.sort(candidateLocations, new Comparator<Location>() {
    public int compare(Location a, Location b) {
        return Integer.compare(a.getApplianceCount(), b.getApplianceCount());
    }
});
for (Location loc : candidateLocations) {
    if (grid.getTotalPower() <= warningLevel) break;
    loc.brownOut();
    locationsBrowned++;
}
```

Same structure as Phase 1, but operating on locations instead of individual appliances:
1. **Collects** locations that have not been browned out yet and are still using power.
2. **Sorts** them in ascending order by appliance count. Locations with fewer appliances are browned out first because they affect fewer people.
3. **Iterates**, calling `brownOut()` on each location until the total power is under the limit.

**Step 4: Record results.** Log everything for both the screen summary and the detailed file report.

### printSummaryReport()

**Lines 216–250:** After all timesteps complete, this method prints the aggregate stats: total smart appliances throttled across all steps, total brownouts, and the single worst timestep (the timestep with the most brownouts). It also prints a per-timestep breakdown table, but only for timesteps with significant events to keep the output readable when running longer simulations.

### writeDetailedReport()

**Lines 258–277:** Writes the detailed log to a text file using `PrintWriter` wrapped around `FileWriter`. The `try-with-resources` statement (`try (PrintWriter writer = ...)`) ensures that the file is properly closed even if the writing fails while creating the file. The report includes header info (warning level, appliance count, location count) followed by the timestep-by-timestep log.

---

## 7. AppClient.java

**Role:** The main entry point. Handles all user interaction through a text-based menu system. Collects the warning level, delegates CRUD operations to `PowerGrid`, and launches the simulation via `Simulator`.

### The menu loop

```java
while (true) {
    // print options
    option = scan.nextLine().trim().toUpperCase();
    switch (option) {
        case "A": addApplianceManual(); break;
        case "D": deleteAppliance(); break;
        case "L": grid.listAppliances(); break;
        case "F": readFromFile(); break;
        case "S": startSimulation(); break;
        case "Q": scan.close(); return;
        default: System.out.println("Invalid option...");
    }
}
```

**Lines 54–89 (condensed):** An infinite loop (`while(true)`) that shows the entire menu until the user types Q. The program uses `.trim().toUpperCase()` on the input to handle both "a" and "A" and to ignore the leading or trailing spaces. The `switch` statement assigns the input to the appropriate method. The `return` statement on Q exits `main()`, which terminates the program.

### Input validation pattern

Every numeric input in `AppClient` follows the same general pattern:

```java
int value = -1;             // start with an invalid value
while (value <= 0) {        // keep looping until we get something valid
    System.out.print(prompt);
    try {
        value = Integer.parseInt(scan.nextLine().trim());
        if (value <= 0) {
            System.out.println("Error: Must be positive.");
        }
    } catch (NumberFormatException e) {
        System.out.println("Error: Enter a valid integer.");
    }
}
```

This general pattern appears in:
- Warning level prompt (must be > 0, FAQ #5)
- Wattage input when manually adding an appliance (must be ≥ 0)
- Probability input (must be in [0.0, 1.0])
- Timestep count (must be > 0)

The `try-catch` handles non-numeric input (letters, symbols), and the conditional check handles numeric input that's out of range. The loop guarantees that the algorithm will never proceed with invalid data.

### startSimulation()

```java
private static void startSimulation() {
    if (grid.getAllAppliances().isEmpty()) {
        System.out.println("Error: No appliances loaded...");
        return;
    }
    // prompt for timesteps...
    Simulator sim = new Simulator(grid, timesteps);
    sim.runSimulation();
}
```

**Lines 206–227 (Condensed):** Before running, the program checks that appliances actually exist in the grid as running a simulation with zero appliances would print empty results. After collecting the timestep count, it creates a `Simulator` object (passing it the `PowerGrid` and timestep count) and calls `runSimulation()`. Note that a new `Simulator` object is created each time. This means each simulation run starts fresh with no leftover data from previous runs.

---

## Class dependency summary

```
AppClient
  └── creates PowerGrid (stores as static field)
  └── creates Simulator (passes PowerGrid to it)
        └── reads PowerGrid
              └── manages Location objects (via HashMap)
                    └── contains Appliance objects (via ArrayList)
                          ├── RegularAppliance (extends Appliance)
                          └── SmartAppliance (extends Appliance)
```

Data flows top-down: `AppClient` collects user input and delegates to `PowerGrid` for storage and `Simulator` for computation. The algorithm operates on the same `PowerGrid` instance that was populated by the user's add/load operations — there's no copying or duplication of data.
