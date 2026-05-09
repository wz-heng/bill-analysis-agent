package com.billanalysis.agent.tools;

import com.billanalysis.agent.BillSessionContext;
import com.billanalysis.parser.BillRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StatsTool {

    private final BillSessionContext context;

    @Tool(description = "Calculate total income, total expense, net balance, and per-category amount breakdown from the current bill data.")
    public StatsResult calculateStats() {
        List<BillRecord> bills = safeBills();

        BigDecimal totalIncome = bills.stream()
                .filter(b -> "INCOME".equals(b.getType()))
                .map(BillRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = bills.stream()
                .filter(b -> "EXPENSE".equals(b.getType()))
                .map(BillRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> incomeByCategory = sumByCategory(bills, "INCOME");
        Map<String, BigDecimal> expenseByCategory = sumByCategory(bills, "EXPENSE");

        return new StatsResult(
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense),
                incomeByCategory,
                expenseByCategory
        );
    }

    private List<BillRecord> safeBills() {
        return context.getBills() == null ? List.of() : context.getBills();
    }

    private Map<String, BigDecimal> sumByCategory(List<BillRecord> bills, String type) {
        return bills.stream()
                .filter(b -> type.equals(b.getType()))
                .collect(Collectors.groupingBy(
                        BillRecord::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, BillRecord::getAmount, BigDecimal::add)
                ));
    }

    public record StatsResult(
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal netBalance,
            Map<String, BigDecimal> incomeByCategory,
            Map<String, BigDecimal> expenseByCategory
    ) {}
}
