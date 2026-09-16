package com.github.auciellos.db;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automated test suite verifying Liquibase migrations lifecycle:
 * forward migration (update), schema verification, data integrity,
 * and backward migration (rollback).
 */
public class LiquibaseMigrationTest {

    private static final String CHANGELOG_PATH = "liquibase/master.xml";

    private Connection connection;
    private Liquibase liquibase;

    @BeforeEach
    void setUp() throws Exception {
        // Use an isolated in-memory H2 database per test
        String jdbcUrl = "jdbc:h2:mem:test_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1";
        connection = DriverManager.getConnection(jdbcUrl, "sa", "");

        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));

        liquibase = new Liquibase(CHANGELOG_PATH, new ClassLoaderResourceAccessor(), database);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (liquibase != null) {
            liquibase.close();
        }
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Should successfully apply all changesets and verify schema, relations, and data")
    void testFullMigration() throws Exception {
        // 1. Run migrations
        liquibase.update(new Contexts(), new LabelExpression());

        // 2. Verify table 'users' exists and contains seeded pioneer records
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, username, email, first_name, last_name, status FROM users WHERE id = 1")) {

            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong("id")).isEqualTo(1L);
            assertThat(rs.getString("username")).isEqualTo("alovelace");
            assertThat(rs.getString("email")).isEqualTo("ada.lovelace@example.com");
            assertThat(rs.getString("first_name")).isEqualTo("Ada");
            assertThat(rs.getString("last_name")).isEqualTo("Lovelace");
            assertThat(rs.getString("status")).isEqualTo("ACTIVE");
        }

        // 3. Verify total user count
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM users")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("total")).isEqualTo(3);
        }

        // 4. Verify 'roles' table and many-to-many relationship via 'user_roles'
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT u.username, r.name AS role_name " +
                     "FROM users u " +
                     "JOIN user_roles ur ON u.id = ur.user_id " +
                     "JOIN roles r ON ur.role_id = r.id " +
                     "WHERE u.username = 'alovelace'")) {

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("role_name")).isEqualTo("ROLE_ADMIN");
        }

        // 5. Verify 'audit_logs' table exists and allows insertion
        assertThat(tableExists("audit_logs")).isTrue();
        try (Statement stmt = connection.createStatement()) {
            int inserted = stmt.executeUpdate(
                    "INSERT INTO audit_logs (id, user_id, action, ip_address, details) " +
                    "VALUES (100, 1, 'USER_LOGIN', '192.168.1.1', 'Successful authentication')");
            assertThat(inserted).isEqualTo(1);
        }

        // 6. Verify DATABASECHANGELOG contains all 4 executed changesets
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM DATABASECHANGELOG")) {

            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("total")).isEqualTo(4);
        }
    }

    @Test
    @DisplayName("Should successfully rollback changeset 4 (audit_logs) and re-apply cleanly")
    void testRollback() throws Exception {
        // 1. Apply all changesets
        liquibase.update(new Contexts(), new LabelExpression());
        assertThat(tableExists("audit_logs")).isTrue();
        assertThat(tableExists("users")).isTrue();

        // 2. Rollback the last changeset (changeset 4 which created audit_logs)
        liquibase.rollback(1, (String) null);

        // 3. Verify audit_logs table was dropped, while users and roles remain intact
        assertThat(tableExists("audit_logs")).isFalse();
        assertThat(tableExists("users")).isTrue();
        assertThat(tableExists("roles")).isTrue();

        // 4. Re-apply migration to confirm idempotency
        liquibase.update(new Contexts(), new LabelExpression());
        assertThat(tableExists("audit_logs")).isTrue();
    }

    @Test
    @DisplayName("Should enforce foreign key cascade on user_roles when user is deleted")
    void testCascadeDelete() throws Exception {
        liquibase.update(new Contexts(), new LabelExpression());

        // Verify user 3 has an assigned role
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM user_roles WHERE user_id = 3")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("total")).isEqualTo(1);
        }

        // Delete user 3
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM users WHERE id = 3");
        }

        // Verify foreign key cascade deleted the association in user_roles
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM user_roles WHERE user_id = 3")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("total")).isEqualTo(0);
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                if (tableName.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
