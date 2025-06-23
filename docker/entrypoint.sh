#!/bin/bash
set -e

echo "Starting Facto entrypoint script..."

# Copy missing config files from default to mounted conf dir
echo "Copying default configuration files to /opt/facto/conf..."
for file in "/opt/facto-install/conf"/*; do
  target="/opt/facto/conf/$(basename "$file")"
  if [ ! -e "$target" ]; then
    cp "$file" "$target"
  fi
done


# Setup environment variables in to the configuration file
CONF_FILE="/opt/facto/conf/application.conf"

# Replace http.secret.key
echo "Setup app config"
echo "APP_SECRET_KEY"
[ -n "$APP_SECRET_KEY" ] && \
  sed -i "s|^\(\s*http\.secret\.key\s*=\s*\).*|\1\"$APP_SECRET_KEY\"|" "$CONF_FILE"

# Compose JDBC URL from env vars
echo "Database"
if [[ -n "$DB_HOST" && -n "$DB_PORT" && -n "$DB_NAME" && -n "$DB_USER" && -n "$DB_PASS" ]]; then
  JDBC_URL="jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?user=$DB_USER&password=$DB_PASS&disableMariaDbDriver"

  perl -pi -e "s|jdbc:mysql://localhost/facto\\?user=mysqluser&password=mysqlpassword|$JDBC_URL|g" "$CONF_FILE"
else
  echo "One or more required DB env vars are missing!" >&2
  exit 1
fi

# Replace default password
echo "Setup default password"
[ -n "$DEFAULT_PASSWORD" ] && \
  sed -i "s|^\(\s*setup\.defaultPassword\s*=\s*\).*|\1\"$SETUP_DEFAULT_PASSWORD\"|" "$CONF_FILE"


# Check if the database is ready
WAITED=0
echo "Waiting for MySQL to start..."
until mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "SELECT 1;" >/dev/null 2>&1; do
  if [ "$WAITED" -ge "$DB_MAX_RETRIES" ]; then
    echo "Timed out waiting for MySQL to start."
    exit 1
  fi
  echo "Still waiting for MySQL... ($WAITED/$DB_MAX_RETRIES)"
  sleep 2
  WAITED=$((WAITED + 1))
done


# Check if the database is initialized
echo "Checking if the database is initialized..."
TABLE_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" \
  -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${DB_NAME}';")

if [ "$TABLE_COUNT" -lt 1 ]; then
  echo "Database not initialized. Running setup mode..."
  /opt/facto/bin/server -DdropAndCreateNewDb
  echo "Create admin user..."
  /opt/facto/bin/server -DcreateAdminUser
  echo "Database setup complete."
fi


# Run the facto binary with all passed arguments
echo "Starting Facto server"
exec /opt/facto/bin/server "$@"
