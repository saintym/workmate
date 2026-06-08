package com.workmate.domain.database;

/**
 * Port: executes a pre-validated, read-only {@link SqlQuery} and returns its result.
 *
 * <p>Callers <strong>must</strong> pass only queries that have already been validated and
 * hardened by a {@link SqlValidator}. This port does not re-validate; it trusts that the
 * query is a safe {@code SELECT}.
 *
 * <p>Implementations (in the infrastructure layer) should:
 * <ul>
 *   <li>Apply a reasonable statement timeout (e.g. 5 seconds) to prevent runaway queries.</li>
 *   <li>Map every cell to a {@link String} via the JDBC {@code getString} accessor, treating
 *       SQL {@code NULL} as the literal string {@code "NULL"}.</li>
 *   <li>Never mutate the database — a {@code SELECT} statement is the only accepted input.</li>
 * </ul>
 *
 * <p>Pure Java — no framework dependencies on this interface.
 */
public interface QueryExecutor {

    /**
     * Executes {@code safeQuery} and returns all rows up to the embedded {@code LIMIT}.
     *
     * @param safeQuery a validated, LIMIT-hardened query; must not be {@code null}
     * @return the query result; never {@code null}
     */
    QueryResult execute(SqlQuery safeQuery);
}
