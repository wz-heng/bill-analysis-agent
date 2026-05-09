package com.billanalysis.parser;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvBillParserTest {

    private final CsvBillParser parser = new CsvBillParser();

    @Test
    void parsesValidCsv() throws Exception {
        String csv = """
                date,type,amount,category,description
                2024-01-01,income,1000.00,Salary,January salary
                2024-01-02,EXPENSE,12.50,Food,Lunch
                """;

        List<BillRecord> records = parser.parse(new StringReader(csv));

        assertThat(records).hasSize(2);
        assertThat(records.get(0).getType()).isEqualTo("INCOME");
        assertThat(records.get(1).getCategory()).isEqualTo("Food");
    }

    @Test
    void rejectsInvalidHeader() {
        String csv = """
                when,type,amount,category,description
                2024-01-01,INCOME,1000.00,Salary,January salary
                """;

        assertThatThrownBy(() -> parser.parse(new StringReader(csv)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CSV header");
    }

    @Test
    void rejectsInvalidType() {
        String csv = """
                date,type,amount,category,description
                2024-01-01,TRANSFER,1000.00,Salary,January salary
                """;

        assertThatThrownBy(() -> parser.parse(new StringReader(csv)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INCOME or EXPENSE");
    }
}
