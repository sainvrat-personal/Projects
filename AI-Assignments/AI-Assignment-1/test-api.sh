#!/bin/bash

# Base URL for the API
BASE_URL="http://localhost:8080/api"

echo "--- Starting Inventory Service API Test ---"

# ==============================================================================
# 1. CREATE Operations
# ==============================================================================
echo -e "\n--- 1. Testing CREATE operations ---"

# Create a new category
echo "Creating a new category 'Electronics'..."
CATEGORY_RESPONSE=$(curl -s -X POST "${BASE_URL}/categories" \
    -H "Content-Type: application/json" \
    -d '{"name": "Electronics"}')
CATEGORY_ID=$(echo ${CATEGORY_RESPONSE} | jq -r '.id')
echo "Category 'Electronics' created with ID: ${CATEGORY_ID}"
echo "${CATEGORY_RESPONSE}" | jq .

# Create a new product in the category
echo -e "\nCreating a new product 'Laptop'..."
PRODUCT_RESPONSE=$(curl -s -X POST "${BASE_URL}/products?categoryId=${CATEGORY_ID}" \
    -H "Content-Type: application/json" \
    -d '{"name": "Laptop"}')
PRODUCT_ID=$(echo ${PRODUCT_RESPONSE} | jq -r '.id')
echo "Product 'Laptop' created with ID: ${PRODUCT_ID}"
echo "${PRODUCT_RESPONSE}" | jq .

# Add an SKU to the product
echo -e "\nAdding an SKU to the 'Laptop' product..."
SKU_RESPONSE=$(curl -s -X POST "${BASE_URL}/products/${PRODUCT_ID}/skus" \
    -H "Content-Type: application/json" \
    -d '{"sku": "LP-123", "price": 1200.00, "quantity": 10}')
SKU_ID=$(echo ${SKU_RESPONSE} | jq -r '.id')
echo "SKU 'LP-123' added with ID: ${SKU_ID}"
echo "${SKU_RESPONSE}" | jq .


# ==============================================================================
# 2. READ Operations (Verify Creation)
# ==============================================================================
echo -e "\n--- 2. Testing READ operations (verifying creation) ---"

echo "Getting all categories..."
curl -s "${BASE_URL}/categories" | jq .

echo -e "\nGetting category by ID: ${CATEGORY_ID}"
curl -s "${BASE_URL}/categories/${CATEGORY_ID}" | jq .

echo -e "\nGetting all products..."
curl -s "${BASE_URL}/products" | jq .

echo -e "\nGetting product by ID: ${PRODUCT_ID}"
curl -s "${BASE_URL}/products/${PRODUCT_ID}" | jq .

echo -e "\nGetting all SKUs for product ID: ${PRODUCT_ID}"
curl -s "${BASE_URL}/products/${PRODUCT_ID}/skus" | jq .

echo -e "\nGetting SKU by ID: ${SKU_ID} for product ID: ${PRODUCT_ID}"
curl -s "${BASE_URL}/products/${PRODUCT_ID}/skus/${SKU_ID}" | jq .


# ==============================================================================
# 3. UPDATE Operations
# ==============================================================================
echo -e "\n--- 3. Testing UPDATE operations ---"

echo "Updating category ${CATEGORY_ID} to 'Consumer Electronics'..."
curl -s -X PUT "${BASE_URL}/categories/${CATEGORY_ID}" \
    -H "Content-Type: application/json" \
    -d '{"name": "Consumer Electronics"}' | jq .

echo -e "\nUpdating product ${PRODUCT_ID} to 'Gaming Laptop'..."
curl -s -X PUT "${BASE_URL}/products/${PRODUCT_ID}" \
    -H "Content-Type: application/json" \
    -d '{"name": "Gaming Laptop"}' | jq .

echo -e "\nUpdating SKU ${SKU_ID} to have price 1250.00 and quantity 8..."
curl -s -X PUT "${BASE_URL}/products/${PRODUCT_ID}/skus/${SKU_ID}" \
    -H "Content-Type: application/json" \
    -d '{"sku": "LP-123-UPDATED", "price": 1250.00, "quantity": 8}' | jq .


# ==============================================================================
# 4. READ Operations (Verify Update)
# ==============================================================================
echo -e "\n--- 4. Testing READ operations (verifying updates) ---"

echo "Getting category ${CATEGORY_ID} to verify update..."
curl -s "${BASE_URL}/categories/${CATEGORY_ID}" | jq .

echo -e "\nGetting product ${PRODUCT_ID} to verify update..."
curl -s "${BASE_URL}/products/${PRODUCT_ID}" | jq .

echo -e "\nGetting SKU ${SKU_ID} to verify update..."
curl -s "${BASE_URL}/products/${PRODUCT_ID}/skus/${SKU_ID}" | jq .


# ==============================================================================
# 5. DELETE Operations (in reverse order of creation)
# ==============================================================================
echo -e "\n--- 5. Testing DELETE operations ---"

echo "Deleting SKU ${SKU_ID}..."
curl -s -X DELETE "${BASE_URL}/products/${PRODUCT_ID}/skus/${SKU_ID}"

echo -e "\nDeleting Product ${PRODUCT_ID}..."
curl -s -X DELETE "${BASE_URL}/products/${PRODUCT_ID}"

echo -e "\nDeleting Category ${CATEGORY_ID}..."
curl -s -X DELETE "${BASE_URL}/categories/${CATEGORY_ID}"


# ==============================================================================
# 6. Final Verification (Verify Deletion)
# ==============================================================================
echo -e "\n--- 6. Verifying that all data has been deleted ---"

echo "Attempting to get deleted SKU ${SKU_ID} (should be empty or error)..."
curl -s "${BASE_URL}/products/${PRODUCT_ID}/skus" | jq .

echo -e "\nAttempting to get deleted Product ${PRODUCT_ID} (should be not found)..."
curl -s -w "\nHTTP Status: %{http_code}\n" "${BASE_URL}/products/${PRODUCT_ID}"

echo -e "\nAttempting to get deleted Category ${CATEGORY_ID} (should be not found)..."
curl -s -w "\nHTTP Status: %{http_code}\n" "${BASE_URL}/categories/${CATEGORY_ID}"

echo -e "\n\n--- Test Complete ---" 