package org.apachencamel.quarkus.test.extensions;

import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class HelloET extends CamelQuarkusTestSupport {

    @Override
    protected void doAfterConstruct() throws Exception {
        super.doAfterConstruct();
    }

    @Test
    public void hello1Test() throws Exception {
        Files.createDirectories(testDirectory());
        Path testFile = testFile("hello.txt");
        Files.write(testFile, "Hello ".getBytes());

        RestAssured.given()
                .body(fileUri() + "?fileName=hello.txt")
                .post("/hello/message")

                .then()
                .statusCode(200)
                .body(is("Hello Sheldon"));

    }

    @Test
    public void hello2Test() throws Exception {
        Files.createDirectories(testDirectory());
        Path testFile = testFile("hello.txt");
        Files.write(testFile, "Hello ".getBytes());

        RestAssured.given()
                .body(fileUri() + "?fileName=hello.txt")
                .post("/hello/message")

                .then()
                .statusCode(200)
                .body(is("Hello Leonard"));
    }

    @Test
    public void hello3Test() throws Exception {
        Files.createDirectories(testDirectory());
        Path testFile = testFile("hello.txt");
        Files.write(testFile, "Hello ".getBytes());

        RestAssured.given()
                .body(fileUri() + "?fileName=hello.txt")
                .post("/hello/message")

                .then()
                .statusCode(200)
                .body(is("Hello Leonard"));
    }

}
