package com.workmate.domain.database;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

/**
 * A validated SQL query string produced by the Text-to-SQL pipeline.
 *
 * <p><b>Phase 2 placeholder</b> — full Text-to-SQL validation (AST parsing, read-only
 * enforcement, parameter binding, dialect checks) is deferred to Phase 2. For now only
 * a non-blank invariant is enforced.
 *
 * <p>Pure Java — no framework dependencies.
 *
 * @param sql the raw SQL string; must not be blank
 */
public record SqlQuery(String sql) implements ValueObject {

    public SqlQuery {
        if (sql == null || sql.isBlank()) {
            throw new DomainException("SqlQuery sql must not be blank");
        }
    }

    /**
     * Convenience factory.
     *
     * @param sql the raw SQL string; must not be blank
     * @return a validated {@code SqlQuery}
     */
    public static SqlQuery of(String sql) {
        return new SqlQuery(sql);
    }
}
