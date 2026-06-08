package com.workmate.domain.database;

import com.workmate.domain.common.ValueObject;

import java.util.List;

/**
 * The result of executing a validated SQL SELECT query.
 *
 * <p>Immutable: defensive copies are made for both {@code columns} and {@code rows}
 * on construction, and the accessor records in each row are also copied.
 *
 * <p>Pure Java — no framework dependencies.
 *
 * @param columns ordered list of column names from the result-set metadata
 * @param rows    each inner list contains one cell per column, serialised as {@link String}
 *                via {@code ResultSet.getString}; {@code null} cells are represented as the
 *                literal string {@code "NULL"}
 */
public record QueryResult(List<String> columns, List<List<String>> rows) implements ValueObject {

    public QueryResult {
        columns = List.copyOf(columns);
        rows = rows.stream()
                .map(List::copyOf)
                .collect(java.util.stream.Collectors.toUnmodifiableList());
    }

    /**
     * Number of data rows (excludes the header).
     *
     * @return row count; zero for an empty result
     */
    public int rowCount() {
        return rows.size();
    }

    /**
     * Convenience factory for an empty result with no columns.
     *
     * @return a {@code QueryResult} with zero columns and zero rows
     */
    public static QueryResult empty() {
        return new QueryResult(List.of(), List.of());
    }

    /**
     * Compact tabular representation for debugging. Not intended for production output;
     * callers that need structured JSON should serialise this record directly.
     *
     * @return a pipe-delimited table string, e.g. {@code "id|name\n1|Alice\n2|Bob"}
     */
    public String toCompactString() {
        if (columns.isEmpty()) {
            return "(empty)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.join("|", columns));
        for (List<String> row : rows) {
            sb.append('\n');
            sb.append(String.join("|", row));
        }
        return sb.toString();
    }
}
