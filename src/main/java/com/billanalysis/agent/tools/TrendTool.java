package com.billanalysis.agent.tools;

import com.billanalysis.agent.BillSessionContext;
import com.billanalysis.parser.BillRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TrendTool {

    private final BillSessionContext context;

    @Tool(description = "Analyze month-by-month income and expense trends. Returns each month's income, expense, and net balance sorted chronologically.")
    public Map<String, MonthlyTrend> analyzeMonthlyTrend() {
        context.recordTool("trend");
        List<BillRecord> bills = context.getBills() == null ? List.of() : context.getBills();

        Map<YearMonth, List<BillRecord>> byMonth = bills.stream()
                .collect(Collectors.groupingBy(b -> YearMonth.from(b.getDate())));

        Map<String, MonthlyTrend> result = new TreeMap<>();
        byMonth.forEach((month, records) -> {
            BigDecimal income = records.stream()
                    .filter(r -> "INCOME".equals(r.getType()))
                    .map(BillRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal expense = records.stream()
                    .filter(r -> "EXPENSE".equals(r.getType()))
                    .map(BillRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.put(month.toString(), new MonthlyTrend(income, expense, income.subtract(expense)));
        });
        return result;
    }

    public record MonthlyTrend(BigDecimal income, BigDecimal expense, BigDecimal net) {}
}
