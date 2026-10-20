# Invigilator allocation

Implementation: `InvigilationAllocator` with persistence and previews in `InvigilationDao`.

1. Use the selected active examinations' completed seating plans. For each occupied room, create `ceil(seated students / students per invigilator)` duty slots. One faculty member can take only one slot per exam.
2. Keep accepted/manual assignments unless full replacement is explicitly selected. Validate every kept assignment before using it.
3. Consider only faculty with login accounts and eligibility. Exclude recorded unavailable intervals, teaching/course conflicts, optional own-department exclusions, overlapping duties, insufficient rest and exceeded daily/yearly limits. A faculty member who declined that examination is excluded from automatic generation.
4. Process the most constrained slots first, breaking ties by date/time, exam, room and slot number.
5. Rank eligible faculty by assigned minutes in the academic year, then duty count, employee code and ID. Try the lowest-loaded candidate first.
6. If a later slot cannot be filled, backtrack and try another candidate. The search is bounded at 100,000 explored states, with at most 256 requested slots per batch. Reaching a bound asks the user to allocate fewer exams; it never saves a partial plan.
7. Return a complete proposed mapping. The DAO checks its revision and saves it with notifications and history in a transaction. The planner lock prevents competing administrative saves from interleaving.

This is a deterministic fairness heuristic with backtracking, not a claim of globally optimal workload. Daily limits span all academic years; yearly limits and balancing are scoped to the exam's academic year. Cancelled and declined duties do not consume workload; completed duties remain part of that year's historical workload.

Manual replacement uses the same eligibility checks. Faculty responses use an assignment version so a stale schedule cannot accept a replacement duty by mistake.
