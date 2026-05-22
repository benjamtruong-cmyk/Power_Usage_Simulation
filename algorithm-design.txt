# Algorithm Design — Two-Phase Power Control

> This document explains how our power control algorithm works, why we designed it this way, and walks through concrete numerical examples.

## The problem

Each timestep, a random subset of appliances turns ON. If the combined wattage of all ON appliances exceeds the building's warning level, we need to reduce consumption. The challenge is doing this **intelligently** — shutting things off randomly would work, but we'd disrupt more locations and appliances than necessary.

Our goal: bring total power ≤ warning level while impacting the fewest devices and locations possible.

---

## Why two phases?

We split the algorithm into two phases because smart and regular appliances respond differently:

- **Smart appliances** can individually reduce their power (ON → LOW) without going completely dark. This is the least disruptive option.
- **Regular appliances** have no LOW state. The only way to reduce their draw is to brown out their entire location, which shuts down every appliance at that address.

Phase 1 handles the light touch (throttling smart appliances). Phase 2 handles the heavy hand (location brownouts). We always try Phase 1 first and only escalate to Phase 2 if throttling isn't enough.

---

## Phase 1: Smart appliance throttling

### Steps

1. Collect all smart appliances currently in the ON state
2. Sort them in **descending order of power savings** (the appliance where ON→LOW saves the most watts comes first)
3. Switch the top appliance from ON to LOW
4. Recalculate total grid power
5. If total ≤ warning level → done. Otherwise → repeat from step 3
6. If all smart appliances exhausted and still over → proceed to Phase 2

### Why descending savings?

This is a **greedy** approach. By switching the highest-savings appliance first, we maximize the wattage reduction per switch, meaning we throttle the minimum number of devices to reach the target. Two switches affecting 2 appliances is better than five switches affecting 5 appliances, even if the total watts freed is the same.

### Worked example

```
Warning level:    5,000W
Total ON power:   6,200W (over by 1,200W)

Smart appliances sorted by descending savings:

  Appliance              ON       LOW      Savings
  ──────────────────────────────────────────────────
  Clothes Dryer (Elec)   5,000W   3,750W   1,250W
  Air Conditioner        1,000W     500W     500W
  Dishwasher               200W     150W      50W

Step 1: Switch Clothes Dryer → LOW
  Freed 1,250W → New total: 4,950W
  4,950W ≤ 5,000W ✓ — DONE. Only 1 appliance affected.
```

---

## Phase 2: Location brownout

### Steps

1. Collect all locations that haven't been browned out and still have active power draw
2. Sort them in **descending order by current total wattage** (highest draw first)
3. Brown out the top location (set ALL its appliances to OFF)
4. Recalculate total grid power
5. If total ≤ warning level → done. Otherwise → repeat from step 3

### Why descending wattage?

This is where the original design (ascending appliance count) needed rethinking.

**The problem with appliance count:** `ApplianceGenerator.java` assigns 15–20 appliances to every location (`applianceCount = (int)(Math.random()*6) + 15`). In a real generated dataset with 100 locations, every location has between 15 and 20 appliances. Sorting by count in that range means you're comparing 15 vs. 18 vs. 16 — differences so small they're effectively arbitrary. It doesn't tell you anything meaningful about how much power a brownout would free.

**Why wattage is the right key:** The appliance mix varies dramatically by location. A location that randomly got an AC unit (1,000W), a clothes dryer (5,000W), and a treadmill (3,000W) is drawing ~9,000W. A location that got clocks (3W), answering machines (10W), and bug killers (40W) is drawing under 200W. Sorting by current wattage reveals this difference directly.

**Why descending (highest first)?** This mirrors the philosophy of Phase 1: **maximize power freed per action taken.** By browning out the highest-draw location first, we minimize the total number of locations that go dark to reach the warning level. Fewer brownouts = fewer locations disrupted = less impact on occupants.

| Sort strategy | Effect | When locations have similar counts |
|---------------|--------|-------------------------------------|
| Ascending count (old) | Small locations first | Essentially random — counts are 15–20 |
| Ascending wattage | Low-draw first | Requires MORE brownouts — wrong direction |
| **Descending wattage (new)** | **High-draw first** | **Fewest brownouts to reach target** |

### Worked example

```
After Phase 1: total still at 7,200W (warning = 5,000W, over by 2,200W)
All locations have 15–18 appliances (counts nearly equal).

Locations sorted by descending current wattage:

  Location      Apps   Current draw   Notes
  ────────────────────────────────────────────────────────
  Loc A         18     5,800W         Has AC, dryer, treadmill
  Loc B         16     1,950W         Has washer, dishwasher, fridge
  Loc C         15       820W         Mostly lights, fans, clocks
  Loc D         17       430W         Low-wattage devices only

Step 1: Brown out Loc A (5,800W freed)
  New total: 7,200 − 5,800 = 1,400W
  1,400W ≤ 5,000W ✓ — DONE. Only 1 location browned out.

Compare: old ascending-count approach might have started with Loc C or D
(15–16 apps), freeing only 820W or 430W, requiring 3–4 brownouts for
the same result.
```

---

## Design trade-offs

| Decision | Advantage | Drawback |
|----------|-----------|----------|
| Greedy descending sort (Phase 1) | Minimizes appliances throttled | Not globally optimal |
| Descending wattage sort (Phase 2) | Minimizes locations browned out | Highest-draw locations hit hardest |
| Two-phase escalation | Clear hierarchy, easy to reason about | Can't interleave throttle and brownout |
| Recalculate after each action | Never overshoots | Slightly more computation than batch |

---

## Connection to real-world systems

Our algorithm mirrors how real demand-response systems work. In the [GridWise project](https://www.nbcnews.com/id/21760974), smart appliances with embedded chips first reduce their own consumption (our Phase 1). If that isn't enough, utilities implement rolling brownouts by feeder circuit or neighborhood, targeting the highest-load circuits first (our Phase 2 descending wattage). The key insight: start with the action that frees the most power per disruption.
