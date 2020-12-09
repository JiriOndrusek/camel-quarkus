package org.apache.camel.quarkus.component.minio.it;

import org.apache.camel.quarkus.testcontainers.ContainerResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.time.Duration;
import java.util.Map;

public class MinioTestResource implements ContainerResourceLifecycleManager {

    private final String CONTAINER_IMAGE = "minio/minio:latest";
    private final int BROKER_PORT = 9000;

    private GenericContainer minioServer = new GenericContainer(CONTAINER_IMAGE)/*.withNetworkAliases(CONTAINER_NAME)*/
                .withEnv("MINIO_ACCESS_KEY", MinioResource.SERVER_ACCESS_KEY)
                .withEnv("MINIO_SECRET_KEY", MinioResource.SERVER_SECRET_KEY)
                .withCommand("server /data")
                .withExposedPorts(BROKER_PORT)
                .waitingFor(new HttpWaitStrategy()
                        .forPath("/minio/health/ready")
                        .forPort(BROKER_PORT)
                        .withStartupTimeout(Duration.ofSeconds(10)));;

    @Override
    public Map<String, String> start() {
        minioServer.start();

        return CollectionHelper.mapOf(
                MinioResource.PARAM_SERVER_PORT, minioServer.getMappedPort(BROKER_PORT) + "",
                MinioResource.PARAM_SERVER_HOST, minioServer.getHost());
    }

    @Override
    public void stop() {
        if(minioServer.isRunning()) {
            minioServer.stop();
        }
    }
}
