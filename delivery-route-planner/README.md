# Delivery Route Planner

## 1. Solution Approach

I read deliveries from a CSV file and validate each row (numeric fields, positive weight, weight within vehicle capacity, no duplicate IDs). Valid deliveries are sorted by priority first, then by area, then by weight descending, then by ID as a final tiebreaker — this ordering does several things at once: it puts urgent deliveries first, area is a secondary key so same-area deliveries within a priority tier cluster together, and sorting heaviest-first within an area helps the packer fill trips more tightly. I then walk the sorted list once and greedily pack deliveries into trips using **first-fit**: for each delivery, check every trip opened so far (in order) and drop it into the first one that still has room; only open a new trip if none of the existing ones fit. This is a single deterministic pass — no backtracking, no re-shuffling — but unlike a simpler "next-fit" approach (which only checks the most recently opened trip), first-fit lets earlier trips keep receiving deliveries throughout the whole run instead of being left half-empty.

### Approaches Explored

I tried three packing strategies before settling on first-fit:

**1. Next-fit (initial version)** — only ever check the single most recently opened trip; if the next delivery doesn't fit, close that trip for good and open a new one. Simplest possible packing loop, but it can strand a trip at low utilization if a delivery just barely doesn't fit — that trip is never revisited even if a later, smaller delivery would've fit perfectly.

On the sample dataset (5 deliveries), this produced 3 trips, with one trip sitting at only 20% utilization (a single 2.0kg delivery) because the next delivery in sort order was too heavy for it and a new trip was opened instead:

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

**Why I kept first-fit:** it produces the same 100%-utilization result as knapsack DP on the sample data, stays O(n log n), and is something I can trace by hand and explain line-by-line in an interview — which the brief explicitly says matters more than a sophisticated solution I can't fully explain.

## 2. Most Difficult Part

Deciding how to resolve the conflict between priority ordering and area grouping, since the challenge doesn't say which one wins. I settled on priority as the primary sort key and area as the secondary key, so priority is never violated, and area grouping happens "where reasonably possible" — meaning only among deliveries that already share a priority level. Writing that rule down precisely, and making sure the same input always produces the same output, took more thought than the actual trip-packing logic.

## 3. Where the Grouping Isn't Optimal

Yes. Since area grouping only applies within a priority tier, two same-area deliveries with different priorities will never end up in the same trip, even if combining them would've made for a tighter route. Also, first-fit is still a greedy heuristic, not an exhaustive search: it places each delivery into the *first* trip with room rather than searching for the *best* combination of deliveries per trip, so it can still end up with slightly more unused capacity than a true optimal packer (which I explored with a knapsack DP approach — see "Approaches Explored" above). I didn't ship the fully optimal version since bin-packing is NP-hard in general, and the knapsack DP alternative trades meaningfully worse time complexity for a small utilization gain that didn't show up on the sample data anyway.

## 4. At 1,000,000 Deliveries

Sorting is the main cost — O(n log n), so about 20 million comparisons, which is fine on modern hardware but not instant. The bigger concern is memory: I load the entire delivery list into memory before sorting and planning, so with a million rows that's a sizable in-memory list, and generating the full report string in memory before writing it out would also grow proportionally. For a truly huge input I'd want to stream the CSV and write the report incrementally instead of building everything in memory at once.

## 5. With Another Day

I'd finish and benchmark the knapsack DP planner I explored (see "Approaches Explored") on larger, more varied datasets to see whether its tighter packing is worth the extra time cost in practice, rather than deciding purely from complexity analysis. I'd also stream the input/output for large files instead of holding everything in memory, and add a couple more validation edge cases (e.g. handling CSV files with a UTF-8 BOM, or configurable strategies for what "same area" should mean when names are inconsistently formatted).
