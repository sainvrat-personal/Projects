#!/bin/sh
# This script waits for Redis to be available before starting the main application.

set -e

# Loop until the redis-cli ping command succeeds
while ! redis-cli -h redis -p 6379 ping; do
  echo "Waiting for Redis to be available..."
  sleep 1
done

echo "Redis is up and running."

# Execute the main application command (the Spring Boot JAR)
exec java -jar app.jar 