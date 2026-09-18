# MySQL schema draft

`schema.sql` defines tables for the proposed MySQL-backed DriveEase system. It
uses MySQL 8 syntax and creates `vehicle_rental_db`, matching the URL already in
`application.properties`. It contains no passwords or real customer data.

In MySQL Workbench, open `schema.sql` and run the full script. From Command
Prompt with the MySQL client installed, run from this `database` directory:

```powershell
mysql -u root -p < schema.sql
```

Then inspect the result in Workbench, or run:

```sql
USE vehicle_rental_db;
SHOW TABLES;
SHOW CREATE TABLE bookings;
SELECT COUNT(*) FROM bookings;
```

The current Java classes are plain models, not JPA entities. Current CRUD
services use `admins.txt`, `customer.txt`, `vehicles.txt`, `booking.txt`,
`billing.txt`, and `maintenance.txt`. The Maven project also has no MySQL
driver or JPA dependency yet. **Creating these tables does not connect the
running app to them or migrate the text files.** Demonstrate a database insert
or delete only after the corresponding service has been moved to MySQL and the
result has been checked in the database.

`branches`, branch references, handovers, returns, and some status columns
represent planned features from the proposal. They are included for schema
review; the current app does not implement those flows. Before migration, agree on ID formats,
status values, password hashing, and a safe import of existing file data.
