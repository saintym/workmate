package com.workmate.domain.database;

import com.workmate.domain.common.DomainException;
import com.workmate.domain.common.ValueObject;

/**
 * Policy that governs what SQL queries are considered safe for execution.
 *
 * <p>Encapsulates a {@link TableWhitelist} (which tables may be queried) and a
 * {@code maxRowLimit} (the maximum number of rows any single query may return). The
 * {@link SqlValidator} port uses this policy to harden incoming queries before they
 * reach the database.
 *
 * <p>Pure Java — no framework dependencies.
 *
 * @param whitelist   the set of tables the query is allowed to reference
 * @param maxRowLimit maximum rows the query may return; must be &gt;= 1
 */
public record QuerySafetyPolicy(TableWhitelist whitelist, int maxRowLimit) implements ValueObject {

    public QuerySafetyPolicy {
        if (whitelist == null) {
            throw new DomainException("QuerySafetyPolicy whitelist must not be null");
        }
        if (maxRowLimit < 1) {
            throw new DomainException("QuerySafetyPolicy maxRowLimit must be >= 1, got: " + maxRowLimit);
        }
    }

    /**
     * Convenience factory that uses a sensible default row limit of 100.
     *
     * @param whitelist the allowed tables
     * @return a policy with {@code maxRowLimit = 100}
     */
    public static QuerySafetyPolicy defaultPolicy(TableWhitelist whitelist) {
        return new QuerySafetyPolicy(whitelist, 100);
    }
}
