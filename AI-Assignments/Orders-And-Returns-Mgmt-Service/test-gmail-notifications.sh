#!/bin/bash

# This script demonstrates how to trigger email functionality for Gmail addresses

# Set environment variables for email configuration
export EMAIL_USERNAME="your-email@gmail.com"
export EMAIL_PASSWORD="your-gmail-app-password"
export EMAIL_ENABLED=true

# Build the application
echo "Building application..."
mvn clean package

# Run the application with the local profile
echo "Starting application..."
java -jar target/order-returns-management-1.0.0.jar --spring.profiles.active=local &
APP_PID=$!

# Wait for the application to start
echo "Waiting for application to start..."
sleep 10

# Create a new order with a Gmail address
echo "Creating a new order with Gmail address..."
curl -X 'POST' \
  'http://localhost:8080/orders/order' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "transactionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "item": {
    "productId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "quantity": 1,
    "price": 100
  },
  "shippingAddress": "123 Main St, Anytown, USA",
  "email": "sainvrat@gmail.com"
}'

# Store the order ID from the response
ORDER_ID=$(curl -s -X 'POST' \
  'http://localhost:8080/orders/order' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "transactionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "item": {
    "productId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "quantity": 1,
    "price": 100
  },
  "shippingAddress": "123 Main St, Anytown, USA",
  "email": "sainvrat@gmail.com"
}' | jq -r '.orderId')

if [ -z "$ORDER_ID" ]; then
  echo "Failed to get order ID. Check if the application is running."
  kill $APP_PID
  exit 1
fi

echo "Order created with ID: $ORDER_ID"
echo "Check your Gmail inbox for the order confirmation email."

# Update order to PAID status to trigger payment confirmation email
echo "Updating order to PAID status..."
curl -X 'PATCH' \
  "http://localhost:8080/orders/$ORDER_ID/state" \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "newState": "PAID",
  "changedBy": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}'

echo "Check your Gmail inbox for the payment confirmation email."
sleep 5

# Update order to PROCESSING_IN_WAREHOUSE status
echo "Updating order to PROCESSING_IN_WAREHOUSE status..."
curl -X 'PATCH' \
  "http://localhost:8080/orders/$ORDER_ID/state" \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "newState": "PROCESSING_IN_WAREHOUSE",
  "changedBy": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}'

sleep 5

# Update order to SHIPPED status to trigger shipment email
echo "Updating order to SHIPPED status..."
curl -X 'PATCH' \
  "http://localhost:8080/orders/$ORDER_ID/state" \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "newState": "SHIPPED",
  "changedBy": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}'

echo "Check your Gmail inbox for the shipment notification email."
sleep 5

# Update order to DELIVERED status to trigger delivery email
echo "Updating order to DELIVERED status..."
curl -X 'PATCH' \
  "http://localhost:8080/orders/$ORDER_ID/state" \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "newState": "DELIVERED",
  "changedBy": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}'

echo "Check your Gmail inbox for the delivery confirmation email."

# Clean up
echo "Stopping application..."
kill $APP_PID

echo "Email functionality test complete!"