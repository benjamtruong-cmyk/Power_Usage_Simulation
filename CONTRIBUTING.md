# Contributing — Team Collaboration Guide

This repo is set up for our group to work together on the Power Usage Simulation System. Here's how to keep things organized.

## Getting started

1. Clone the repo:
   ```bash
   git clone <repo-url>
   cd PowerUsageSimulation
   ```
2. Make sure you can compile and run:
   ```bash
   cd src/
   javac *.java
   java AppClient
   ```

## Branch workflow

- **`main`** — working code only. Don't push broken stuff here.
- **Feature branches** — create a branch for whatever you're working on:
  ```bash
  git checkout -b feature/your-task-name
  ```
  Examples: `feature/csv-parser`, `fix/brownout-bug`, `docs/update-walkthrough`
- When you're done, push your branch and open a pull request so at least one other team member can look at it before merging.

## File organization

| Folder | What goes here |
|--------|----------------|
| `src/` | All `.java` source files — nothing else |
| `docs/` | Documentation markdown files |
| `data/` | Sample input files, generator files |
| `tests/` | Test results and notes |

## Code style

- Use the same formatting already in the codebase: 4-space indentation, curly braces on the same line.
- Add comments explaining **why** you made a decision, not just what the code does. "Sort descending" is obvious from the code — "sort descending because we want maximum savings per switch" is useful.
- If you change a method's behavior, update both the Javadoc in the source file and the relevant section in `docs/code-walkthrough.md`.

## Updating documentation

If you modify any Java code, update the matching section in the docs. The walkthrough (`docs/code-walkthrough.md`) should always reflect the current state of the code. Don't let them drift apart.

## Questions

If something in the assignment is ambiguous, check the FAQ first (most edge cases are answered there). If it's still unclear, post in the Discord channel before guessing.
