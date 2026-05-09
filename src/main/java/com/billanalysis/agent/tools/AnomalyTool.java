package com.billanalysis.agent.tools;

import com.billanalysis.agent.BillSessionContext;
import com.billanalysis.parser.BillRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AnomalyTool {

    /** Transactions whose Z-score exceeds this threshold are flagged. */
    private static final double Z_SCORE_THRESHOLD = 2.0;
    private static final double MEDIAN_MULTIPLIER_THRESHOLD = 3.0;
    private static final BigDecimal MIN_MEDIAN_DELTA = new BigDecimal("500");

    private final BillSessionContext context;

    @Tool(description = "Detect anomalous large transactions using Z-score analysis over all bill amounts. Returns transactions that deviate significantly from the mean.")
    public List<AnomalyRecord> detectAnomalies() {
        List<BillRecord> expenses = (context.getBills() == null ? List.<BillRecord>of() : context.getBills()).stream()
                .filter(b -> "EXPENSE".equals(b.getType()))
                .toList();

        if (expenses.size() < 3) {
            return List.of();
        }

        double mean = expenses.stream()
                .mapToDouble(b -> b.getAmount().doubleValue())
                .average()
                .orElse(0);

        double stdDev = Math.sqrt(expenses.stream()
                .mapToDouble(b -> Math.pow(b.getAmount().doubleValue() - mean, 2))
                .average()
                .orElse(0));

        if (stdDev == 0) return List.of();

        BigDecimal median = medianAmount(expenses);

        return expenses.stream()
                .filter(b -> {
                    double z = (b.getAmount().doubleValue() - mean) / stdDev;
                    return Math.abs(z) > Z_SCORE_THRESHOLD || isLargeComparedToMedian(b.getAmount(), median);
                })
                .map(b -> {
                    double z = (b.getAmount().doubleValue() - mean) / stdDev;
                    return new AnomalyRecord(
                            b.getDate().toString(),
                            b.getType(),
                            b.getAmount(),
                            b.getCategory(),
                            b.getDescription(),
                            Math.round(z * 100.0) / 100.0
                    );
                })
                .collect(Collectors.toList());
    }

    private BigDecimal medianAmount(List<BillRecord> bills) {
        List<BigDecimal> sortedAmounts = bills.stream()
                .map(BillRecord::getAmount)
                .sorted()
                .toList();

        int middle = sortedAmounts.size() / 2;
        if (sortedAmounts.size() % 2 == 1) {
            return sortedAmounts.get(middle);
        }
        return sortedAmounts.get(middle - 1)
                .add(sortedAmounts.get(middle))
                .divide(new BigDecimal("2"));
    }

    private boolean isLargeComparedToMedian(BigDecimal amount, BigDecimal median) {
        if (median.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return amount.compareTo(median.multiply(new BigDecimal(String.valueOf(MEDIAN_MULTIPLIER_THRESHOLD)))) > 0
                && amount.subtract(median).compareTo(MIN_MEDIAN_DELTA) > 0;
    }

    public record AnomalyRecord(
            String date,
            String type,
            BigDecimal amount,
            String category,
            String description,
            double zScore
    ) {}
}
