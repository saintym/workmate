package com.workmate.infrastructure.database;

import com.workmate.domain.database.QuerySafetyPolicy;
import com.workmate.domain.database.SqlQuery;
import com.workmate.domain.database.SqlValidator;
import com.workmate.domain.database.UnsafeQueryException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JSqlParser-backed implementation of {@link SqlValidator}.
 *
 * <p>Enforces SELECT-only, per-table whitelist, and injects/caps the LIMIT clause
 * according to the supplied {@link QuerySafetyPolicy}.
 */
@Component
public class JSqlParserSqlValidator implements SqlValidator {

    private static final Logger log = LoggerFactory.getLogger(JSqlParserSqlValidator.class);

    @Override
    public SqlQuery validateAndHarden(SqlQuery query, QuerySafetyPolicy policy) throws UnsafeQueryException {
        String sql = query.sql().trim();

        // Reject multiple statements (semicolon-separated)
        try {
            Statements stmts = CCJSqlParserUtil.parseStatements(sql);
            if (stmts.getStatements().size() > 1) {
                log.warn("Rejected query with multiple statements");
                throw new UnsafeQueryException("Multiple SQL statements are not permitted");
            }
        } catch (JSQLParserException e) {
            // parseStatements failed — fall through to single-statement parse for a better error
        }

        // Parse as a single statement
        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            log.warn("Rejected unparseable query: {}", e.getMessage());
            throw new UnsafeQueryException("SQL could not be parsed: " + e.getMessage());
        }

        // Must be a SELECT
        if (!(statement instanceof Select)) {
            log.warn("Rejected non-SELECT statement type: {}", statement.getClass().getSimpleName());
            throw new UnsafeQueryException(
                    "Only SELECT statements are permitted; got: " + statement.getClass().getSimpleName());
        }

        Select select = (Select) statement;

        // Enumerate all referenced tables and check against whitelist
        TablesNamesFinder finder = new TablesNamesFinder();
        List<String> tables;
        try {
            tables = finder.getTableList((net.sf.jsqlparser.statement.Statement) select);
        } catch (Exception e) {
            log.warn("Failed to extract table names from query: {}", e.getMessage());
            throw new UnsafeQueryException("Could not determine referenced tables: " + e.getMessage());
        }

        for (String table : tables) {
            if (!policy.whitelist().isAllowed(table)) {
                log.warn("Rejected query referencing non-whitelisted table: {}", table);
                throw new UnsafeQueryException(
                        "Table '" + table + "' is not in the allowed table list");
            }
        }

        // Inject or cap LIMIT — only supported for plain SELECT bodies
        String hardenedSql = injectOrCapLimit(select, sql, policy.maxRowLimit());

        return new SqlQuery(hardenedSql);
    }

    /**
     * Rewrites the SELECT to enforce {@code maxRowLimit}.
     * <ul>
     *   <li>If the body is a {@link PlainSelect} with no LIMIT — appends {@code LIMIT maxRowLimit}.</li>
     *   <li>If it already has a LIMIT larger than {@code maxRowLimit} — replaces it.</li>
     *   <li>For complex SELECT bodies (UNION, etc.) without a top-level LIMIT — appends LIMIT.</li>
     * </ul>
     */
    private String injectOrCapLimit(Select select, String originalSql, int maxRowLimit) {
        if (select.getSelectBody() instanceof PlainSelect plainSelect) {
            net.sf.jsqlparser.statement.select.Limit limit = plainSelect.getLimit();
            if (limit == null) {
                // No LIMIT — inject one
                plainSelect.setLimit(buildLimit(maxRowLimit));
            } else {
                // Has LIMIT — cap if it exceeds maxRowLimit
                long existing = extractRowCount(limit);
                if (existing < 0 || existing > maxRowLimit) {
                    plainSelect.setLimit(buildLimit(maxRowLimit));
                }
            }
            return select.toString();
        }

        // For UNION / INTERSECT etc., check if original SQL already has a trailing LIMIT
        String upper = originalSql.toUpperCase();
        int limitIdx = upper.lastIndexOf("LIMIT");
        if (limitIdx >= 0) {
            // Parse the number after LIMIT
            String afterLimit = originalSql.substring(limitIdx + 5).trim();
            try {
                long existing = Long.parseLong(afterLimit.split("[^0-9]")[0]);
                if (existing > maxRowLimit) {
                    // Replace the value
                    return originalSql.substring(0, limitIdx) + "LIMIT " + maxRowLimit;
                }
                return originalSql;
            } catch (NumberFormatException ignored) {
                // Cannot parse — append our own limit
            }
        }
        // Append LIMIT
        return originalSql + " LIMIT " + maxRowLimit;
    }

    private net.sf.jsqlparser.statement.select.Limit buildLimit(int rows) {
        net.sf.jsqlparser.statement.select.Limit limit = new net.sf.jsqlparser.statement.select.Limit();
        limit.setRowCount(new net.sf.jsqlparser.expression.LongValue(rows));
        return limit;
    }

    private long extractRowCount(net.sf.jsqlparser.statement.select.Limit limit) {
        if (limit.getRowCount() instanceof net.sf.jsqlparser.expression.LongValue lv) {
            return lv.getValue();
        }
        return -1; // unknown / expression — treat as needing a cap
    }
}
