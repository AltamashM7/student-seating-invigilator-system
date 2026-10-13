# Seating algorithm

SeatingAllocator is a pure Java service; JDBC persistence is in SeatingDao. Immutable records represent students and seats. Explicit insertion sorts copy/sort the lists; hash maps and sets index IDs and occupied seats. Greedy search picks the first unused eligible seat.

Steps: reject duplicate inputs; validate/pin manual assignments; sort students by accommodation priority, case-insensitive roll number and ID; sort seats by room label, room ID, row and column; allocate unpinned accommodation students to accessible seats first; allocate other students to non-accessible seats first, using remaining accessible seats only as a capacity fallback; reject insufficient usable/accessibility capacity. Preserved regular manual seats can consume accessibility capacity; reject the plan if too little remains rather than silently moving a manual seat.

Worked example: A01 needs accessibility; A02/A03 do not. R101/S001 is accessible, S002/S003 are ordinary. Full layout assigns A01 to S001, A02 to S002, A03 to S003 regardless of input order. With horizontal spacing, S002 is blocked; add another eligible odd-column seat to accommodate all three. Capacity uses geometry and eligibility rather than room nominal capacity alone.

Worst-case time is `O(n²+m²+n*m)` for n students and m seats (two insertion sorts and candidate searches); auxiliary space is `O(n+m)`. This is a transparent coursework implementation. The 2,000-student load test records actual runtime in `docs/testing/evidence/algorithm-results.txt`; it varies by machine and excludes JDBC/browser work.

The standalone runner executes 23 allocation checks and 17 checks of the same `ManualSeatingRules` service used by the DAO. These cover priority and reservation/fallback, deterministic ordering, uniqueness, manual pins, spacing, capacity, invalid inputs, partial last rows, load, both sides of a swap and input immutability. The focused Sprint 7 API checks cover persistence/rollback, active-exam accommodation tagging, preview revisions, pin retention/replacement, audit and exam state restrictions.

`SeatingDao` computes a proposed allocation before writing rows. Standard regeneration validates and retains manual pins. A confirmed accommodation change releases an incompatible pin for the tagged student only, validates all affected active generated plans, then saves the global flag and those plans in one transaction. Preview revisions fingerprint the relevant roster, layout, requirements and assignment snapshots. The shared planner lock prevents validation and persistence from interleaving with other writes.

Priority is implemented through sorting rather than a literal PriorityQueue; exam conflicts use SQL joins rather than a materialized graph. KPI core-subject concepts are conceptual mappings, not a claim of C/C++ code or absent data structures.
