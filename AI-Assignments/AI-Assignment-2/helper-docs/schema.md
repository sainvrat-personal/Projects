# Database Schema: Order & Returns Management System

This document describes the PostgreSQL tables, columns, types, constraints, and relationships for the backend system. All IDs are UUIDs. Each table includes `created_at` and `updated_at` timestamps. A single `transaction_id` is used to track the complete transaction across orders, returns, and state history.

---

## 1. orders
| Column           | Type      | Constraints                | Description                       |
|------------------|----------|----------------------------|-----------------------------------|
| id               | UUID      | PRIMARY KEY                | Unique order ID                   |
| transaction_id   | UUID      | NOT NULL                   | Links all related records         |
| customer_id      | UUID      | NOT NULL                   | FK to users.id                    |
| status           | VARCHAR   | NOT NULL                   | Order state (enum)                |
| shipping_address | TEXT      | NOT NULL                   | Shipping address                  |
| failure_reason   | TEXT      |                            | Error message if order fails      |
| created_at       | TIMESTAMP | DEFAULT now() NOT NULL     | Creation timestamp                |
| updated_at       | TIMESTAMP | DEFAULT now() NOT NULL     | Last update timestamp             |

## 2. order_items
| Column         | Type      | Constraints                | Description                       |
|----------------|----------|----------------------------|-----------------------------------|
| id             | UUID      | PRIMARY KEY                | Unique item ID                    |
| order_id       | UUID      | NOT NULL, FK orders.id     | Associated order                  |
| product_id     | UUID      | NOT NULL                   | Product reference                 |
| quantity       | INTEGER   | NOT NULL, CHECK > 0        | Quantity ordered                  |
| created_at     | TIMESTAMP | DEFAULT now() NOT NULL     | Creation timestamp                |
| updated_at     | TIMESTAMP | DEFAULT now() NOT NULL     | Last update timestamp             |

## 3. returns
| Column         | Type      | Constraints                | Description                       |
|----------------|----------|----------------------------|-----------------------------------|
| id             | UUID      | PRIMARY KEY                | Unique return ID                  |
| transaction_id | UUID      | NOT NULL                   | Links all related records         |
| order_id       | UUID      | NOT NULL, FK orders.id     | Associated order                  |
| status         | VARCHAR   | NOT NULL                   | Return state (enum)               |
| reason         | TEXT      |                            | Reason for return                 |
| failure_reason | TEXT      |                            | Error message if return fails     |
| created_at     | TIMESTAMP | DEFAULT now() NOT NULL     | Creation timestamp                |
| updated_at     | TIMESTAMP | DEFAULT now() NOT NULL     | Last update timestamp             |

## 4. state_history
| Column         | Type      | Constraints                | Description                       |
|----------------|----------|----------------------------|-----------------------------------|
| id             | UUID      | PRIMARY KEY                | Unique history ID                 |
| transaction_id | UUID      | NOT NULL                   | Links all related records         |
| entity_type    | VARCHAR   | NOT NULL                   | 'order' or 'return'               |
| entity_id      | UUID      | NOT NULL                   | FK to orders.id or returns.id     |
| state         | VARCHAR   | NOT NULL                   | State value                       |
| changed_by     | UUID      |                            | FK to users.id                    |
| timestamp      | TIMESTAMP | DEFAULT now() NOT NULL     | State change timestamp            |
| created_at     | TIMESTAMP | DEFAULT now() NOT NULL     | Creation timestamp                |
| updated_at     | TIMESTAMP | DEFAULT now() NOT NULL     | Last update timestamp             |

## 5. users
| Column         | Type      | Constraints                | Description                       |
|----------------|----------|----------------------------|-----------------------------------|
| id             | UUID      | PRIMARY KEY                | Unique user ID                    |
| name           | VARCHAR   | NOT NULL                   | User name                         |
| email          | VARCHAR   | NOT NULL, UNIQUE           | User email                        |
| role           | VARCHAR   | NOT NULL                   | 'customer', 'manager', etc.       |
| created_at     | TIMESTAMP | DEFAULT now() NOT NULL     | Creation timestamp                |
| updated_at     | TIMESTAMP | DEFAULT now() NOT NULL     | Last update timestamp             |

## 6. refunds
| Column         | Type      | Constraints                | Description                       |
|----------------|----------|----------------------------|-----------------------------------|
| id             | UUID      | PRIMARY KEY                | Unique refund ID                  |
| transaction_id | UUID      | NOT NULL                   | Links all related records         |
| return_id      | UUID      | NOT NULL, FK returns.id    | Associated return                 |
| amount         | NUMERIC   | NOT NULL, CHECK > 0        | Refund amount                     |
| status         | VARCHAR   | NOT NULL                   | Refund status (enum)              |
| payment_txn_id | VARCHAR   |                            | Mock payment gateway txn ID       |
| created_at     | TIMESTAMP | DEFAULT now() NOT NULL     | Creation timestamp                |
| updated_at     | TIMESTAMP | DEFAULT now() NOT NULL     | Last update timestamp             |

---

**Notes:**
- All IDs are UUIDs (use `uuid_generate_v4()` in PostgreSQL).
- `transaction_id` is generated at order creation and propagated to all related records (order, return, state history, refund).
- Foreign keys are enforced for referential integrity.
- Timestamps are stored for both creation and update events.
- Enum columns (`status`, `role`, etc.) should use PostgreSQL enums for strict value control.
- Additional indexes may be added for performance as needed.
