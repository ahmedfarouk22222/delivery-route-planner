# Delivery Route Planner

Reads a list of delivery requests from a CSV file and groups them into vehicle trips that respect a
per-trip weight capacity, handling urgent deliveries first and keeping same-area deliveries together
where possible.

## How to Run

**Requirements:** JDK 17 or newer (the code uses `record`, so 16+ is the hard minimum). No external
libraries, no build tool, no network access needed. Verified on OpenJDK 21.

All commands must be run **from the repository root** — the directory containing `src/` and this README —
because the paths in `config.properties` are relative to the working directory.

```bash
# 1. Compile (javac follows imports from -sourcepath, so only the entry point is listed)
javac -d out -sourcepath src/main/java src/main/java/com/deliveryplanner/Main.java

# 2. Run
java -cp out com.deliveryplanner.Main
```

The same two commands work unchanged in PowerShell, cmd, and bash.

To compile and run the test suite (see [Tests](#tests) below):

```bash
# Windows (note the ';' path separator)
javac -d out -sourcepath "src/main/java;src/test/java" src/test/java/com/deliveryplanner/TestRunner.java

# macOS / Linux (note the ':' path separator)
javac -d out -sourcepath "src/main/java:src/test/java" src/test/java/com/deliveryplanner/TestRunner.java

# Then, on any platform:
java -cp out com.deliveryplanner.TestRunner
```

To run against different data or a different vehicle capacity, pass an alternative properties file as the
single optional argument — nothing needs recompiling:

```bash
java -cp out com.deliveryplanner.Main path/to/other-config.properties
```

**Output:** the report is printed to stdout *and* written to the file named by `output.file`
(`output/trips.txt` by default; the directory is created automatically). Both carry exactly the same text,
including the rejected-rows list at the end — a saved report is never missing information the console
showed.

## Input Format

There are two kinds of input file: the delivery data, and the settings that point at it.

**1. The delivery data — CSV** (`src/main/resources/input/deliveries.csv` by default).
Four comma-separated columns in this order:

```
id,area,priority,weight
1,Nasr City,2,4.5
2,Maadi,1,2.0
```

| Column | Type | Rules |
| --- | --- | --- |
| `id` | integer | must be unique across the file |
| `area` | text | must not be empty |
| `priority` | integer | 0 or greater; **lower means more urgent** |
| `weight` | decimal | greater than 0, and no greater than the vehicle capacity |

- The header row is **optional** — it is detected case-insensitively and skipped if the first line starts
  with `id,area,priority,weight`.
- Blank lines are ignored.
- Fields are trimmed of surrounding whitespace.
- Quoted fields are **not** supported, so an area name must not contain a comma.
- The file must not have a UTF-8 BOM (see "With Another Day" below).

Two sample files are included:

| File | Contents | Purpose |
| --- | --- | --- |
| `input/sample-from-brief.csv` | the 5 rows from the assignment's sample table, unchanged | check the planner against data you already know |
| `input/deliveries.csv` *(default)* | the same 5 rows with delivery 4 raised to 10 kg, plus 6 deliberately invalid ones | exercise every validation rule in a single run, and show a trip filled to exactly capacity |

Run the first one with its own config, which also demonstrates the command-line override:

```bash
java -cp out com.deliveryplanner.Main src/main/resources/config-sample-from-brief.properties
```

**2. The settings — `src/main/resources/config.properties`.** Nothing is hardcoded:

```properties
input.file=src/main/resources/input/deliveries.csv
output.file=output/trips.txt
vehicle.capacity=10.0
```

All three keys are required; `vehicle.capacity` must be a positive number. A missing or malformed key
stops the program with a clear message instead of failing later. Note that `.properties` files treat the
backslash as an escape character, so **paths must use forward slashes** even on Windows — this works fine
on every platform.

## Tests

```bash
java -cp out com.deliveryplanner.TestRunner
```

97 assertions across four suites, all of which must pass before the submission is considered working. The
harness (`Assert` and `TestRunner`, about 120 lines together) is hand-written rather than JUnit for one
reason: the run instructions above promise no external libraries and no build tool, and adding Maven just
to get `@Test` would break that promise for a project whose whole point is that it is simple enough to
read end to end. There is no reflection and no annotation magic — `TestRunner.main` calls each suite's
`run()` method in order, `Assert` counts passes and failures, and the process exits non-zero if anything
failed.

| Suite | Covers |
| --- | --- |
| `DeliveryPlannerTest` | the two invariants the brief actually demands — **every delivery appears in exactly one trip**, and **no trip exceeds capacity** — plus the documented example plan, the first-fit-beats-next-fit case, area grouping within a priority tier, and that a reversed input produces an identical plan (the determinism claim below) |
| `CsvDeliveryReaderTest` | every rejection branch with its exact reason string, the accepted forms (header present/absent/after a blank line, blank lines, padded fields), the exactly-at-capacity boundary, and that one bad row never stops the rest of the file |
| `TripTest` | the capacity boundary, the `add()` guard, the unmodifiable delivery list, and the `Delivery` record's own validation |
| `ReportGeneratorTest` | summary counts agree with the trips, rejected rows reach the report text, and an empty plan reports zeroes rather than `NaN` |

Test fixtures are written to a temp directory rather than read from `src/main/resources`, so editing the
committed sample files can never silently change what the tests assert.

## Example Run

Against the assignment's own sample data
(`java -cp out com.deliveryplanner.Main src/main/resources/config-sample-from-brief.properties`):

```
========== Delivery Summary ==========
Total deliveries:       5
Valid deliveries:       5
Rejected deliveries:    0

Total trips:            2
Total weight:           18.2 kg
Average trip weight:    9.1 kg

Unused capacity:        1.8 kg
Vehicle utilization:    91.0%
=======================================

Trip 1
--------------------------------
ID 2   | Maadi      | Priority 1 | 2.0 kg
ID 4   | Zamalek    | Priority 1 | 7.0 kg

Total weight:       9.0 kg
Remaining capacity: 1.0 kg
Utilization:        90.0%

Trip 2
--------------------------------
ID 5   | Maadi      | Priority 2 | 3.5 kg
ID 1   | Nasr City  | Priority 2 | 4.5 kg
ID 3   | Nasr City  | Priority 3 | 1.2 kg

Total weight:       9.2 kg
Remaining capacity: 0.8 kg
Utilization:        92.0%
```

Both priority-1 deliveries go out on the first trip, the two Nasr City deliveries land together on the
second, and all 5 deliveries appear exactly once across 2 trips at 91% overall utilization.

## Edge Cases

The brief notes there is no single correct answer for these, so here is what I chose and why.

**No deliveries at all.** An empty or header-only file prints `No deliveries found.` and exits cleanly
without writing an output file — an empty report would imply a plan was made when there was nothing to
plan. If the file *had* rows but every one of them was invalid, that is a different situation: it prints
`No valid deliveries found.` and still emits the report and the rejected-rows list, because the operator
needs to see *why* nothing could be planned.

**A package heavier than the vehicle capacity.** It is **rejected at read time, not split**, and reported
with its line number and reason. Splitting would mean inventing a way to divide a physical package, which
isn't the program's decision to make, and silently dropping it would hide a real data problem. Rejecting
it up front also gives the packer a useful guarantee: every delivery it receives is placeable, so no
delivery can ever get stuck unassigned mid-run. This keeps the rule "every *valid* delivery appears in
exactly one trip" true by construction.

**Multiple deliveries with the same priority.** Ties are broken by area (alphabetically), then by weight
descending, then by ID. Because ID is unique, no two deliveries ever compare as equal, so the ordering is
total and the same input always produces byte-identical output — which matters for a result an operator
may need to reproduce or compare against an earlier run.

**Adding the next package would exceed capacity.** The delivery is not forced in and the current trip is
not closed. First-fit moves on to the next open trip with room, and only opens a new trip if none of the
existing ones can take it. The capacity check happens before the add, with a second guard inside
`Trip.add()` that throws if it is ever reached — a safety net that should be unreachable by design.

## Extra Feature: Rejected-Row Audit Trail

A bad row does not stop the run. Every rejected row is collected with its **line number, the raw text, and
a specific reason**, then written in its own block at the end of the report — in the saved file as well as
on stdout, since a count of six rejections is useless to whoever opens `output/trips.txt` later if it
doesn't say *which* six:

```
========== Rejected Rows ==========
Line 7: "6,Giza,4,11.5" -> weight 11.5kg exceeds vehicle capacity 10.0kg
Line 8: "7,,2,3" -> one or more fields are empty
Line 9: "8,Heliopolis,abc,2" -> invalid priority
Line 10: "9,Maadi,1,-2" -> weight must be positive
Line 11: "2,Maadi,1,2" -> duplicate id 2
Line 12: "10,Dokki,3,0" -> weight must be positive
====================================
```

I chose this because real dispatch data is messy, and the two obvious alternatives are both worse for the
person actually using the program. Crashing on the first bad row means a 500-row file with one typo plans
nothing, and the operator fixes one error per run. Silently skipping bad rows is worse still — the program
appears to succeed while quietly under-delivering, and nothing in the output says a delivery went missing.
Reporting every problem in one pass means one run tells you everything that needs fixing, and the summary
counts (`Total / Valid / Rejected`) make it impossible to mistake a partial plan for a complete one.

Smaller additions that came along the way: all settings are externalized to `config.properties` with an
optional command-line override, the report includes per-trip and fleet-wide utilization statistics so the
quality of a plan is visible at a glance rather than having to be worked out by hand, and the dependency-free
test suite described above pins both of the brief's invariants as executable checks.

## 1. Solution Approach

I read deliveries from a CSV file and validate each row (numeric fields, positive weight, weight within vehicle capacity, no duplicate IDs). Valid deliveries are sorted by priority first, then by area, then by weight descending, then by ID as a final tiebreaker — this ordering does several things at once: it puts urgent deliveries first, area is a secondary key so same-area deliveries within a priority tier cluster together, and sorting heaviest-first within an area helps the packer fill trips more tightly. I then walk the sorted list once and greedily pack deliveries into trips using **first-fit**: for each delivery, check every trip opened so far (in order) and drop it into the first one that still has room; only open a new trip if none of the existing ones fit. This is a single deterministic pass — no backtracking, no re-shuffling — but unlike a simpler "next-fit" approach (which only checks the most recently opened trip), first-fit lets earlier trips keep receiving deliveries throughout the whole run instead of being left half-empty.

### Approaches Explored

I tried three packing strategies before settling on first-fit:

**1. Next-fit (initial version)** — only ever check the single most recently opened trip; if the next delivery doesn't fit, close that trip for good and open a new one. Simplest possible packing loop, but it can strand a trip at low utilization if a delivery just barely doesn't fit — that trip is never revisited even if a later, smaller delivery would've fit perfectly.

On the 5 valid rows of `deliveries.csv` (where delivery 4 weighs 10 kg), this produced 3 trips, with one trip sitting at only 20% utilization (a single 2.0kg delivery) because the next delivery in sort order was too heavy for it and a new trip was opened instead:

```
Trip 1: ID 2 (Maadi, 2.0kg)                                   → 20% utilization
Trip 2: ID 4 (Zamalek, 10.0kg)                                → 100% utilization
Trip 3: ID 5, ID 1, ID 3 (Maadi/Nasr City/Nasr City, 9.2kg)   → 92% utilization
```

**2. First-fit (what I kept)** — check *every* trip opened so far, not just the last one, and place the delivery in the first one with room. Same O(n log n) sort, same single pass, only the "which trip do I check" logic changed. On the same data this fully closed the gap left by next-fit:

```
Trip 1: ID 2, ID 5, ID 1 (Maadi/Maadi/Nasr City, 10.0kg) → 100% utilization
Trip 2: ID 4 (Zamalek, 10.0kg)                           → 100% utilization
Trip 3: ID 3 (Nasr City, 1.2kg)                          → leftover
```

**3. Knapsack DP (explored, not used)** — instead of greedily scanning for the first trip that fits, use 0/1 knapsack DP to find the *heaviest possible subset* of the remaining deliveries (within a priority tier) that fits under capacity, and make that subset one trip. This gets closer to the true optimum per trip than first-fit does, but its complexity is O(n · capacity) per trip instead of O(n log n) overall, which would become a real bottleneck at large input sizes (see question 4). It also adds real implementation complexity — discretizing weights into integer units, a 2D DP table, and backtracking to recover which items were chosen — that's hard to justify for a challenge that explicitly says a "reasonable," not mathematically optimal, solution is expected.

Note that this difference only shows up on data that can strand a trip. On the assignment's own sample rows (delivery 4 at 7.0 kg, `sample-from-brief.csv`) next-fit and first-fit both produce the same 2 trips — which is why I used the heavier `deliveries.csv` variant to tell the two strategies apart.

**Why I kept first-fit:** it produces the same 100%-utilization result as knapsack DP on this data, stays O(n log n), and is something I can trace by hand and explain line-by-line in an interview — which the brief explicitly says matters more than a sophisticated solution I can't fully explain.

## 2. Most Difficult Part

Deciding how to resolve the conflict between priority ordering and area grouping, since the challenge doesn't say which one wins. I settled on priority as the primary sort key and area as the secondary key, so priority is never violated, and area grouping happens "where reasonably possible" — meaning only among deliveries that already share a priority level. Writing that rule down precisely, and making sure the same input always produces the same output, took more thought than the actual trip-packing logic.

## 3. Where the Grouping Isn't Optimal

Yes. Since area grouping only applies within a priority tier, two same-area deliveries with different priorities will never end up in the same trip, even if combining them would've made for a tighter route. Also, first-fit is still a greedy heuristic, not an exhaustive search: it places each delivery into the *first* trip with room rather than searching for the *best* combination of deliveries per trip, so it can still end up with slightly more unused capacity than a true optimal packer (which I explored with a knapsack DP approach — see "Approaches Explored" above). I didn't ship the fully optimal version since bin-packing is NP-hard in general, and the knapsack DP alternative trades meaningfully worse time complexity for a small utilization gain that didn't show up on the sample data anyway.

## 4. At 1,000,000 Deliveries

Sorting is the main cost — O(n log n), so about 20 million comparisons, which is fine on modern hardware but not instant. The bigger concern is memory: I load the entire delivery list into memory before sorting and planning, so with a million rows that's a sizable in-memory list, and generating the full report string in memory before writing it out would also grow proportionally. For a truly huge input I'd want to stream the CSV and write the report incrementally instead of building everything in memory at once.

## 5. With Another Day

I'd finish and benchmark the knapsack DP planner I explored (see "Approaches Explored") on larger, more varied datasets to see whether its tighter packing is worth the extra time cost in practice, rather than deciding purely from complexity analysis. I'd also stream the input/output for large files instead of holding everything in memory, and close the parsing limitations I know about:

- **UTF-8 BOM.** A CSV saved with a byte-order mark makes the first line fail header detection and get rejected as `invalid id`. A three-byte check when opening the file would fix it.
- **Quoted fields.** `CsvUtils.splitRow` splits on every comma, so an area name containing a comma would be mis-parsed. Handling quotes properly (or moving to a small CSV library) would remove the restriction.
- **Area normalization.** `"Maadi"`, `"maadi"` and `" Maadi"` are currently three different areas for grouping purposes, so they would not cluster together. I'd make the comparison configurable rather than silently normalizing, since which spellings count as "the same area" is a business decision.
- **Floating-point accumulation.** Trip weight is a running `double` sum, so after many additions a package that exactly fills the remaining capacity could be rejected by a fraction of a gram. Storing weights in integer grams would make the capacity check exact.
