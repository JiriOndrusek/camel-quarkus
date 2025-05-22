package org.apache.camel.quarkus.component.debezium.common.it.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumTest;
import org.apache.camel.quarkus.test.support.debezium.Type;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(value = DebeziumPostgresTestResource.class, restrictToAnnotatedClass = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DebeziumPostgresTest extends AbstractDebeziumTest {
    private static final Logger LOG = Logger.getLogger(DebeziumPostgresTest.class);

    private static Connection connection;

    public DebeziumPostgresTest() {
        super(Type.postgres);
    }

    @BeforeAll
    public static void setUp() throws SQLException {
        Config config = ConfigProvider.getConfig();
        final String jdbcUrl = config.getValue(Type.postgres.getPropertyJdbc(), String.class);
        connection = DriverManager.getConnection(jdbcUrl);
    }

    @Test
    @Order(4)
    public void testAdditionalProperty() {
        //https://github.com/apache/camel-quarkus/issues/3488
        RestAssured.get(Type.postgres.getComponent() + "/getAdditionalProperties")
                .then()
                .statusCode(200)
                .body("'database.connectionTimeZone'", is("CET"));
    }

    @AfterAll
    public static void cleanUp() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    protected Connection getConnection() {
        return connection;
    }

}
