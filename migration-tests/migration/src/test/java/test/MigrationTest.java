package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MigrationTest {
    //
    //    @BeforeAll
    //    public static void update() throws Exception {
    //        System.out.println("************ updating *************************");
    //        Runtime.getRuntime().exec("java version");
    ////        Runtime.getRuntime().exec("qss update --update-recipes-version=1.0.3-SNAPSHOT");
    //    }

    @Test
    public void migrationExecution() throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        System.out.println("**************** migrating *************************");
        builder.command("qss", "update", "--update-recipes-version=1.0.3-SNAPSHOT");

        //get modules directory
        Path modulePath = Paths.get(this.getClass().getClassLoader().getResource("").getPath());
        //resolve csimple
        Path parent = modulePath.getParent().getParent().getParent();
        //list all subdirectories
        Files.list(parent)
                .filter(Files::isDirectory)
                //skip migration module
                .filter(f -> !f.getFileName().endsWith("migration"))
                //pom.xml has to exist in that directory
                .filter(f -> f.resolve("pom.xml").toFile().exists())
                .forEach(MigrationTest::runMigration);
    }

    private static void runMigration(Path module) {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            System.out.println("************ migrating " + module.getFileName() + " *************************");
            builder.command("qss", "update", "--update-recipes-version=1.0.3-SNAPSHOT");

            builder.directory(module.toFile());
            Process process = builder.start();

            OutputStream outputStream = process.getOutputStream();
            InputStream inputStream = process.getInputStream();
            InputStream errorStream = process.getErrorStream();

            printStream(inputStream);
            printStream(errorStream);

            boolean isFinished = process.waitFor(30, TimeUnit.SECONDS);
            outputStream.flush();
            outputStream.close();

            if (!isFinished) {
                process.destroyForcibly();
            }

        } catch (Exception e) {
            //todo disable module for the testing
            e.printStackTrace();
        }
    }

    private static void printStream(InputStream inputStream) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
            }

        }
    }
}
