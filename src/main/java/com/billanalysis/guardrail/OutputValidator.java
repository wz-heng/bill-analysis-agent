package com.billanalysis.guardrail;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts numeric values from free-form AI output for downstream validation.
 */
@Component
public class OutputValidator {

    // Matches formats such as 1234.56, 1,234.56, ¥1234, ￥1234, and $-99.9.
    private static final Pattern NUMERIC_PATTERN =
            Pattern.compile("[¥￥$]?-?\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?(?!\\d)"
                    + "|[¥￥$]?-?\\d+(?:\\.\\d+)?(?!\\d)");

    public List<BigDecimal> extractNumbers(String aiOutput) {
        List<BigDecimal> numbers = new ArrayList<>();
        if (aiOutput == null || aiOutput.isBlank()) {
            return numbers;
        }

        Matcher matcher = NUMERIC_PATTERN.matcher(aiOutput);
        while (matcher.find()) {
            String raw = matcher.group().replaceAll("[¥￥$,]", "");
            try {
                numbers.add(new BigDecimal(raw));
            } catch (NumberFormatException ignored) {
            }
        }
        return numbers;
    }

    /** Sanity guard: flags outputs containing implausibly large figures. */
    public boolean hasHallucination(String aiOutput) {
        return extractNumbers(aiOutput).stream()
                .anyMatch(n -> n.abs().compareTo(new BigDecimal("1000000000")) > 0);
    }
}
