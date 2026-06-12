package com.billanalysis.guardrail;

import com.billanalysis.agent.tools.AnomalyTool;
import com.billanalysis.agent.tools.StatsTool;
import com.billanalysis.agent.tools.TrendTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates an AI answer by collecting the deterministic amounts from every
 * analysis tool, then checking each non-trivial figure in the answer against
 * that ground-truth set. Replaces the previous heuristic that compared free-form
 * numbers against only totalIncome/totalExpense.
 */
@Component
@RequiredArgsConstructor
public class GroundTruthChecker {

    private static final BigDecimal EXACT_TOLERANCE = new BigDecimal("0.01");
    /** Figures whose absolute value is below this are treated as non-amounts (months, z-scores, counts). */
    private static final BigDecimal AMOUNT_THRESHOLD = new BigDecimal("10");
    /** Relative band used to flag close-but-wrong figures (e.g. 0.05 = within 5%). */
    private static final BigDecimal RELATIVE_BAND = new BigDecimal("0.05");

    private final OutputValidator outputValidator;
    private final StatsTool statsTool;
    private final TrendTool trendTool;
    private final AnomalyTool anomalyTool;

    public ValidationResult validate(String aiOutput) {
        if (aiOutput == null || aiOutput.isBlank()) {
            return new ValidationResult(false, 0.0, List.of(), "AI output is empty");
        }
        if (outputValidator.hasHallucination(aiOutput)) {
            return new ValidationResult(false, 0.0, List.of(),
                    "AI output contains implausibly large figures");
        }

        Set<BigDecimal> truth = collectTruthAmounts();
        List<BigDecimal> aiNumbers = outputValidator.extractNumbers(aiOutput);

        int totalChecked = 0;
        int matched = 0;
        List<BigDecimal> suspicious = new ArrayList<>();

        for (BigDecimal n : aiNumbers) {
            BigDecimal abs = n.abs();
            if (abs.compareTo(AMOUNT_THRESHOLD) < 0) {
                continue;
            }
            totalChecked++;
            if (matchesExactly(abs, truth)) {
                matched++;
            } else if (closeToAny(abs, truth)) {
                suspicious.add(n);
            }
        }

        boolean valid = suspicious.isEmpty();
        double confidence = totalChecked == 0 ? 1.0 : (double) matched / totalChecked;
        return new ValidationResult(valid, confidence, List.copyOf(suspicious),
                buildMessage(matched, totalChecked, suspicious));
    }

    private Set<BigDecimal> collectTruthAmounts() {
        Set<BigDecimal> set = new HashSet<>();

        StatsTool.StatsResult stats = statsTool.calculateStats();
        addAmount(set, stats.totalIncome());
        addAmount(set, stats.totalExpense());
        addAmount(set, stats.netBalance());
        addAll(set, stats.incomeByCategory());
        addAll(set, stats.expenseByCategory());

        Map<String, TrendTool.MonthlyTrend> trend = trendTool.analyzeMonthlyTrend();
        for (TrendTool.MonthlyTrend m : trend.values()) {
            addAmount(set, m.income());
            addAmount(set, m.expense());
            addAmount(set, m.net());
        }

        for (AnomalyTool.AnomalyRecord a : anomalyTool.detectAnomalies()) {
            addAmount(set, a.amount());
        }
        return set;
    }

    private void addAll(Set<BigDecimal> set, Map<String, BigDecimal> values) {
        if (values == null) return;
        values.values().forEach(v -> addAmount(set, v));
    }

    private void addAmount(Set<BigDecimal> set, BigDecimal amount) {
        if (amount == null) return;
        set.add(amount.abs().setScale(2, RoundingMode.HALF_UP));
    }

    private boolean matchesExactly(BigDecimal n, Set<BigDecimal> truth) {
        BigDecimal scaled = n.setScale(2, RoundingMode.HALF_UP);
        if (truth.contains(scaled)) return true;
        return truth.stream().anyMatch(t -> scaled.subtract(t).abs().compareTo(EXACT_TOLERANCE) <= 0);
    }

    private boolean closeToAny(BigDecimal n, Set<BigDecimal> truth) {
        return truth.stream().anyMatch(t -> {
            if (t.signum() == 0) return false;
            BigDecimal relative = n.subtract(t).abs().divide(t.abs(), 4, RoundingMode.HALF_UP);
            return relative.compareTo(RELATIVE_BAND) <= 0;
        });
    }

    private String buildMessage(int matched, int totalChecked, List<BigDecimal> suspicious) {
        if (totalChecked == 0) {
            return "No numeric figures to validate";
        }
        if (suspicious.isEmpty()) {
            return String.format("Matched %d/%d figures against ground truth", matched, totalChecked);
        }
        return String.format("Suspicious figures %s (matched %d/%d)", suspicious, matched, totalChecked);
    }

    public record ValidationResult(
            boolean valid,
            double confidence,
            List<BigDecimal> suspicious,
            String message
    ) {}
}
