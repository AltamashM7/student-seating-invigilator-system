# First Evaluation Checklist

## S1
- [ ] Current repository contains Java Servlet structure, not Node/Express runtime files
- [ ] MySQL schema creates successfully
- [ ] User stories and constraint rules are available
- [ ] ER, Use Case and Level-0 DFD are available
- [ ] Three role dashboard wireframes are available
- [ ] Master test plan, severity levels and test template are available

## S2
- [ ] Admin login works
- [ ] Faculty login works
- [ ] Invalid login is rejected
- [ ] Unauthorized admin API access is rejected
- [ ] Admin can create faculty/admin accounts
- [ ] Password rules are enforced
- [ ] Logout destroys the session
- [ ] Reset password works for matching account details

## S3
- [ ] Room CRUD works
- [ ] Seat rows are generated from room capacity
- [ ] Course CRUD works
- [ ] CSV import accepts valid rows
- [ ] Duplicate roll numbers are detected
- [ ] Invalid CSV rows are reported

## S4
- [ ] Exam create/read/update/delete flow works for allowed states
- [ ] State transitions follow the documented workflow
- [ ] Room overlap is detected
- [ ] Shared-student overlap is detected
- [ ] Non-conflicting exam is saved
- [ ] Timetable list and calendar views load
- [ ] Conflict warnings appear in the exam screen
