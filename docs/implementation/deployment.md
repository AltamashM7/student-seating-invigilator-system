# Setup and deployment

## Database

Use Java 17+, MySQL 8, Maven and Tomcat 10.1+. The application continues to use Java Servlets, JDBC and plain HTML/CSS/JavaScript, without a scheduling framework or external notification API.

Fresh database:

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
# Optional fictional records:
mysql -u root -p < database/demo-data.sql
```

Existing S1-S7 database: back it up, stop the application, then run only:

```bash
mysql -u root -p < database/migrations/002_complete_project.sql
```

In Windows PowerShell, use `Get-Content -Raw database/migrations/002_complete_project.sql | mysql -u root -p`. The migration retains records and is safe to rerun after a partial failure. MySQL schema changes are not a rollback transaction. The isolated local startup script handles migration 002 automatically.

For the original S1-S4 database, first apply `001_s1_s6_upgrade.sql`, then `002_complete_project.sql`. Never reload the fresh schema over an existing database.

Configure the application process with `DB_URL`, `DB_USER` and `DB_PASSWORD`. For deployment use a dedicated database account and a real password. The empty local-demo password belongs only to the isolated loopback instance at port 33307.

## Build and Tomcat

```bash
mvn -Dmaven.test.skip=true clean package
```

Deploy `target/exam-seating.war` in Tomcat's `webapps` directory. The normal URL is `http://localhost:8080/exam-seating/`. This workspace's isolated script uses `http://127.0.0.1:33080/exam-seating/` instead.

Passwords use salted PBKDF2. Existing SHA-256 accounts remain usable and upgrade on successful login, after migration 002 has expanded the password column. Keep default demonstration accounts out of a public production database.

Serve production traffic over HTTPS. When using a trusted reverse proxy, configure Tomcat's proxy scheme/port or its trusted RemoteIpValve so requests are recognized as HTTPS and same-origin checks use the public origin. Enable secure session cookies in that deployment's web.xml/Tomcat configuration. Sessions are HttpOnly, use cookie tracking and request SameSite=Lax. Do not expose the isolated development database or its ports publicly.

## Email

Set these environment variables in the Tomcat process, then restart Tomcat:

```text
SMTP_HOST=smtp.your-provider.example
SMTP_PORT=587
SMTP_MODE=STARTTLS
SMTP_FROM=exams@your-college.example
SMTP_USERNAME=your-smtp-account
SMTP_PASSWORD=your-smtp-password-or-app-password
```

Implicit TLS is also supported with `SMTP_MODE=TLS`, typically port 465. Server certificates and hostnames are validated. Passwords stay in the environment and are not stored in the repository. Configure authentication as required by the provider.

Notifications create an in-app record and an outbox entry. Missing configuration is shown as Disabled. Once configured, an administrator can queue disabled/failed entries and use **Send next 20 queued emails**. A successful SMTP acceptance marks an entry Sent; failure is shown without losing the in-app notice. There is no automatic claim of recipient inbox delivery. The browser training copy never sends email.

A send interrupted by a server crash can remain Sending. After 30 minutes the administrator can explicitly queue it again. Check whether it was received first, because SMTP cannot guarantee exactly-once delivery across an interrupted connection.

## Backups and updates

Back up the database before deployment and migrations, and retain the prior WAR. Preserve database records and history; don't run the disposable demo reset on production. If rolling back code after enabling PBKDF2, keep an application version that understands the upgraded password format.

Production hosting, HTTPS/domain setup, SMTP credentials, manual acceptance and KPI verification are deployment/evaluation tasks. This local implementation does not push repository changes or publish hosting updates.
