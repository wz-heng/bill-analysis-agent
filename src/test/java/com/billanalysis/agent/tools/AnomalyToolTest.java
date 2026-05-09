package com.billanalysis.agent.tools;

import com.billanalysis.agent.BillSessionContext;
import com.billanalysis.parser.BillRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyToolTest {

    @Test
    void detectsExpenseAnomaliesWithoutIncomeBias() {
        BillSessionContext context = new BillSessionContext();
        context.setBills(List.of(
                bill("INCOME", "Salary", "15000"),
                bill("EXPENSE", "Food", "50"),
                bill("EXPENSE", "Food", "55"),
                bill("EXPENSE", "Food", "60"),
                bill("EXPENSE", "Shopping", "1000")
        ));

        List<AnomalyTool.AnomalyRecord> anomalies = new AnomalyTool(context).detectAnomalies();

        assertThat(anomalies)
                .extracting(AnomalyTool.AnomalyRecord::category)
                .containsExactly("Shopping");
    }

    private BillRecord bill(String type, String category, String amount) {
        return BillRecord.builder()
                .date(LocalDate.parse("2024-01-01"))
                .type(type)
                .amount(new BigDecimal(amount))
                .category(category)
                .description(category)
                .build();
    }
}
