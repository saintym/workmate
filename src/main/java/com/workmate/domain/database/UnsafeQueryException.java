package com.workmate.domain.database;

import com.workmate.domain.common.DomainException;

/**
 * Thrown by {@link SqlValidator} when a query violates the {@link QuerySafetyPolicy}.
 *
 * <p>Examples of violations: non-SELECT statement, reference to a table not in the
 * whitelist, multiple statements in a single string, or an unparseable query.
 *
 * <p>Pure Java — no framework dependencies.
 */
public class UnsafeQueryException extends DomainException {

    public UnsafeQueryException(String message) {
        super(message);
    }
}
