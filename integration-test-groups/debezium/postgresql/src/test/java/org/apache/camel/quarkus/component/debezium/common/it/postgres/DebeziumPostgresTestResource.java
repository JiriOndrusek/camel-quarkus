package org.apache.camel.quarkus.component.debezium.common.it.postgres;

import org.apache.camel.quarkus.component.debezium.common.it.DebeziumPostgresResource;
import org.apache.camel.quarkus.test.support.debezium.AbstractDebeziumTestResource;
import org.apache.camel.quarkus.test.support.debezium.Type;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class DebeziumPostgresTestResource extends AbstractDebeziumTestResource<PostgreSQLContainer<?>> {

    public static final String DB_USERNAME = "postgres";
    public static final String DB_PASSWORD = "changeit";
    private static final String POSTGRES_IMAGE = ConfigProvider.getConfig().getValue("postgres-debezium.container.image",
            String.class);
    private static final int DB_PORT = 5432;

    public DebeziumPostgresTestResource() {
        super(Type.postgres);
    }

    @Override
    protected PostgreSQLContainer<?> createContainer() {
        DockerImageName imageName = DockerImageName.parse(POSTGRES_IMAGE)
                .asCompatibleSubstituteFor("postgres");
        return new PostgreSQLContainer<>(imageName)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASSWORD)
                .withDatabaseName(DebeziumPostgresResource.DB_NAME)
                .withInitScript("initPostgres.sql");
    }

    @Override
    protected String getJdbcUrl() {
        return "jdbc:postgresql://" + container.getHost() + ":"
                + container.getMappedPort(DB_PORT) + "/" + DebeziumPostgresResource.DB_NAME + "?user="
                + DB_USERNAME + "&password=" + DB_PASSWORD;
    }

    @Override
    protected String getUsername() {
        return DB_USERNAME;
    }

    @Override
    protected String getPassword() {
        return DB_PASSWORD;
    }

    @Override
    protected int getPort() {
        return DB_PORT;
    }
}
