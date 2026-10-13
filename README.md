# Exam Planner browser version

Public address: https://exam-seating-planner.pages.dev/

This is the separate interactive familiarization copy hosted on Cloudflare Pages. The original application remains under `src/` and uses Java Servlets, JDBC, MySQL and Tomcat.

- Administrator: `admin` / `Admin@123`
- Faculty: `cse.faculty1` / `Faculty@123`
- Public seat lookup example: `CSE26Y1A001`
- Open User guide for practice steps. Reset sample data restores the original 240 fictional students, 480 enrollments, 24 exams and 480 seat assignments.

All accounts and data changes in this copy are simulated and stored in the current browser. Use fictional records and demo passwords. Clearing browser storage removes practice changes. Members do not share their practice data.

`engine.js` implements the training rules and data operations; `runtime.js` connects the existing page controllers to browser storage and simulated sessions. `guide.html` and `demo.css` contain the training help and controls. The remaining UI files are generated from the real webapp by `python scripts/build-pages-demo.py`.

Preview: `python -m http.server 33081 --bind 127.0.0.1`, then open `/demo/`. Check the simulator with `node scripts/test-pages-demo.mjs`.

Rebuild and publish using `wrangler pages deploy demo --project-name exam-seating-planner --branch main` from the repository root. Wrangler uses the existing Cloudflare account login; no token is stored in the project. The production branch for the Pages project is `main`. The existing GitHub Pages copy can also be updated by publishing this directory to the root of `gh-pages`. No Java, SQL, credentials for real services, compiled files or member ZIPs belong in the Pages output.
