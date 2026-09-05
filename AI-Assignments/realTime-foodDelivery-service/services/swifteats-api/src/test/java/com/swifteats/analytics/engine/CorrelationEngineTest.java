package com.swifteats.analytics.engine;

import com.swifteats.analytics.model.CorrelationResult;
import com.swifteats.analytics.model.EnrichedOrder;
import com.swifteats.common.domain.DomainLabels;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationEngineTest {

    private final CorrelationEngine engine = new CorrelationEngine();

    @Test
    void correlatesStockoutWithWarehouseStockDelay() {
        EnrichedOrder order = sample(
                DomainLabels.FailureReason.STOCKOUT,
                "Stock delay on item",
                null, null, null, false, true);

        CorrelationResult result = engine.correlate(order);

        assertThat(result.matches()).extracting(m -> m.ruleId()).contains("STOCKOUT_WAREHOUSE");
    }

    @Test
    void correlatesTrafficTripleConfirm() {
        EnrichedOrder order = sample(
                DomainLabels.FailureReason.TRAFFIC_CONGESTION,
                null,
                DomainLabels.FleetNote.HEAVY_CONGESTION,
                DomainLabels.Traffic.HEAVY,
                null,
                false,
                true);

        CorrelationResult result = engine.correlate(order);

        assertThat(result.matches()).extracting(m -> m.ruleId()).contains("TRAFFIC_TRIPLE_CONFIRM");
    }

    @Test
    void correlatesSlaBreachForDelayedNonFailedOrders() {
        EnrichedOrder order = sample(null, null, null, null, null, true, false);

        CorrelationResult result = engine.correlate(order);

        assertThat(result.matches()).extracting(m -> m.ruleId()).contains("SLA_BREACH");
    }

    private static EnrichedOrder sample(
            String failureReason,
            String warehouseNotes,
            String gpsNotes,
            String traffic,
            String weather,
            boolean delayed,
            boolean failed) {
        return new EnrichedOrder(
                1L, "Pune", failed ? "Failed" : "Delivered", failureReason,
                delayed, failed, 1L, "Test Client", 1L, "WH-1",
                warehouseNotes, gpsNotes, 1L, traffic, weather, null,
                null, null, null);
    }
}
