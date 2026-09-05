package com.swifteats.analytics.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CsvParsingUtilsTest {

    @Test
    void isFailedStatus_detectsFailedOrders() {
        assertThat(CsvParsingUtils.isFailedStatus("Failed")).isTrue();
        assertThat(CsvParsingUtils.isFailedStatus("Delivered")).isFalse();
    }

    @Test
    void isDelayed_whenActualAfterPromised() {
        LocalDateTime promised = LocalDateTime.of(2025, 4, 26, 23, 54);
        LocalDateTime actual = LocalDateTime.of(2025, 4, 27, 1, 0);
        assertThat(CsvParsingUtils.isDelayed(actual, promised)).isTrue();
    }

    @Test
    void isDelayed_whenActualMissing() {
        LocalDateTime promised = LocalDateTime.of(2025, 4, 26, 23, 54);
        assertThat(CsvParsingUtils.isDelayed(null, promised)).isFalse();
    }

    @Test
    void blankToNull_treatsEmptyAsNull() {
        assertThat(CsvParsingUtils.blankToNull("  ")).isNull();
        assertThat(CsvParsingUtils.blankToNull("value")).isEqualTo("value");
    }
}
