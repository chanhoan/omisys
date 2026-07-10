# FCM database migrations

Apply each script to its owning service database before deploying application code:

1. `user/20260610-user-devices.sql` to the user-service MySQL database.
2. `notification/20260610-notification-device.sql` to the notification-service MySQL database.
3. `user/20260709-seed-admin-account.sql` to the user-service MySQL database — replace `:BCRYPT_HASH` with a freshly generated BCrypt hash before running; never commit the real hash or plaintext password.

The repository does not currently run Flyway or Liquibase, so these scripts are operational migrations and are not applied automatically.
