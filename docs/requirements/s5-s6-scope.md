# Accepted evaluator scope and S5-S6 acceptance

The user authorized local implementation on 4 October 2026, with no GitHub mutations. The evaluator requested S5/S6 completion, academic year/year-of-study and year/division counts for every department, practical CRUD, at least 50 realistic fictional records and KPI verification. The user authorized standard implementation decisions even where they differ from the original sprint sequence.

| Requirement | Acceptance evidence |
|---|---|
| Calendar academic year and study year | Course offerings, semester validation, annual student placements and year selector |
| Counts for all departments/years/divisions | Counts independent of multiple subject enrollments; zero-count department/year rows |
| S1-S4 fixes and administration | Working CRUD/import, room IDs, role protection, controlled errors, lifecycle/conflict validation |
| S5 seating | All-or-nothing allocation, room/seat chart, capacity and unique assignments |
| S6 accommodations | Accessible students first; valid accessible room/seat combinations |
| S6 manual operations | Swap/reassign validates both affected students; reason/actor/time/history |
| S6 regeneration | Preserve manual assignments by default; explicit full replacement |
| Realistic dummy data | 240 fictional students, 480 enrollments, multiple years/divisions/subjects and accommodation cases |
| KPI verification | Explicit algorithm runner, disposable API/integration/migration checks and browser evidence |
| Local team delivery | Complete source and non-overlapping member bundles; no generated Git authors or automatic pushes |

Invigilator allocation and later-sprint reporting/notifications remain out of this scope. Current rules are in `docs/implementation/current-rules.md`; first-evaluation S1-S4 documents remain historical context.
