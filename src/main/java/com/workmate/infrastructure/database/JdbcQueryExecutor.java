package com.workmate.infrastructure.database;

import com.workmate.domain.database.QueryExecutor;
import com.workmate.domain.database.QueryResult;
import com.workmate.domain.database.SqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-backed implementation of {@link QueryExecutor}.
 *
 * <p>Executes the pre-validated SELECT using {@link JdbcTemplate} with a 5-second
 * statement timeout. All cell values are read as {@link String} via
 * {@code ResultSet.getString}; SQL {@code NULL} is mapped to the literal {@code "NULL"}.
 */
@Component
public class JdbcQueryExecutor implements QueryExecutor {

    private static final Logger log = LoggerFactory.getLogger(JdbcQueryExecutor.class);
    private static final int QUERY_TIMEOUT_SECONDS = 5;

    private final JdbcTemplate jdbcTemplate;

    public JdbcQueryExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcTemplate.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
    }

    @Override
    public QueryResult execute(SqlQuery safeQuery) {
        log.debug("Executing safe query: {}", safeQuery.sql());

        return jdbcTemplate.query(safeQuery.sql(), rs -> {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            List<String> columns = new ArrayList<>(colCount);
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }

            List<List<String>> rows = new ArrayList<>();
            while (rs.next()) {
                List<String> row = new ArrayList<>(colCount);
                for (int i = 1; i <= colCount; i++) {
                    String value = rs.getString(i);
                    row.add(value != null ? value : "NULL");
                }
                rows.add(row);
            }

            log.debug("Query returned {} row(s) with {} column(s)", rows.size(), colCount);
            return new QueryResult(columns, rows);
        });
    }
}
