# Database migrations

Apply each script to its owning service database before deploying application code:

1. `mysql/01-init-schemas.sh` runs itself against the consolidated MySQL instance (`omisys-mysql`) — it creates the eight service schemas (user, product, order, payment, promotion, review, notification, delivery) and their per-schema accounts. It is mounted at `/docker-entrypoint-initdb.d/01-init-schemas.sh` and runs only on first start, while the `mysql-data` volume is still empty; drop that volume to re-run it. It creates the schemas the scripts below run inside, so it comes first.
2. `user/20260610-user-devices.sql` to the user-service MySQL database.
3. `notification/20260610-notification-device.sql` to the notification-service MySQL database.
4. `user/20260709-seed-admin-account.sql` to the user-service MySQL database — replace `:BCRYPT_HASH` with a freshly generated BCrypt hash before running; never commit the real hash or plaintext password.

Manual procedures (documents, not scripts):

- `product/cassandra-to-mysql.md` — moves product data from Cassandra into the `omisys_product` schema. Skip it and start from empty tables if the environment will be reseeded with fresh mock data.

Schema credentials come from the environment, not from placeholders in the file. `mysql/01-init-schemas.sh` reads three variables per service — `<SVC>_MYSQL_DATABASE`, `<SVC>_MYSQL_USER` and `<SVC>_MYSQL_PASSWORD` for each of `USER`, `PRODUCT`, `ORDER`, `PAYMENT`, `PROMOTION`, `REVIEW`, `NOTIFICATION` and `DELIVERY`. These are the same names the application services already use in `docker-compose.yml`, so one `.env` feeds both. Database and user default to `omisys_<svc>` when unset; a missing password aborts startup rather than creating an account with a wrong one.

Placeholder substitution: values written as `:NAME` are placeholders, never real secrets — substitute them at deploy time and keep the real values out of the repository. `user/20260709-seed-admin-account.sql` uses `:BCRYPT_HASH`.

The repository does not currently run Flyway or Liquibase, so these scripts are operational migrations and are not applied automatically.
