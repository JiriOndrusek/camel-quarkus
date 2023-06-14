package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import groovy.lang.GroovyShell;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MigrationTest {

    @Test
    public void createModules() throws Exception {
        Path migrationsPath = Paths.get(MigrationTest.class.getClassLoader().getResource("").getPath())
                .getParent().getParent().getParent();
        //read all modules to migrate
        List<String> modules = Files
                .readAllLines(Paths.get(MigrationTest.class.getClassLoader().getResource("modules").getPath()));
        for (String module : modules) {
            createModule(module, migrationsPath);
            copyModule(module, migrationsPath);
        }

        createGeneratedPOM(modules, migrationsPath.resolve("generated").resolve("pom.xml"));

        for (String module : modules) {
            runMigration(migrationsPath.resolve("generated").resolve(module));
            upgradeQuarkusInPom(migrationsPath.resolve("generated").resolve(module).resolve("pom.xml"));
        }
    }

    private void createGeneratedPOM(List<String> modules, Path targetLocation) throws Exception {

        List<String> pomContent = Arrays.asList(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!--\n" +
                "\n" +
                "    Licensed to the Apache Software Foundation (ASF) under one or more\n" +
                "    contributor license agreements.  See the NOTICE file distributed with\n" +
                "    this work for additional information regarding copyright ownership.\n" +
                "    The ASF licenses this file to You under the Apache License, Version 2.0\n" +
                "    (the \"License\"); you may not use this file except in compliance with\n" +
                "    the License.  You may obtain a copy of the License at\n" +
                "\n" +
                "         http://www.apache.org/licenses/LICENSE-2.0\n" +
                "\n" +
                "    Unless required by applicable law or agreed to in writing, software\n" +
                "    distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                "    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                "    See the License for the specific language governing permissions and\n" +
                "    limitations under the License.\n" +
                "\n" +
                "-->\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                +
                "\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <parent>\n" +
                "        <groupId>org.apache.camel.quarkus</groupId>\n" +
                "        <artifactId>camel-quarkus</artifactId>\n" +
                "        <version>2.13.4-SNAPSHOT</version>\n" +
                "        <relativePath>../../pom.xml</relativePath>\n" +
                "    </parent>\n" +
                "\n" +
                "    <artifactId>camel-quarkus-migration-generated-tests</artifactId>\n" +
                "    <packaging>pom</packaging>\n" +
                "\n" +
                "    <name>Camel Quarkus :: Migration Generated Tests</name>\n" +
                "\n" +
                "    <properties>\n" +
                "        <quarkus.banner.enabled>false</quarkus.banner.enabled>\n" +
                "    </properties>\n" +
                "\n" +
                "    <modules>").split("\\n"));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetLocation.toFile()))) {
            for (String line : pomContent) {
                writer.write(line);
                writer.newLine();
            }
            for (String module : modules) {
                writer.write("        <module>" + module + "</module>");
                writer.newLine();
            }
            writer.write("    </modules>");
            writer.newLine();
            writer.write("</project>");
            writer.newLine();
        }
    }

    private void createModule(final String module, Path migrationsPath) throws Exception {
        System.out.println("**************** creating " + module + " *************************");

        if (migrationsPath.resolve("generated").toFile().exists()
                && Files.list(migrationsPath.resolve("generated")).anyMatch(new Predicate<Path>() {
                    public boolean test(Path path) {
                        return path.toString().endsWith(File.separator + module) && path.toFile().exists();
                    }
                })) {
            System.out.println("already exists");
            return;
        }

        //create folder
        if (!migrationsPath.resolve("generated").toFile().exists()) {
            migrationsPath.resolve("generated").toFile().mkdirs();
        }
        migrationsPath.resolve("generated").resolve(module).toFile().mkdirs();
        //copy pom file
        copyAndModifyPom(migrationsPath.getParent().resolve("integration-tests").resolve(module).resolve("pom.xml"),
                migrationsPath.resolve("generated").resolve(module).resolve("pom.xml"));
    }

    private void copyModule(final String module, Path migrationsPath) throws Exception {
        //copy content using copy-tests.groovy
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("copy-tests.source.dir",
                migrationsPath.getParent().resolve("integration-tests").resolve(module).toString());
        properties.put("copy-tests.dest.module.dir", migrationsPath.resolve("generated").resolve(module).toString());
        properties.put("copy-tests.dest.directly", "true");
        // Create a GroovyShell with the binding
        GroovyShell shell = new GroovyShell();
        shell.setProperty("properties", properties);

        // Run the Groovy script
        Path groovyScript = migrationsPath.getParent().resolve("tooling").resolve("scripts").resolve("copy-tests.groovy");
        Object result = shell.evaluate(groovyScript.toFile());
    }

    private void upgradeQuarkusInPom(Path pom) throws Exception {
        System.out.println(
                "**************** upgrading Qarkus and Camel Quarkus BOM version (" + pom + ") *************************");
        LinkedList<String> pomContent = new LinkedList<>(Files.readAllLines(pom));
        for (final ListIterator<String> iterator = pomContent.listIterator(); iterator.hasNext();) {
            String line = iterator.next();
            if (line.contains("<quarkus.version>")) {
                iterator.set("<quarkus.version>999-SNAPSHOT</quarkus.version>");
            }
            if (line.contains("<quarkus.platform.version>")) {
                iterator.set("<quarkus.platform.version>999-SNAPSHOT</quarkus.platform.version>");
            }
            if (line.contains("<camel-quarkus.platform.version>")) {
                iterator.set("<camel-quarkus.platform.version>3.0.0-SNAPSHOT</camel-quarkus.platform.version>");
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pom.toFile()))) {
            for (String line : pomContent) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private void copyAndModifyPom(Path pom, Path targetLocation) throws Exception {
        LinkedList<String> pomContent = new LinkedList<>(Files.readAllLines(pom));

        // Modify the content (remove and add lines)
        modifyPom(pomContent);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetLocation.toFile()))) {
            for (String line : pomContent) {
                writer.write(line);
                writer.newLine();
            }
        }

    }

    private void runMigration(Path module) {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            System.out.println("************ upgrading " + module.getFileName() + " *************************");
            builder.command("qss", "update", "--update-recipes-version=1.0.3-SNAPSHOT");

            builder.directory(module.toFile());
            Process process = builder.start();

            OutputStream outputStream = process.getOutputStream();
            InputStream inputStream = process.getInputStream();
            InputStream errorStream = process.getErrorStream();

            printStream(inputStream);
            printStream(errorStream);

            boolean isFinished = process.waitFor(200, TimeUnit.SECONDS);
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
                System.out.println("......" + line);
            }

        }
    }

    private void modifyPom(LinkedList<String> fileContent) {

        //instead of parent, keep only groupId and version
        PomPart part = PomPart.start;
        for (final ListIterator<String> iterator = fileContent.listIterator(); iterator.hasNext();) {
            String line = iterator.next();
            switch (part) {
            case start:
                if (line.contains("<parent>")) {
                    part = PomPart.parent;
                }
                continue;
            case parent:
                if (line.contains("</parent>")) {
                    part = PomPart.header;
                }
                if (line.contains("<artifactId>camel-quarkus-build-parent-it</artifactId>")) {
                    iterator.set(line.replaceFirst("camel-quarkus-build-parent-it", "camel-quarkus-build-parent-mi-it"));
                }
                if (line.contains("../poms/build-parent-it")) {
                    iterator.set(line.replaceFirst("../poms/build-parent-it", "../../poms/build-parent-mi-it"));
                }
                continue;
            case header:
                if (line.contains("artifactId")) {
                    iterator.set(line.replaceFirst("integration", "migration"));
                }
                if (line.contains("name") || line.contains("description")) {
                    iterator.set(line.replaceFirst("Integration", "Migration"));
                }
                if (line.contains("<dependencies>")) {
                    part = PomPart.dependencies;
                    iterator.remove();
                }
                continue;
            case dependencies:
                iterator.remove();
                Arrays.stream(("    <properties>\n" +
                        "        <quarkus.version>2.13.8.Final</quarkus.version><!-- https://repo1.maven.org/maven2/io/quarkus/quarkus-bom/ -->\n"
                        +
                        "        <!-- Allow running our tests against alternative BOMs, such as io.quarkus.platform:quarkus-camel-bom https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-camel-bom/ -->\n"
                        +
                        "        <quarkus.platform.group-id>io.quarkus</quarkus.platform.group-id>\n" +
                        "        <quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>\n" +
                        "        <quarkus.platform.version>${quarkus.version}</quarkus.platform.version>\n" +
                        "        <camel-quarkus.platform.group-id>org.apache.camel.quarkus</camel-quarkus.platform.group-id>\n"
                        +
                        "        <camel-quarkus.platform.artifact-id>camel-quarkus-bom</camel-quarkus.platform.artifact-id>\n" +
                        "        <camel-quarkus.platform.version>2.13.4-SNAPSHOT</camel-quarkus.platform.version>\n" +
                        "        <camel-quarkus.version>3.0.0-SNAPSHOT</camel-quarkus.version><!-- This needs to be set to the underlying CQ version from command line when testing against Platform BOMs -->\n"
                        +
                        "\n" +
                        "        <quarkus.banner.enabled>false</quarkus.banner.enabled>\n" +
                        "        <maven.compiler.source>17</maven.compiler.source>\n" +
                        "        <maven.compiler.target>17</maven.compiler.target>\n" +
                        "    </properties>\n" +
                        "\n" +
                        "    <dependencyManagement>\n" +
                        "        <dependencies>\n" +
                        "            <dependency>\n" +
                        "                <groupId>${quarkus.platform.group-id}</groupId>\n" +
                        "                <artifactId>${quarkus.platform.artifact-id}</artifactId>\n" +
                        "                <version>${quarkus.platform.version}</version>\n" +
                        "                <type>pom</type>\n" +
                        "                <scope>import</scope>\n" +
                        "            </dependency>\n" +
                        "            <dependency>\n" +
                        "                <groupId>${camel-quarkus.platform.group-id}</groupId>\n" +
                        "                <artifactId>${camel-quarkus.platform.artifact-id}</artifactId>\n" +
                        "                <version>${camel-quarkus.platform.version}</version>\n" +
                        "                <type>pom</type>\n" +
                        "                <scope>import</scope>\n" +
                        "            </dependency>\n" +
                        "            <dependency>\n" +
                        "                <groupId>org.apache.camel.quarkus</groupId>\n" +
                        "                <artifactId>camel-quarkus-bom-test</artifactId>\n" +
                        "                <version>${camel-quarkus.version}</version>\n" +
                        "                <type>pom</type>\n" +
                        "                <scope>import</scope>\n" +
                        "            </dependency>\n" +
                        "        </dependencies>\n" +
                        "    </dependencyManagement>\n" +
                        "\n" +
                        "    <dependencies>\n" +
                        "        <dependency>").split("\\n"))
                        .forEach(new Consumer<String>() {
                            public void accept(String s) {
                                iterator.add(s);
                            }
                        });
                part = PomPart.other;
                continue;
            case other:
                if (line.contains("<plugins>")) {
                    part = PomPart.plugins;
                    iterator.remove();
                }
                continue;
            case plugins:
                iterator.remove();
                if (line.contains("</plugins>")) {
                    part = PomPart.other;

                }
                continue;
            }
        }
    }

    private enum PomPart {
        start, parent, header, dependencies, plugins, other
    }

}
