package com.billanalysis.parser;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Parses CSV bills with header: date,type,amount,category,description
 */
@Component
public class CsvBillParser {

    private static final Set<String> VALID_TYPES = Set.of("INCOME", "EXPENSE");
    private static final List<String> EXPECTED_HEADER =
            List.of("date", "type", "amount", "category", "description");

    public List<BillRecord> parse(Reader reader) throws IOException, CsvException {
        List<BillRecord> records = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(reader)) {
            List<String[]> rows = csvReader.readAll();
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("CSV content is empty");
            }
            validateHeader(rows.get(0));

            for (int i = 1; i < rows.size(); i++) {   // skip header
                String[] row = rows.get(i);
                if (isBlankRow(row)) {
                    continue;
                }
                records.add(parseRecord(row, i + 1));
            }
            if (records.isEmpty()) {
                throw new IllegalArgumentException("CSV content contains no bill records");
            }
        }
        return records;
    }

    private void validateHeader(String[] header) {
        List<String> actual = Arrays.stream(header)
                .map(value -> value.trim().toLowerCase())
                .toList();
        if (actual.size() < EXPECTED_HEADER.size()
                || !actual.subList(0, EXPECTED_HEADER.size()).equals(EXPECTED_HEADER)) {
            throw new IllegalArgumentException(
                    "CSV header must be: date,type,amount,category,description");
        }
    }

    private boolean isBlankRow(String[] row) {
        return Arrays.stream(row).allMatch(value -> value == null || value.isBlank());
    }

    private BillRecord parseRecord(String[] row, int rowNumber) {
        if (row.length < EXPECTED_HEADER.size()) {
            throw new IllegalArgumentException("CSV row " + rowNumber + " has fewer than 5 columns");
        }

        try {
            LocalDate date = LocalDate.parse(required(row[0], rowNumber, "date"));
            String type = required(row[1], rowNumber, "type").toUpperCase();
            if (!VALID_TYPES.contains(type)) {
                throw new IllegalArgumentException("CSV row " + rowNumber + " type must be INCOME or EXPENSE");
            }

            BigDecimal amount = new BigDecimal(required(row[2], rowNumber, "amount"));
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("CSV row " + rowNumber + " amount must be non-negative");
            }

            return BillRecord.builder()
                    .date(date)
                    .type(type)
                    .amount(amount)
                    .category(required(row[3], rowNumber, "category"))
                    .description(required(row[4], rowNumber, "description"))
                    .build();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("CSV row " + rowNumber + " date must use ISO format yyyy-MM-dd", e);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("CSV row " + rowNumber + " amount is not a valid number", e);
        }
    }

    private String required(String value, int rowNumber, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CSV row " + rowNumber + " " + field + " is required");
        }
        return value.trim();
    }
}
