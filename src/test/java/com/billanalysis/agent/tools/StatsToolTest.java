package com.billanalysis.agent.tools;

import com.billanalysis.agent.BillSessionContext;
import com.billanalysis.parser.BillRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatsToolTest {

    @Test
    void separatesIncomeAndExpenseCategoryBreakdowns() {
        BillSessionContext context = new BillSessionContext();
        context.setBills(List.of(
                bill("INCOME", "Salary", "1000"),
                bill("EXPENSE", "Food", "20"),
                bill("EXPENSE", "Food", "30")
        ));

        StatsTool.StatsResult result = new StatsTool(context).calculateStats();

        assertThat(result.totalIncome()).isEqualByComparingTo("1000");
        assertThat(result.totalExpense()).isEqualByComparingTo("50");
        assertThat(result.netBalance()).isEqualByComparingTo("950");
        assertThat(result.incomeByCategory()).containsEntry("Salary", new BigDecimal("1000"));
        assertThat(result.expenseByCategory()).containsEntry("Food", new BigDecimal("50"));
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
