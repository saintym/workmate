package com.workmate.domain.database;

/**
 * Port: validates and hardens a raw {@link SqlQuery} according to a {@link QuerySafetyPolicy}.
 *
 * <p>Implementations (in the infrastructure layer) <strong>must</strong>:
 * <ol>
 *   <li>Parse the SQL and reject anything that is not a single {@code SELECT} statement —
 *       {@code DROP}, {@code DELETE}, {@code UPDATE}, {@code INSERT}, DDL, multiple
 *       statements separated by {@code ;}, and comment-injection tricks are all rejected.</li>
 *   <li>Enumerate every table referenced in the query (including sub-queries and joins) and
 *       reject any that are not present in {@code policy.whitelist()}.</li>
 *   <li>If the {@code SELECT} has no {@code LIMIT} clause, inject
 *       {@code LIMIT policy.maxRowLimit()}.</li>
 *   <li>If the {@code SELECT} already has a {@code LIMIT} that exceeds
 *       {@code policy.maxRowLimit()}, replace it with {@code policy.maxRowLimit()}.</li>
 *   <li>Return a new {@link SqlQuery} whose {@code sql()} is the (possibly rewritten)
 *       safe query string.</li>
 * </ol>
 *
 * <p>Pure Java — no framework dependencies on this interface.
 */
public interface SqlValidator {

    /**
     * Validates {@code query} against {@code policy} and returns a safe, possibly rewritten
     * query with an enforced {@code LIMIT}.
     *
     * @param query  the raw query to validate; must not be {@code null}
     * @param policy the safety policy to enforce; must not be {@code null}
     * @return a validated, LIMIT-hardened {@link SqlQuery} ready for execution
     * @throws UnsafeQueryException if the query violates any rule in {@code policy}
     */
    SqlQuery validateAndHarden(SqlQuery query, QuerySafetyPolicy policy) throws UnsafeQueryException;
}
