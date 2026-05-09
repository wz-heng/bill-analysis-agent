package com.billanalysis.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalysisResponse {
    private String answer;
    private boolean groundTruthValid;
    private String validationMessage;
    private long processingTimeMs;
}
