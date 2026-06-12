package com.billanalysis.api;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AnalysisResponse {
    private String answer;
    private boolean groundTruthValid;
    private double confidence;
    private List<BigDecimal> suspiciousFigures;
    private String validationMessage;
    private long processingTimeMs;
}
