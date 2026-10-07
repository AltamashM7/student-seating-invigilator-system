# Seating algorithm

SeatingAllocator is a pure Java service; JDBC persistence is in SeatingDao. Immutable records represent students and seats. Explicit insertion sorts copy/sort the lists; hash maps and sets index IDs and occupied seats. Greedy search picks the first unused eligible seat.

Steps: reject duplicate inputs; validate/pin manual assignments; sort students by accommodation priority, case-insensitive roll number and ID; sort seats by room label, room ID, row and column; allocate unpinned students to the first eligible unused seat; reject insufficient usable/accessibility capacity. Preserved regular manual seats can consume accessibility capacity; reject the plan if too little remains rather than silently moving a manual seat.

Worked example: A01 needs accessibility; A02/A03 do not. R101/S001 is accessible, S002/S003 are ordinary. Full layout assigns A01 to S001, A02 to S002, A03 to S003 regardless of input order. With horizontal spacing, S002 is blocked; add another eligible odd-column seat to accommodate all three. Capacity uses geometry and eligibility rather than room nominal capacity alone.

Worst-case time is `O(n²+m²+n*m)` for n students and m seats (two insertion sorts and candidate searches); auxiliary space is `O(n+m)`. This is a transparent coursework implementation. The 2,000-student load test records actual runtime in `docs/testing/evidence/algorithm-results.txt`; it varies by machine and excludes JDBC/browser work.

Seventeen pure Java checks cover full allocation, accommodation priority, deterministic ordering, uniqueness, valid pins, spacing, both capacity failures, duplicate/invalid/unknown pins, duplicate inputs, empty input, load and input permutation. Integration checks cover persistence/rollback, both sides of swaps, pin retention, audit and publication.

Priority is implemented through sorting rather than a literal PriorityQueue; exam conflicts use SQL joins rather than a materialized graph. KPI core-subject concepts are conceptual mappings, not a claim of C/C++ code or absent data structures.
