# Sprint 7: seating refinement, accommodations and manual overrides

The scope follows the user's Sprint 7 issue: define the manual-change, accommodation-priority and regeneration rules; implement their UI and APIs; verify the rules through unit and application tests. The original `exam_seating_sprint_plan.md` labels the matching work **Sprint 6** and calls its next sprint invigilator allocation. The user's issue is the authority for this local delivery.

## Manual override workflow

1. Select a draft or scheduled examination.
2. Click an occupied chart seat or select its student in **Manual seating changes**.
3. Select an empty target to move the student, or an occupied target to swap both students. The chart highlights source and target; invalid targets are disabled.
4. Enter a reason and review the old/new seats in the confirmation dialog.
5. Save. The server revalidates the roster, exam state, current assignment, target room, occupancy, spacing and both students' accessibility inside one transaction.
6. The changed assignments are marked manual. Each affected student gets an audit event containing the actor, time, reason and seat labels.

Capacity is a hard limit: manual operations replace existing assignments within the examination's physical layout; they cannot create additional seats or assign outside selected rooms. Completed and cancelled examinations cannot be moved, swapped or regenerated. Cancelled or draft seating may be cleared; its history is retained.

## Accommodation priority and tagging

The supported PWD/accommodation is **accessible seating required**. Staff record a physical seating requirement rather than a diagnosis. Qualification requires both an accessible room entry and an individually marked accessible seat. Staff configure those flags based on the actual room/seat suitability, including barrier-free access and adequate aisle space; the application does not infer suitability from a room number.

Automatic allocation works in this order:

1. Validate and preserve existing manual assignments.
2. Pre-allocate qualifying seats to students requiring accessible seating, ordered by roll number and ID.
3. Allocate other students to eligible regular seats.
4. Use remaining accessible seats for other students only when regular capacity is insufficient.

Horizontal spacing remains a hard exam constraint: only odd columns qualify. No confirmation can bypass accessibility, room membership, capacity or spacing. A mixed-course/section adjacency-warning policy is outside the accepted seating model. This policy preserves the rules already agreed for this application's S5/S6 delivery.

Students' profiles contain the flag, and the chart displays **Accommodation required** independently of **Accessible seat**. The seating page can tag an enrolled student before or after allocation:

- The requirement applies to the student across academic years.
- A preview lists every generated active examination affected and the number of seat changes.
- A confirmed update reallocates all those plans in one transaction. Other students' valid manual assignments are preserved.
- An incompatible manual pin for the tagged student is released. Its replacement is automatic and audited.
- Ungenerated examinations remain ungenerated. Completed/cancelled plans are not regenerated.
- If any affected exam lacks qualifying capacity or has an invalid remaining pin, the flag and all plans remain unchanged.
- Tagging and the resulting seat changes are audited separately.

## Regeneration workflow

**Regenerate seating** first computes a read-only preview. The dialog shows how many seat positions change, which students move, and how many manual assignments are preserved or replaced. The administrator supplies a reason.

Standard regeneration preserves valid manual assignments. Full replacement requires the advanced **Replace manual assignments too** checkbox, a second acknowledgement in the dialog, and the server-side `confirmReplaceManual=true` flag. Cancelling the dialog makes no changes and resets replacement mode.

The preview carries a revision derived from the current exam, roster, requirements, layout and assignments. Saving a stale reviewed plan fails and refreshes the chart. Accommodation previews include all affected active exam snapshots, so a change in another exam also invalidates the preview. All validation completes before assignment rows are replaced.

The UI shows validation/saving progress and prevents repeated submissions during an operation.

## API contract

All routes require an ADMIN session. Mutations are form-encoded. `examId` is required.

| Request to `/api/admin/seating` | Parameters | Result |
|---|---|---|
| GET chart | `examId` | Exam, roster, seat/requirement/manual flags, history and `revision` |
| GET regeneration preview | `action=preview`, `replaceManual` | Proposed seat changes and manual preservation/replacement counts |
| GET accommodation preview | `action=previewAccommodation`, `studentId`, `requiresAccessible` | Affected exams, changed-seat count, released-pin count and revision |
| POST generate/regenerate | `action=generate` or `regenerate`, `reason`, `replaceManual`, `expectedRevision` | Validated allocation; full replacement additionally requires `confirmReplaceManual=true` |
| POST move/swap | `action=reassign` or `swap`, `studentId`, `seatId`, `reason`, `expectedRevision` | One or two validated manual assignment changes |
| POST accommodation | `action=accommodation`, `studentId`, `requiresAccessible`, `reason`, preview's `expectedRevision` | Atomic global flag and affected-plan update |
| POST clear | `action=clear`, `reason`, `expectedRevision` | Removes eligible seating while retaining history |

`requiresAccessible` accepts `true/false` or `1/0`. Reasons must contain 1–250 non-whitespace characters. The browser always sends a reviewed revision. Legacy generate/move/swap/clear callers may omit it; accommodation changes require it. Invalid read/preview inputs return HTTP 400; invalid/conflicting mutations return HTTP 409. Failed mutations save nothing.

## Acceptance evidence

- `scripts/test-algorithm.ps1` executes allocation and manual-rule unit checks independently of Maven's test discovery.
- `scripts/test-sprint7.py --confirm-disposable --base-url http://127.0.0.1:33090/exam-seating` executes focused API cases against a separate disposable application/database. It must not target the normal demo or a shared database.
- Browser checks cover chart source/destination selection, tagging preview/save and requirement badges, manual markers, regeneration preservation, replacement acknowledgement, cancellation, selectors and progress/feedback.
- Results and screenshots are under `docs/testing/evidence`; the current KPI report records the executed counts.
- No database-schema migration is needed; the existing student flag, seat/room flags, manual markers and history support the workflow.
