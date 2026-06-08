package com.workmate.infrastructure.database;

import com.workmate.domain.database.QuerySafetyPolicy;
import com.workmate.domain.database.SqlQuery;
import com.workmate.domain.database.TableWhitelist;
import com.workmate.domain.database.UnsafeQueryException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Security tests for the Text-to-SQL safety layer — the engineering heart of the
 * data-query feature. Verifies SELECT-only enforcement, table whitelisting, LIMIT
 * injection/capping, and rejection of multi-statement / mutation queries.
 *
 * <p>Pure JSqlParser; no Spring context needed.
 */
class JSqlParserSqlValidatorTest {

    private final JSqlParserSqlValidator validator = new JSqlParserSqlValidator();
    private final QuerySafetyPolicy policy = QuerySafetyPolicy.defaultPolicy(
            TableWhitelist.of("products", "orders", "refunds", "customers"));

    private String harden(String sql) {
        return validator.validateAndHarden(new SqlQuery(sql), policy).sql();
    }

    @Test
    void injects_limit_when_select_has_none() {
        String hardened = harden("SELECT name FROM products");
        assertThat(hardened.toUpperCase()).contains("LIMIT 100");
    }

    @Test
    void caps_limit_that_exceeds_policy_max() {
        String hardened = harden("SELECT name FROM products LIMIT 9999");
        assertThat(hardened).contains("100");
        assertThat(hardened).doesNotContain("9999");
    }

    @Test
    void keeps_existing_small_limit() {
        String hardened = harden("SELECT name FROM products LIMIT 5");
        assertThat(hardened).contains("5");
        assertThat(hardened.toUpperCase()).doesNotContain("LIMIT 100");
    }

    @Test
    void allows_join_across_whitelisted_tables() {
        String hardened = harden(
                "SELECT p.name, count(*) FROM refunds r "
                        + "JOIN orders o ON r.order_id = o.id "
                        + "JOIN products p ON o.product_id = p.id GROUP BY p.name");
        assertThat(hardened.toUpperCase()).contains("LIMIT 100");
    }

    @Test
    void rejects_delete() {
        assertThatThrownBy(() -> harden("DELETE FROM products"))
                .isInstanceOf(UnsafeQueryException.class);
    }

    @Test
    void rejects_drop() {
        assertThatThrownBy(() -> harden("DROP TABLE products"))
                .isInstanceOf(UnsafeQueryException.class);
    }

    @Test
    void rejects_update() {
        assertThatThrownBy(() -> harden("UPDATE products SET price_cents = 0"))
                .isInstanceOf(UnsafeQueryException.class);
    }

    @Test
    void rejects_insert() {
        assertThatThrownBy(() -> harden("INSERT INTO products(name) VALUES ('x')"))
                .isInstanceOf(UnsafeQueryException.class);
    }

    @Test
    void rejects_non_whitelisted_table() {
        // 'users' holds auth data and is NOT in the business whitelist.
        assertThatThrownBy(() -> harden("SELECT * FROM users"))
                .isInstanceOf(UnsafeQueryException.class)
                .hasMessageContaining("users");
    }

    @Test
    void rejects_multiple_statements() {
        assertThatThrownBy(() -> harden("SELECT name FROM products; DROP TABLE products"))
                .isInstanceOf(UnsafeQueryException.class);
    }

    @Test
    void rejects_unparseable_sql() {
        assertThatThrownBy(() -> harden("this is not sql"))
                .isInstanceOf(UnsafeQueryException.class);
    }
}
