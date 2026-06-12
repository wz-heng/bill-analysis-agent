package com.billanalysis.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * One row in src/test/resources/eval/fixtures.json. Defines a single test case
 * for the evaluation harness.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvaluationFixture(
        String id,
        String csvFile,
        String question,
        List<BigDecimal> expectedNumbers,
        List<String> expectedTools
) {}
