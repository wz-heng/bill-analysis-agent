package com.billanalysis.guardrail;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OutputValidatorTest {

    private final OutputValidator validator = new OutputValidator();

    @Test
    void extractsCommonCurrencyFormats() {
        assertThat(validator.extractNumbers("income ¥15,000.00, expense ￥3,200 and delta $-99.9"))
                .containsExactly(
                        new BigDecimal("15000.00"),
                        new BigDecimal("3200"),
                        new BigDecimal("-99.9"));
    }

    @Test
    void flagsImplausiblyLargeNumbers() {
        assertThat(validator.hasHallucination("total is 1000000001")).isTrue();
    }
}
