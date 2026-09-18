# DriveEase integration test branch

This branch assembles the existing Java 17 / Spring Boot 3.3 vehicle rental app
for integration testing. The shared application is under `app/`.

## Run locally

Open `app/pom.xml` in IntelliJ with JDK 17 or newer. In the IntelliJ terminal,
from the `app` folder:

```powershell
Copy-Item sample-data\vehicles.txt vehicles.txt
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The app writes local records such as `admins.txt`, `customer.txt`,
`booking.txt`, `billing.txt`, and `maintenance.txt`; these are ignored by Git.
Use only demo data. The current source has known gaps in branch management,
handover/return, and parts of validation and billing.

## MySQL design

[`app/database/schema.sql`](app/database/schema.sql) is a MySQL 8 schema draft
for the planned database version. Its tables do not yet receive data from the
running app: the current services are file-backed and Maven has no MySQL driver
or JPA dependency. See [`app/database/README.md`](app/database/README.md) for
schema inspection steps. Do not claim a database CRUD demo until persistence
is connected and verified.

This test branch is separate from `main` and from the six member branches.
It is an assembled preview, not a replacement for each member's commits.
