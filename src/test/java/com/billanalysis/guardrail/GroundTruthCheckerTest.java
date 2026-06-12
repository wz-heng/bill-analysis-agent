package com.billanalysis.guardrail;

import com.billanalysis.agent.BillSessionContext;
import com.billanalysis.agent.tools.AnomalyTool;
import com.billanalysis.agent.tools.StatsTool;
import com.billanalysis.agent.tools.TrendTool;
import com.billanalysis.parser.BillRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroundTruthCheckerTest {

    private GroundTruthChecker checker;

    @BeforeEach
    void setUp() {
        BillSessionContext context = new BillSessionContext();
        context.setBills(List.of(
                bill("2024-01-10", "INCOME", "Salary", "5000"),
                bill("2024-01-15", "EXPENSE", "Food", "200"),
                bill("2024-02-10", "INCOME", "Salary", "5000"),
                bill("2024-02-20", "EXPENSE", "Food", "300")
        ));
        checker = new GroundTruthChecker(
                new OutputValidator(),
                new StatsTool(context),
                new TrendTool(context),
                new AnomalyTool(context));
    }

    @Test
    void exactMatchesAcrossTotalsCategoriesAndTrend() {
        // 10000 = totalIncome, 4700 = Feb net, 500 = Food expense category
        GroundTruthChecker.ValidationResult result =
                checker.validate("总收入 10,000 元，2 月份净收入 4,700 元，餐饮支出 500 元");

        assertThat(result.valid()).isTrue();
        assertThat(result.confidence()).isEqualTo(1.0);
        assertThat(result.suspicious()).isEmpty();
    }

    @Test
    void closeButWrongFigureIsFlaggedSuspicious() {
        // truth has 10000; 9800 is within 5% but not an exact match.
        GroundTruthChecker.ValidationResult result =
                checker.validate("估算总收入约 9,800 元");

        assertThat(result.valid()).isFalse();
        assertThat(result.suspicious()).containsExactly(new BigDecimal("9800"));
        assertThat(result.confidence()).isEqualTo(0.0);
    }

    @Test
    void lowMagnitudeNumbersAreSkipped() {
        // 5.2 (z-score-like) and 1 (month index) are below the amount threshold.
        GroundTruthChecker.ValidationResult result =
                checker.validate("z-score 是 5.2，月份是 1");

        assertThat(result.valid()).isTrue();
        assertThat(result.confidence()).isEqualTo(1.0);
        assertThat(result.message()).contains("No numeric figures");
    }

    @Test
    void emptyAnswerIsInvalid() {
        GroundTruthChecker.ValidationResult result = checker.validate("");

        assertThat(result.valid()).isFalse();
        assertThat(result.confidence()).isEqualTo(0.0);
    }

    @Test
    void implausiblyLargeFiguresAreRejected() {
        GroundTruthChecker.ValidationResult result =
                checker.validate("总收入 1500000000 元");

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("implausibly large");
    }

    @Test
    void partialMatchYieldsFractionalConfidence() {
        // 10000 matches totalIncome; 12345 has no close truth → not suspicious, just unmatched.
        GroundTruthChecker.ValidationResult result =
                checker.validate("总收入 10,000 元，附注金额 12,345 元");

        assertThat(result.valid()).isTrue();
        assertThat(result.confidence()).isEqualTo(0.5);
    }

    private BillRecord bill(String date, String type, String category, String amount) {
        return BillRecord.builder()
                .date(LocalDate.parse(date))
                .type(type)
                .amount(new BigDecimal(amount))
                .category(category)
                .description(category)
                .build();
    }
}
