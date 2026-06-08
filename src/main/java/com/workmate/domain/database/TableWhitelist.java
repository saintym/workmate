package com.workmate.domain.database;

import com.workmate.domain.common.ValueObject;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable set of table names that are permitted in SQL queries.
 *
 * <p>Table names are normalised to lower-case on construction so comparisons are
 * case-insensitive.
 *
 * <p>Pure Java — no framework dependencies.
 *
 * @param allowedTables lower-cased set of permitted table names
 */
public record TableWhitelist(Set<String> allowedTables) implements ValueObject {

    public TableWhitelist {
        allowedTables = allowedTables.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns {@code true} if {@code table} (case-insensitive) is in the allowed set.
     *
     * @param table the table name to check
     * @return {@code true} when the table is permitted
     */
    public boolean isAllowed(String table) {
        if (table == null) {
            return false;
        }
        return allowedTables.contains(table.toLowerCase());
    }

    /**
     * Factory method — normalises each name to lower-case.
     *
     * @param tables one or more table names
     * @return a {@code TableWhitelist} containing all provided names
     */
    public static TableWhitelist of(String... tables) {
        return new TableWhitelist(
                Arrays.stream(tables).collect(Collectors.toSet())
        );
    }
}
