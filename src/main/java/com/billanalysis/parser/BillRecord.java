package com.billanalysis.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillRecord {
    private LocalDate date;
    /** INCOME or EXPENSE */
    private String type;
    private BigDecimal amount;
    private String category;
    private String description;
}
