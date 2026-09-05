# SwiftEats Domain Model — Assignment 1 + Shared Assignment 2

This document defines the **unified domain vocabulary** used by the live platform (`public` schema, UUID keys) and the Assignment 2 sample import (`analytics` schema, BIGINT keys). Field names and semantics align so analytics correlation does not require remapping later.

## Design principle

| Layer | Purpose |
|-------|---------|
| **Operational (`public.*`)** | Live SwiftEats — restaurants, orders, GPS, feedback emitted during A1 flows |
| **Analytics (`analytics.*`)** | Imported CSV sample dataset for A2 root-cause analysis |
| **`external_id` columns** | Optional bridge from live UUID rows to imported BIGINT ids when demoing both |

Shared literals (failure reasons, kitchen notes, traffic/weather) live in `com.swifteats.common.domain.DomainLabels`. Temporary/demo **display names** use the **`[T] `** prefix via `TemporaryDataLabels` (seed data, admin-created restaurants/menu/cuisines, registered customers).

---

## Entity mapping

| Live entity (A1) | Table | A2 analytics table | CSV file |
|------------------|-------|--------------------|----------|
| `BusinessClient` | `business_client` | `analytics.client` | `clients.csv` |
| `Restaurant` | `restaurant` | `analytics.warehouse` | `warehouses.csv` |
| `Driver` | `driver` | `analytics.driver` | `drivers.csv` |
| `Order` | `"order"` | `analytics.order` | `orders.csv` |
| `KitchenPrepLog` | `kitchen_prep_log` | `analytics.warehouse_log` | `warehouse_logs.csv` |
| `FleetDeliveryLog` | `fleet_delivery_log` | `analytics.fleet_log` | `fleet_logs.csv` |
| `CustomerFeedback` | `customer_feedback` | `analytics.feedback` | `feedback.csv` |
| `OrderExternalFactor` | `order_external_factor` | `analytics.external_factor` | `external_factors.csv` |

---

## Order — shared fields (A1 writes, A2 reads)

| Field | CSV column | Notes |
|-------|------------|-------|
| `client_id` | `client_id` | B2B segmentation |
| `customer_name`, `customer_phone` | same | Snapshot at order time; live orders copy prefixed customer/menu names (e.g. `[T] Demo Customer`, `[T] Kolhapuri Misal`) |
| `delivery_address_line1/2`, `city`, `state`, `pincode` | same | Address structure matches CSV |
| `status` | `status` | Includes `DELIVERED`, `FAILED`, `DELAYED`, etc. |
| `payment_mode`, `payment_status` | same | Async payment in A1 |
| `total_amount` | `total_amount` | |
| `promised_delivery_at` | `promised_delivery_date` | Instant in live; date in CSV |
| `actual_delivery_at` | `actual_delivery_date` | |
| `failure_reason` | `failure_reason` | Use `DomainLabels.FailureReason.*` |
| `delay_reason` | `delay_reason` | |
| `is_delayed`, `is_failed` | same | Updated on state transitions |
| `prep_started_at`, `prep_completed_at` | derived from kitchen log | Also on order for fast queries |
| `out_for_delivery_at`, `delivered_at` | lifecycle timestamps | |

---

## Restaurant ↔ Warehouse

| Live (`restaurant`) | Analytics (`warehouse`) |
|---------------------|-------------------------|
| `name` | `warehouse_name` |
| `address_line1/2`, `city`, `state`, `pincode` | same structure |
| `capacity` | `capacity` |
| `external_id` | `warehouse_id` |

---

## Kitchen prep ↔ Warehouse log

| `kitchen_prep_log` | `warehouse_log` |
|--------------------|-----------------|
| `order_id` | `order_id` |
| `restaurant_id` | `warehouse_id` |
| `picking_start/end`, `dispatch_time` | same |
| `notes` | `notes` (`DomainLabels.KitchenNote.*`) |

---

## Fleet ↔ Fleet log

| `fleet_delivery_log` | `fleet_log` |
|----------------------|-------------|
| `order_id`, `driver_id` | same |
| `vehicle_number`, `route_code` | same |
| `gps_delay_notes` | same (`DomainLabels.FleetNote.*`) |
| `departure_time`, `arrival_time` | same |

---

## Events emitted for A2 (future)

When order/tracking modules are implemented, each transition publishes domain events with the same field names:

- `OrderCreated`, `OrderStatusChanged` — includes `failureReason`, `delayReason`, timestamps
- `KitchenPrepUpdated` — mirrors `kitchen_prep_log`
- `FleetLegUpdated` — mirrors `fleet_delivery_log`
- `FeedbackSubmitted` — mirrors `customer_feedback`
- `ExternalFactorRecorded` — mirrors `order_external_factor`

These feed the A2 correlation engine without schema changes.

---

## Package layout

```
com.swifteats.client.*       BusinessClient
com.swifteats.restaurant.*   Restaurant, MenuItem, KitchenPrepLog
com.swifteats.order.*        Order, Customer, OrderStateHistory, OrderExternalFactor
com.swifteats.tracking.*     Driver, FleetDeliveryLog, DriverLocationArchive
com.swifteats.feedback.*     CustomerFeedback
com.swifteats.analytics.*    CSV import + future correlation (A2)
com.swifteats.common.domain  Enums + DomainLabels
```

Migration: `V3__operational_schema.sql`
