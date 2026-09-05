package com.swifteats.analytics.service;

import com.swifteats.analytics.dto.InsightQueryRequest;
import com.swifteats.analytics.engine.CorrelationEngine;
import com.swifteats.analytics.engine.InsightGenerator;
import com.swifteats.analytics.exception.AnalyticsDataNotLoadedException;
import com.swifteats.analytics.model.QueryType;
import com.swifteats.analytics.repository.AnalyticsOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceTest {

    @Mock
    private AnalyticsOrderRepository repository;

    private AnalyticsQueryService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsQueryService(
                repository,
                new CorrelationEngine(),
                new InsightGenerator());
    }

    @Test
    void throwsWhenSampleDataMissing() {
        when(repository.hasSampleData()).thenReturn(false);

        assertThatThrownBy(() -> service.analyzeDelaysByCity("Pune", LocalDate.parse("2025-03-17")))
                .isInstanceOf(AnalyticsDataNotLoadedException.class);
    }

    @Test
    void buildsDelayInsightWithNarrative() {
        when(repository.hasSampleData()).thenReturn(true);
        when(repository.countAffectedByCityAndDate("Pune", LocalDate.parse("2025-03-17"))).thenReturn(10L);
        when(repository.failureReasonCountsByCityAndDate(eq("Pune"), any())).thenReturn(
                new LinkedHashMap<>(Map.of("Warehouse delay", 5L, "Traffic congestion", 3L)));
        when(repository.countHeavyTrafficByCityAndDate(any(), any())).thenReturn(3L);
        when(repository.countSlowPackingByCityAndDate(any(), any())).thenReturn(2L);
        when(repository.countNegativeFeedbackByCityAndDate(any(), any())).thenReturn(4L);
        when(repository.findAffectedByCityAndDate(any(), any(), anyInt())).thenReturn(List.of());

        var response = service.executeInsightQuery(new InsightQueryRequest(
                QueryType.DELAY_BY_CITY,
                Map.of("city", "Pune", "date", "2025-03-17")));

        assertThat(response.narrative()).contains("Pune");
        assertThat(response.evidence()).containsKey("failureReasons");
    }
}
