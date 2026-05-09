package com.billanalysis.api;

import com.billanalysis.agent.BillAnalysisAgent;
import com.billanalysis.agent.tools.StatsTool;
import com.billanalysis.guardrail.GroundTruthChecker;
import com.billanalysis.parser.BillRecord;
import com.billanalysis.parser.CsvBillParser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.StringReader;
import java.util.List;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class AnalysisController {

    private final CsvBillParser csvBillParser;
    private final BillAnalysisAgent billAnalysisAgent;
    private final StatsTool statsTool;
    private final GroundTruthChecker groundTruthChecker;

    /**
     * POST /api/bills/analyze
     * Body: { "question": "...", "csvContent": "date,type,..." }
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(@RequestBody AnalysisRequest request) throws Exception {
        long start = System.currentTimeMillis();
        validateRequest(request);

        // 1. Parse CSV
        List<BillRecord> bills = csvBillParser.parse(new StringReader(request.getCsvContent()));

        // 2. Run agent (sets BillSessionContext internally)
        String answer = billAnalysisAgent.analyze(request.getQuestion(), bills);

        // 3. Ground-truth guardrail — compare AI figures against deterministic calculation
        StatsTool.StatsResult groundTruth = statsTool.calculateStats();
        GroundTruthChecker.ValidationResult validation = groundTruthChecker.validate(answer, groundTruth);

        return ResponseEntity.ok(AnalysisResponse.builder()
                .answer(answer)
                .groundTruthValid(validation.valid())
                .validationMessage(validation.message())
                .processingTimeMs(System.currentTimeMillis() - start)
                .build());
    }

    private void validateRequest(AnalysisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new IllegalArgumentException("question is required");
        }
        if (request.getCsvContent() == null || request.getCsvContent().isBlank()) {
            throw new IllegalArgumentException("csvContent is required");
        }
    }
}
