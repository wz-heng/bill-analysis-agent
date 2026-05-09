package com.billanalysis.api;

import lombok.Data;

@Data
public class AnalysisRequest {
    /** Natural-language question about the bills. */
    private String question;
    /** Raw CSV text (including header row). */
    private String csvContent;
}
