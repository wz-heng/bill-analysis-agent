package com.billanalysis.guardrail;

import com.billanalysis.agent.tools.StatsTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Compares key figures mentioned in the AI's text response against
 * independently computed ground-truth values to catch hallucinated numbers.
 */
@Component
@RequiredArgsConstructor
public class GroundTruthChecker {

    private static final BigDecimal EXACT_TOLERANCE = new BigDecimal("0.01");
    /** Figures within this range of a ground-truth value are considered close but wrong. */
    private static final BigDecimal SUSPICION_BAND = new BigDecimal("500");

    private final OutputValidator outputValidator;

    public ValidationResult validate(String aiOutput, StatsTool.StatsResult groundTruth) {
        if (aiOutput == null || aiOutput.isBlank()) {
            return new ValidationResult(false, "AI output is empty");
        }

        List<BigDecimal> aiNumbers = outputValidator.extractNumbers(aiOutput);

        if (outputValidator.hasHallucination(aiOutput)) {
            return new ValidationResult(false, "AI output contains implausibly large figures");
        }

        boolean suspiciousIncome = hasCloseButWrong(aiNumbers, groundTruth.totalIncome());
        boolean suspiciousExpense = hasCloseButWrong(aiNumbers, groundTruth.totalExpense());

        if (suspiciousIncome || suspiciousExpense) {
            return new ValidationResult(false,
                    String.format("AI output contains figures inconsistent with ground truth " +
                                    "(expected income=%s, expense=%s)",
                            groundTruth.totalIncome(), groundTruth.totalExpense()));
        }
        return new ValidationResult(true, "Ground truth check passed");
    }

    private boolean hasCloseButWrong(List<BigDecimal> numbers, BigDecimal target) {
        return numbers.stream().anyMatch(n -> {
            BigDecimal diff = n.subtract(target).abs();
            return diff.compareTo(EXACT_TOLERANCE) > 0
                    && diff.compareTo(SUSPICION_BAND) <= 0;
        });
    }

    public record ValidationResult(boolean valid, String message) {}
}
