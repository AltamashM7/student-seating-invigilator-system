# Authentication Approach

Authentication uses the `users` table and an HTTP session.

- Admin and faculty accounts log in with username and password.
- Passwords are stored as SHA-256 hashes for this academic implementation instead of plain text.
- Successful login stores user id, username and role in `HttpSession`.
- Admin and faculty API paths are checked by `AuthFilter`.
- Registration is restricted to an already logged-in administrator.
- Reset password checks username and registered email before replacing the stored hash.
- Session timeout is 30 minutes.

Student login is not required in the supplied functional-requirement table. Student-facing timetable pages are therefore read-only/public for the S1-S4 build.
