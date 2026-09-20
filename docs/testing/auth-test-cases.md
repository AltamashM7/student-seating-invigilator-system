# Authentication Test Cases

| ID | Scenario | Expected result |
|---|---|---|
| AUTH-01 | Correct admin username/password | Login succeeds and admin dashboard opens |
| AUTH-02 | Wrong password | 401 and no session is created |
| AUTH-03 | Missing username | 400 validation error |
| AUTH-04 | Faculty opens admin API | 403 access error |
| AUTH-05 | Logged-out user opens admin API | 401 access error |
| AUTH-06 | Weak password during registration | 400 validation error |
| AUTH-07 | Duplicate username/email | 409 duplicate error |
| AUTH-08 | Reset with matching username/email | Password changes |
| AUTH-09 | Reset with wrong email | Password does not change |
| AUTH-10 | Logout | Session is invalidated |
