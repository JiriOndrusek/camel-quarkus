# Plan: Add PQC TLS 1.3 Test with openquantumsafe/nginx Docker Container

## Context

This plan addresses adding a test for Post-Quantum Cryptography (PQC) TLS 1.3 to the camel-http integration tests in `integration-test-groups/http/http/`. 

**Current State:**
- The HTTP integration tests already include basic PQC support:
  - `testPqcSign()` tests PQC digital signatures using Dilithium
  - `testPqcTls()` tests TLS connections but only against the local Quarkus test server
  - Dependencies on `camel-quarkus-pqc` and `camel-quarkus-integration-tests-support-pqc` are already present
  - BouncyCastle BCPQC provider is registered for PQC cryptographic operations

**Goal:**
Test PQC TLS 1.3 handshake between camel-http client and an external server (openquantumsafe/nginx Docker container) using PQC-signed certificates.

**Why This Matters:**
The existing `testPqcTls()` only validates TLS against the local Quarkus server with standard certificates. Testing against openquantumsafe/nginx will verify that camel-http can properly handle PQC-signed certificates from real-world servers, which is critical for post-quantum security readiness.

## Approach

### High-Level Strategy

Use the **openquantumsafe/nginx** Docker image to create an HTTPS server with PQC-signed certificates, then test that camel-http can successfully connect and communicate over TLS 1.3.

### Key Technical Decisions

1. **Certificate Type**: Use **Dilithium2-signed X.509 certificates** (not full PQC cipher suites)
   - Dilithium is NIST-standardized and supported by BouncyCastle BCPQC
   - Standard TLS 1.3 with PQC certificates is more realistic than full PQC cipher suites
   - Java's standard JSSE/Apache HttpClient can validate PQC-signed certs if truststore is configured properly

2. **Container Management**: Create **PqcNginxTestResource** following the Testcontainers pattern
   - Extends `QuarkusTestResourceLifecycleManager`
   - Generates PQC certificates before container start
   - Configures and starts openquantumsafe/nginx with PQC certs
   - Follows existing patterns from `KafkaSaslSslTestResource` and `PahoTestResource`

3. **Certificate Generation**: Use **BouncyCastle BCPQC** to generate Dilithium2 keypairs and self-signed certificates
   - Generate at test resource startup time (before container launch)
   - Export to PEM format for nginx compatibility
   - Create both server certificate and client truststore

4. **Client Configuration**: Create **pqcNginxSslContextParameters** bean with custom truststore
   - Trust the Dilithium-signed CA certificate
   - Use standard TLS 1.3 protocol
   - BouncyCastle will handle PQC signature validation

## Implementation Details

### 1. Create PqcNginxTestResource.java

**File**: `integration-test-groups/http/http/src/test/java/org/apache/camel/quarkus/component/http/http/it/PqcNginxTestResource.java`

This test resource will:
- Generate PQC certificates using BouncyCastle BCPQC provider
- Start openquantumsafe/nginx container with PQC certificates mounted
- Expose nginx HTTPS port for testing
- Provide configuration properties (nginx host, port, truststore path)

**Key Methods**:
```java
@Override
public Map<String, String> start() {
    // 1. Generate Dilithium2 key pair
    // 2. Create self-signed X.509 certificate with Dilithium signature
    // 3. Export certificate and key to PEM files
    // 4. Create truststore with CA certificate
    // 5. Start nginx container with mounted certificates
    // 6. Return config properties (host, port, truststore)
}

@Override
public void stop() {
    // Stop and remove container
}

private void generatePqcCertificates() {
    // Use org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
    // Generate Dilithium2 keypair
    // Create X509v3CertificateBuilder with Dilithium signature
}
```

**Dependencies to use**:
- `org.testcontainers.containers.GenericContainer`
- `org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider`
- `org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec`
- `org.bouncycastle.cert.X509v3CertificateBuilder`
- `org.bouncycastle.cert.jcajce.JcaX509CertificateConverter`
- `org.bouncycastle.operator.jcajce.JcaContentSignerBuilder`

**Certificate Generation Pattern**:
```java
Security.addProvider(new BouncyCastlePQCProvider());

KeyPairGenerator keyGen = KeyPairGenerator.getInstance("Dilithium", "BCPQC");
keyGen.initialize(DilithiumParameterSpec.dilithium2);
KeyPair keyPair = keyGen.generateKeyPair();

X500Name issuer = new X500Name("CN=PQC Nginx Test CA");
X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
    issuer,
    BigInteger.valueOf(System.currentTimeMillis()),
    new Date(),
    new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000),
    issuer,
    keyPair.getPublic()
);

ContentSigner signer = new JcaContentSignerBuilder("Dilithium").build(keyPair.getPrivate());
X509Certificate cert = new JcaX509CertificateConverter()
    .setProvider("BCPQC")
    .getCertificate(certBuilder.build(signer));

// Write to PEM files for nginx
// Create truststore for Java client
```

**Container Configuration**:
```java
GenericContainer<?> nginxContainer = new GenericContainer<>("openquantumsafe/nginx")
    .withExposedPorts(443)
    .withCopyFileToContainer(
        MountableFile.forHostPath(nginxConfPath),
        "/etc/nginx/conf.d/default.conf"
    )
    .withCopyFileToContainer(
        MountableFile.forHostPath(certPath),
        "/etc/nginx/tls/pqc-nginx.crt"
    )
    .withCopyFileToContainer(
        MountableFile.forHostPath(keyPath),
        "/etc/nginx/tls/pqc-nginx.key"
    )
    .withLogConsumer(new Slf4jLogConsumer(LOG).withPrefix("PQC-NGINX"))
    .waitingFor(Wait.forListeningPort());

nginxContainer.start();
```

### 2. Create Nginx Configuration File

**File**: `integration-test-groups/http/http/src/test/resources/nginx/pqc-default.conf`

```nginx
server {
    listen 443 ssl;
    server_name localhost;
    
    ssl_certificate /etc/nginx/tls/pqc-nginx.crt;
    ssl_certificate_key /etc/nginx/tls/pqc-nginx.key;
    ssl_protocols TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    location /pqc-test {
        return 200 "PQC TLS connection successful from nginx\n";
        add_header Content-Type text/plain;
    }
}
```

### 3. Update HttpProducers.java

**File**: `integration-test-groups/http/http/src/main/java/org/apache/camel/quarkus/component/http/http/HttpProducers.java`

Add a new SSL context parameters bean for the nginx container:

```java
@Named
public SSLContextParameters pqcNginxSslContextParameters() {
    // Load truststore containing PQC certificate
    KeyStoreParameters truststoreParameters = new KeyStoreParameters();
    truststoreParameters.setResource("file://target/certs/pqc-nginx-truststore.p12");
    truststoreParameters.setPassword("changeit");
    
    TrustManagersParameters trustManagersParameters = new TrustManagersParameters();
    trustManagersParameters.setKeyStore(truststoreParameters);
    
    SSLContextParameters sslContextParameters = new SSLContextParameters();
    sslContextParameters.setTrustManagers(trustManagersParameters);
    sslContextParameters.setSecureSocketProtocol("TLS");
    
    return sslContextParameters;
}
```

### 4. Update HttpRoutes.java

**File**: `integration-test-groups/http/http/src/main/java/org/apache/camel/quarkus/component/http/http/HttpRoutes.java`

Add a new route for the nginx container test:

```java
from("direct:pqc-nginx-tls")
    .to("https://{{pqc.nginx.host}}:{{pqc.nginx.port}}/pqc-test"
        + "?sslContextParameters=#pqcNginxSslContextParameters");
```

### 5. Update HttpResource.java

**File**: `integration-test-groups/http/http/src/main/java/org/apache/camel/quarkus/component/http/http/HttpResource.java`

Add a REST endpoint to trigger the nginx test:

```java
@Path("/pqc/nginx/tls")
@GET
@Produces(MediaType.TEXT_PLAIN)
public String pqcNginxTls() {
    return producerTemplate.requestBody("direct:pqc-nginx-tls", null, String.class);
}
```

### 6. Update HttpTest.java

**File**: `integration-test-groups/http/http/src/test/java/org/apache/camel/quarkus/component/http/http/it/HttpTest.java`

Add the test resource and test method:

**Add annotation**:
```java
@QuarkusTestResource(PqcNginxTestResource.class)
```

**Add test method**:
```java
@Test
public void testPqcNginxTls() {
    // Test HTTPS connection to openquantumsafe/nginx with PQC certificate
    RestAssured
        .given()
        .when()
        .get("/test/client/{component}/pqc/nginx/tls", component())
        .then()
        .statusCode(200)
        .body(is("PQC TLS connection successful from nginx"));
}
```

### 7. Dependencies Check

The `pom.xml` already has most dependencies, but verify these are present (likely inherited):
- `org.testcontainers:testcontainers` (for GenericContainer)
- `org.bouncycastle:bcpkix-jdk18on` (for X.509 certificate utilities)

These are likely already available through parent POMs used by other integration tests.

## Critical Files

**Files to Create:**
1. `/home/jondruse/git/community/camel-quarkus/integration-test-groups/http/http/src/test/java/org/apache/camel/quarkus/component/http/http/it/PqcNginxTestResource.java`
2. `/home/jondruse/git/community/camel-quarkus/integration-test-groups/http/http/src/test/resources/nginx/pqc-default.conf`

**Files to Modify:**
1. `/home/jondruse/git/community/camel-quarkus/integration-test-groups/http/http/src/main/java/org/apache/camel/quarkus/component/http/http/HttpProducers.java` - Add `pqcNginxSslContextParameters()` bean
2. `/home/jondruse/git/community/camel-quarkus/integration-test-groups/http/http/src/main/java/org/apache/camel/quarkus/component/http/http/HttpRoutes.java` - Add `direct:pqc-nginx-tls` route
3. `/home/jondruse/git/community/camel-quarkus/integration-test-groups/http/http/src/main/java/org/apache/camel/quarkus/component/http/http/HttpResource.java` - Add `/pqc/nginx/tls` endpoint
4. `/home/jondruse/git/community/camel-quarkus/integration-test-groups/http/http/src/test/java/org/apache/camel/quarkus/component/http/http/it/HttpTest.java` - Add `@QuarkusTestResource` annotation and `testPqcNginxTls()` test

## Verification

### Build and Test
```bash
cd integration-test-groups/http/http

# Run JVM mode tests
./mvnw clean test -Dtest=HttpTest#testPqcNginxTls

# Run full test suite
./mvnw clean verify

# Run native mode tests (slow)
./mvnw clean verify -Dnative -Ddocker
```

### Expected Behavior
1. PqcNginxTestResource generates Dilithium2-signed certificates
2. openquantumsafe/nginx container starts with PQC certificates
3. camel-http client connects over TLS 1.3
4. Client validates PQC certificate signature using BouncyCastle BCPQC
5. HTTP GET request succeeds and returns nginx response
6. Test passes in both JVM and native modes

### Validation Points
- Certificate generation completes without errors
- Nginx container starts and listens on port 443
- TLS handshake succeeds (no SSL errors)
- HTTP response body matches expected value
- Test completes in reasonable time (~10-30 seconds)

### Troubleshooting
- If certificate validation fails, check that BouncyCastle BCPQC provider is registered
- If container fails to start, check nginx logs via container.getLogs()
- If native mode fails, ensure PQC reflection configuration is registered (should already be done by PqcProcessor)
- Check that openquantumsafe/nginx image is accessible (may need Docker Hub authentication)

## Alternative Approaches Considered

### Full PQC Cipher Suite Testing
Instead of just PQC-signed certificates, could test full PQC cipher suites (e.g., TLS_KYBER_*):
- Requires BouncyCastle TLS provider (`bctls-jdk18on`)
- Nginx would need specific PQC cipher configuration
- More complex but tests complete PQC TLS stack

**Decision**: Start with certificate-based approach as it's simpler and tests real-world PQC deployment. Full cipher suites can be added later if needed.

### Using Pre-Generated Certificates
Could use static pre-generated PQC certificates instead of runtime generation:
- Simpler implementation
- Faster test startup

**Decision**: Generate at runtime to ensure freshness and avoid committing large binary certificates to repository.

### Using Different PQC Algorithms
Could use Falcon or SPHINCS+ instead of Dilithium:
- Falcon has smaller signatures
- SPHINCS+ is stateless

**Decision**: Use Dilithium2 as it's NIST-standardized, well-supported by BouncyCastle, and already used in existing `testPqcSign()`.
