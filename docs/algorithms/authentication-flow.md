# Authentication Flow

```text
START
  |
Read username and password
  |
Any field empty? -- yes --> show validation error --> END
  |
 no
  |
Search user by username
  |
User found? -- no --> invalid credentials --> END
  |
 yes
  |
Hash entered password and compare
  |
Matches? -- no --> invalid credentials --> END
  |
 yes
  |
Create HTTP session and store user id + role
  |
Open role dashboard
  |
END
```
