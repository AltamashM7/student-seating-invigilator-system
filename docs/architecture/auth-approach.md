# Authentication Approach

Authentication uses the `users` table and an HTTP session.

- Admin and faculty accounts log in with username and password.
- Passwords are stored as SHA-256 hashes for this academic implementation instead of plain text.
- Successful login stores user id, username and role in `HttpSession`.
- Admin and faculty API paths are checked by `AuthFilter`.
- Registration is restricted to an already logged-in administrator.
- Reset password requires an ADMIN session and matching username/email before replacing the stored hash. Unauthenticated recovery is rejected.
- Session timeout is 30 minutes.

Student login is not required in the supplied functional-requirement table. Timetable and exact-roll published seating lookup are public/read-only. The baseline SHA-256 hashing is retained for this academic implementation; production password-storage/security hardening is outside this delivery.
