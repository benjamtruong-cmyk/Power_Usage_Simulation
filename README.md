[README.md](https://github.com/user-attachments/files/27451935/README.md)
# Power Usage Simulation System

**CS 1400 — Introduction to Programming and Problem Solving**  
**Group Assignment 3 | Group 4 | Cal Poly Pomona**

---

## What is this?

A Java application that simulates the power consumption of a building across multiple locations (rooms). Each location contains a mix of Regular and Smart appliances. The simulation runs over discrete timesteps — each step randomly activates appliances based on per-device probabilities, and when total power exceeds a user-defined warning level, a two-phase demand-response algorithm kicks in to bring consumption back under the limit.

The project is based on the [GridWise smart appliance research](https://www.nbcnews.com/id/21760974) by Pacific Northwest National Laboratory, where real appliances equipped with embedded sensors could autonomously reduce power consumption during peak grid stress.

---

## Repository structure

```
PowerUsageSimulation/
├── README.md                         ← you are here
├── CONTRIBUTING.md                   ← collaboration guide for team editors
├── .gitignore
├── src/                              ← all Java source files
│   ├── Appliance.java                  abstract superclass
│   ├── RegularAppliance.java           ON/OFF subclass
│   ├── SmartAppliance.java             ON/LOW/OFF subclass
│   ├── Location.java                   room grouping + brownout
│   ├── PowerGrid.java                  grid manager + CSV parser
│   ├── Simulator.java                  timestep loop + algorithm
│   └── AppClient.java                  main entry point + menu
├── docs/                             ← detailed documentation
│   ├── code-walkthrough.md             line-by-line code analysis
│   ├── algorithm-design.md             algorithm explanation + flowchart
│   └── testing-strategy.md             test cases + edge cases
├── data/                             ← sample input files
│   ├── app.txt                         sample appliance file
│   ├── ApplianceGenerator.java         generates larger test files
│   └── ApplianceDetail.txt             appliance data for generator
└── tests/                            ← test documentation
    └── test-results.md                 results from our runs
```

---

## How to compile and run

```bash
# 1. Navigate to the src/ directory
cd src/

# 2. Compile all source files
javac *.java

# 3. Run the program
java AppClient
```

The program will prompt you for:
1. The total allowed wattage (warning level) — must be greater than 0
2. Use the interactive menu to either load appliances from a CSV file (option `F`) or add them manually (option `A`)
3. Start the simulation (option `S`) and enter the number of timesteps

**Client class:** `AppClient.java`

---

## Input file format

The appliance CSV has six comma-separated fields per line:

```
locationID,applianceName,onWattage,probOn,isSmart,reductionPercent
```

Example:
```
10000001,Clothes Washer,1200,0.025,true,0.25
10000001,VCR,45,0.05,false,0.0
10000002,Clothes Dryer,5400,0.1,true,0.33
```

---

## Outputs

| Output | Destination | Content |
|--------|-------------|---------|
| Per-timestep summary | Console | # smart appliances switched to LOW, # locations browned out |
| Simulation summary | Console | Total locations affected per interval, max affected location |
| Detailed report | `simulation_report.txt` | Every appliance and location affected during each interval |

---

## Documentation

For a deep dive into how the code works, start here:

- **[Code walkthrough](docs/code-walkthrough.md)** — line-by-line analysis of every class, field, and method
- **[Algorithm design](docs/algorithm-design.md)** — the two-phase power control algorithm with worked examples
- **[Testing strategy](docs/testing-strategy.md)** — all test cases, edge cases, and expected results

---

## Team

Group 4 — CS 1400, Cal Poly Pomona

See the [Division of Labor report](DivisionOfLabor.docx) for individual contributions.
