package com.billanalysis.eval;

import com.billanalysis.api.AnalysisRequest;
import com.billanalysis.api.AnalysisResponse;
import com.billanalysis.guardrail.OutputValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Layer-1 evaluation harness: runs each fixture against the live agent through
 * the HTTP controller, scores it (number coverage, intent match, confidence),
 * and prints an aggregate report. Tagged "eval" so it's excluded from the
 * default {@code mvn test} run — invoke with {@code mvn test -Dgroups=eval}.
 *
 * <p>Requires {@code DEEPSEEK_API_KEY} to be set in the environment.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("eval")
class EvaluationRunner {

    private static final BigDecimal NUMBER_TOLERANCE = new BigDecimal("0.01");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OutputValidator outputValidator;

    @Test
    void runEvaluationSuite() throws Exception {
        List<EvaluationFixture> fixtures = loadFixtures();
        List<FixtureResult> results = new ArrayList<>();

        for (EvaluationFixture fixture : fixtures) {
            results.add(runOne(fixture));
        }

        printReport(results);
    }

    private FixtureResult runOne(EvaluationFixture fixture) {
        try {
            String csv = loadCsv(fixture.csvFile());
            AnalysisRequest request = new AnalysisRequest();
            request.setQuestion(fixture.question());
            request.setCsvContent(csv);

            long start = System.currentTimeMillis();
            String body = mockMvc.perform(post("/api/bills/analyze")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            long elapsed = System.currentTimeMillis() - start;

            AnalysisResponse response = objectMapper.readValue(body, AnalysisResponse.class);
            return score(fixture, response, elapsed, null);
        } catch (Exception e) {
            return FixtureResult.failure(fixture, e.getMessage());
        }
    }

    private FixtureResult score(EvaluationFixture fixture, AnalysisResponse response,
                                long elapsedMs, String error) {
        String answer = response.getAnswer() == null ? "" : response.getAnswer();
        List<BigDecimal> answerNumbers = outputValidator.extractNumbers(answer);

        int numbersHit = 0;
        for (BigDecimal expected : fixture.expectedNumbers()) {
            BigDecimal target = expected.abs();
            boolean found = answerNumbers.stream().anyMatch(n ->
                    n.abs().subtract(target).abs().compareTo(NUMBER_TOLERANCE) <= 0);
            if (found) numbersHit++;
        }
        boolean numbersPass = numbersHit == fixture.expectedNumbers().size();

        List<String> actualTools = response.getToolsInvoked() == null
                ? List.of() : response.getToolsInvoked();
        Set<String> actualToolSet = new HashSet<>(actualTools);
        boolean toolPass = fixture.expectedTools().stream().allMatch(actualToolSet::contains);

        return new FixtureResult(
                fixture.id(),
                numbersPass,
                numbersHit,
                fixture.expectedNumbers().size(),
                toolPass,
                fixture.expectedTools(),
                actualTools,
                response.getConfidence(),
                numbersPass && toolPass,
                elapsedMs,
                error
        );
    }

    private void printReport(List<FixtureResult> results) {
        long passed = results.stream().filter(FixtureResult::overallPass).count();
        long numbersPassed = results.stream().filter(FixtureResult::numbersPass).count();
        long toolsPassed = results.stream().filter(FixtureResult::toolPass).count();
        double avgConfidence = results.stream()
                .mapToDouble(FixtureResult::confidence)
                .average()
                .orElse(0);
        long totalMs = results.stream().mapToLong(FixtureResult::elapsedMs).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("=".repeat(96)).append("\n");
        sb.append(String.format(" Evaluation Report (%d fixtures, %.1fs total)%n",
                results.size(), totalMs / 1000.0));
        sb.append("=".repeat(96)).append("\n");
        sb.append(String.format(" %-28s | %-7s | %-5s | %-10s | %-7s | %s%n",
                "ID", "Numbers", "Tools", "Confidence", "Elapsed", "Pass"));
        sb.append("-".repeat(96)).append("\n");

        for (FixtureResult r : results) {
            sb.append(String.format(Locale.ROOT, " %-28s | %d/%-5d | %-5s | %-10.2f | %-7s | %s%n",
                    truncate(r.id(), 28),
                    r.numbersHit(), r.numbersTotal(),
                    r.toolPass() ? "OK" : "MISS",
                    r.confidence(),
                    formatElapsed(r.elapsedMs()),
                    r.overallPass() ? "PASS" : "FAIL"));
            if (r.error() != null) {
                sb.append(String.format("   error: %s%n", r.error()));
            }
        }
        sb.append("-".repeat(96)).append("\n");
        sb.append(String.format(Locale.ROOT,
                " Overall: %d/%d passed (%.0f%%) | numbers: %d/%d | tools: %d/%d | avg confidence: %.2f%n",
                passed, results.size(), 100.0 * passed / results.size(),
                numbersPassed, results.size(),
                toolsPassed, results.size(),
                avgConfidence));
        sb.append("=".repeat(96)).append("\n");

        System.out.println(sb);
    }

    private List<EvaluationFixture> loadFixtures() throws Exception {
        try (InputStream in = new ClassPathResource("eval/fixtures.json").getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<EvaluationFixture>>() {});
        }
    }

    private String loadCsv(String csvFile) throws Exception {
        try (InputStream in = new ClassPathResource("eval/csv/" + csvFile).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String formatElapsed(long ms) {
        return ms < 1000 ? ms + "ms" : String.format(Locale.ROOT, "%.1fs", ms / 1000.0);
    }

    record FixtureResult(
            String id,
            boolean numbersPass,
            int numbersHit,
            int numbersTotal,
            boolean toolPass,
            List<String> expectedTools,
            List<String> actualTools,
            double confidence,
            boolean overallPass,
            long elapsedMs,
            String error
    ) {
        static FixtureResult failure(EvaluationFixture f, String error) {
            return new FixtureResult(f.id(), false, 0, f.expectedNumbers().size(),
                    false, f.expectedTools(), List.of(), 0.0, false, 0, error);
        }
    }
}
