# PQC Test Support Module

Auto-registration of Post-Quantum Cryptography (PQC) keypairs as CDI beans for Quarkus integration tests.

## Overview

This module provides a Quarkus extension that automatically generates and registers PQC keypairs as named CDI beans based on the `@PQCKeyPairs` annotation. Keypairs are generated directly at build time with **zero setup** and **no disk I/O**.

## Usage

### 1. Add Dependency

Add the deployment module to your test module's `pom.xml`:

```xml
<dependency>
    <groupId>org.apache.camel.quarkus</groupId>
    <artifactId>camel-quarkus-integration-tests-support-pqc-deployment</artifactId>
    <scope>test</scope>
</dependency>
```

### 2. Declare Keypairs

Annotate your test class with `@PQCKeyPairs`:

```java
@QuarkusTest
@PQCKeyPairs(keyPairs = {
    @PQCKeyPair(name = "dilithiumKeyPair", algorithm = DILITHIUM2),
    @PQCKeyPair(name = "falconKeyPair", algorithm = DILITHIUM3),
    @PQCKeyPair(name = "sphincsKeyPair", algorithm = DILITHIUM5)
})
class MyPqcTest {
    // Test implementation
}
```

### 3. Inject Keypairs

Use `@Inject @Named` to inject the generated keypairs:

```java
@Inject
@Named("dilithiumKeyPair")
KeyPair dilithiumKeyPair;

@Inject
@Named("falconKeyPair")
KeyPair falconKeyPair;
```

That's it! No manual producer methods, no file generation required.

## How It Works

1. **Build Time**: The `PQCKeyPairBuildStep` scans for `@PQCKeyPairs` annotations
2. **Key Generation**: Generates keypairs directly using BouncyCastle PQC provider (~100ms total)
3. **Bean Registration**: Creates `SyntheticBeanBuildItem` for each keypair with `@Singleton @Named` scope
4. **Runtime**: Keypairs are reconstructed from encoded bytes and injected as CDI beans

## Architecture

```
integration-tests-support/pqc/
├── runtime/                        # Runtime module
│   ├── PQCAlgorithm.java          # Enum of supported algorithms
│   ├── PQCKeyPair.java            # Single keypair annotation
│   ├── PQCKeyPairs.java           # Container annotation
│   ├── PQCKeyPairRecorder.java    # Quarkus recorder for runtime reconstruction
│   └── PQCKeyPairGenerationExtension.java  # JUnit extension (for tests needing files)
└── deployment/                     # Deployment module
    └── PQCKeyPairBuildStep.java   # Build step for bean registration
```

## Supported Algorithms

- **Dilithium2** - Fast, smaller keys
- **Dilithium3** - Balanced security/performance
- **Dilithium5** - Maximum security

## Performance

- **Key generation**: ~5-30ms per keypair
- **Total overhead**: ~100ms for typical test suite (5-7 keypairs)
- **No disk I/O**: All generation happens in-memory at build time

## Comparison with Manual Approach

### Before (Manual Producers)
```java
@ApplicationScoped
public class PqcKeyPairProducers {
    @Produces @Singleton @Named("dilithiumKeyPair")
    public KeyPair dilithiumKeyPair() throws Exception {
        return generateKeyPair("Dilithium", DilithiumParameterSpec.dilithium2);
    }
    // ... more manual producer methods
}
```

### After (Automatic)
```java
@PQCKeyPairs(keyPairs = {
    @PQCKeyPair(name = "dilithiumKeyPair", algorithm = DILITHIUM2)
})
```

No manual producer class needed! ✨

## JUnit Extension (Optional)

The `@PQCKeyPairs` annotation also triggers a JUnit extension that can generate keypair files to disk. This is useful if tests need actual key files for file-based operations.

**Parameters** (JUnit extension only):
- `baseDir`: Directory for generated files (default: `target/certs`)
- `replaceIfExists`: Overwrite existing files (default: `false`)

**Note**: CDI bean registration ignores these parameters and generates keys in-memory.

## Migration from Certificate-Generator

If you were using `PqcKeyPairProducers` from `certificate-generator`, simply:

1. Replace dependency with `camel-quarkus-integration-tests-support-pqc-deployment`
2. Add `@PQCKeyPairs` annotation to your test class
3. Remove manual producer class (if you had one)
4. Injection code remains unchanged

## Examples

See integration tests in:
- `integration-tests/pqc/` - PQC component tests
- `integration-tests-jvm/pqc/` - JVM-only PQC tests
