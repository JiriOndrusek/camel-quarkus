<!-- apache/camel-quarkus#8407 -->

<!-- apache/camel-quarkus#8407 -->

# Fix plan: Provide native support for camel-pqc (Post-Quantum Crypto) component

## Issue
The camel-pqc extension has been promoted from JVM-only (`extensions-jvm/pqc`) to native support (`extensions/pqc`) and includes basic GraalVM registrations in `PqcProcessor`, but is missing the cipher transformation registration pattern used by other cryptographic extensions (crypto-pgp). Without explicit cipher transformation registration via `CipherTransformationBuildItem`, GraalVM may not properly analyze reachability of PQC algorithm implementations at build time, potentially causing runtime failures in native mode when attempting to use post-quantum cryptographic algorithms like Dilithium, Falcon, Kyber, or SPHINCS+.

## Root cause
The current `PqcProcessor.java` registers BouncyCastle PQC classes for reflection and runtime-initializes the `BouncyCastlePQCProvider`, but does not follow the established pattern used by `crypto-pgp` and other crypto extensions that produce `CipherTransformationBuildItem` instances. This build item signals to `BouncyCastleSupportProcessor` which cipher transformations are used, allowing it to explicitly instantiate algorithms via `BouncyCastleRecorder` at `STATIC_INIT` time. This explicit instantiation pattern helps GraalVM understand which security services are reachable and need to be included in the native image. Additionally, the extension's parent pom still references the JVM-only parent (`camel-quarkus-extensions-jvm`) instead of the native-capable parent (`camel-quarkus-extensions`).

## Proposed fix

### Step 1: Fix parent POM reference
Update `extensions/pqc/pom.xml` line 24 to change the parent from `camel-quarkus-extensions-jvm` to `camel-quarkus-extensions`:

```xml
<parent>
    <groupId>org.apache.camel.quarkus</groupId>
    <artifactId>camel-quarkus-extensions</artifactId>
    <version>...</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```

### Step 2: Add PQC algorithm transformation registration
Create a new method in `PqcProcessor.java` to produce `CipherTransformationBuildItem` instances for PQC algorithms. Reference the pattern from `extensions-support/bouncycastle/deployment/src/main/java/org/apache/camel/quarkus/support/bouncycastle/deployment/BouncyCastleSupportProcessor.java` and `CipherTransformationBuildItem.java`.

Add after the existing `runtimeInitializeProvider()` method:

```java
@BuildStep
void registerPqcCipherTransformations(BuildProducer<CipherTransformationBuildItem> cipherTransformations) {
    // Digital Signature algorithms
    cipherTransformations.produce(new CipherTransformationBuildItem("Dilithium"));
    cipherTransformations.produce(new CipherTransformationBuildItem("Falcon"));
    cipherTransformations.produce(new CipherTransformationBuildItem("SPHINCS+"));
    cipherTransformations.produce(new CipherTransformationBuildItem("SPHINCSPlus"));

    // KEM (Key Encapsulation Mechanism) algorithms
    cipherTransformations.produce(new CipherTransformationBuildItem("Kyber"));

    // Key generation algorithms
    cipherTransformations.produce(new CipherTransformationBuildItem("DilithiumKeyPairGenerator"));
    cipherTransformations.produce(new CipherTransformationBuildItem("FalconKeyPairGenerator"));
    cipherTransformations.produce(new CipherTransformationBuildItem("KyberKeyPairGenerator"));
    cipherTransformations.produce(new CipherTransformationBuildItem("SPHINCSPlusKeyPairGenerator"));
}
```

### Step 3: Import CipherTransformationBuildItem
Add the import at the top of `PqcProcessor.java`:

```java
import org.apache.camel.quarkus.support.bouncycastle.deployment.CipherTransformationBuildItem;
```

### Step 4: Verify dependency on bouncycastle-support deployment
Check that `extensions/pqc/deployment/pom.xml` has a dependency on `camel-quarkus-support-bouncycastle-deployment`. If not present, add:

```xml
<dependency>
    <groupId>org.apache.camel.quarkus</groupId>
    <artifactId>camel-quarkus-support-bouncycastle-deployment</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Step 5: Re-enable integration tests
Remove the `@Disabled` annotations from the test methods in:
- `integration-tests/pqc/src/test/java/org/apache/camel/quarkus/component/pqc/it/PqcTest.java`

The tests are currently disabled due to "BouncyCastle 1.83 algorithm naming and header compatibility issues". Verify if these issues are resolved in the current BC version. If compatibility issues persist, this may be a separate upstream issue that needs addressing before native testing can proceed.

### Step 6: Add native test class
Create `integration-tests/pqc/src/test/java/org/apache/camel/quarkus/component/pqc/it/PqcIT.java` following the standard pattern:

```java
package org.apache.camel.quarkus.component.pqc.it;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class PqcIT extends PqcTest {
}
```

## Files to change

| File | What to change |
|------|---------------|
| `extensions/pqc/pom.xml` | Change parent from `camel-quarkus-extensions-jvm` to `camel-quarkus-extensions` (line 24) |
| `extensions/pqc/deployment/pom.xml` | Add dependency on `camel-quarkus-support-bouncycastle-deployment` if missing |
| `extensions/pqc/deployment/src/main/java/org/apache/camel/quarkus/component/pqc/deployment/PqcProcessor.java` | Add import for `CipherTransformationBuildItem`; Add `registerPqcCipherTransformations()` build step method |
| `integration-tests/pqc/src/test/java/org/apache/camel/quarkus/component/pqc/it/PqcTest.java` | Remove `@Disabled` annotations if BC 1.83 compatibility is resolved |
| `integration-tests/pqc/src/test/java/org/apache/camel/quarkus/component/pqc/it/PqcIT.java` | Create new file for native integration tests extending `PqcTest` |

## Edge cases to consider

1. **Algorithm naming variations**: BouncyCastle PQC may use different algorithm name formats (e.g., "SPHINCS+" vs "SPHINCSPlus"). Register both variants to ensure compatibility.

2. **Provider ordering**: BouncyCastlePQCProvider must be registered before algorithms are accessed. The current runtime initialization should handle this, but verify provider is available during `STATIC_INIT`.

3. **Parameter spec classes**: Ensure all parameter spec classes (DilithiumParameterSpec, FalconParameterSpec, KyberParameterSpec, SPHINCSPlusParameterSpec) are reachable. Current index-based registration should cover these.

4. **BouncyCastle version compatibility**: Tests reference BC 1.83 compatibility issues. Verify current BC version in `pom.xml` and whether upstream Camel PQC component has addressed naming/header format changes.

5. **KEM vs Signature operations**: Different PQC algorithms serve different purposes (Kyber for KEM, Dilithium/Falcon/SPHINCS+ for signatures). Ensure both operation types are tested.

6. **Native image size impact**: PQC algorithms may significantly increase native image size. Monitor binary size after enabling native support.

## Testing

### Unit tests (deployment module)
No new deployment unit tests required - existing pattern from other crypto extensions is well-established.

### Integration tests (JVM mode)
Run existing tests in `integration-tests/pqc/`:

```bash
./mvnw clean verify -pl integration-tests/pqc
```

**Expected tests to pass** (after removing `@Disabled`):
- `PqcTest.testSignAndVerify()` - Tests Dilithium digital signature operations
- `PqcTest.testKem()` - Tests Kyber KEM encapsulation/decapsulation
- `PqcTest.testSignatureAlgorithms()` - Tests multiple signature algorithms (Dilithium, Falcon, SPHINCS+)

If tests still fail due to BC 1.83 compatibility, investigate:
1. Check Camel version for PQC component fixes
2. Review algorithm naming in `PqcResource.java` vs BC provider
3. Consider updating BouncyCastle version if compatible with Camel

### Integration tests (Native mode)
After JVM tests pass, run native compilation:

```bash
./mvnw clean verify -pl integration-tests/pqc -Dnative -Ddocker
```

**Success criteria**:
- Native image builds without GraalVM errors
- `PqcIT` (native mode) tests pass with same assertions as `PqcTest` (JVM mode)
- No reflection warnings for PQC classes
- No missing resource errors for crypto provider

### Verification checklist
- [ ] Extension parent POM references correct parent artifact
- [ ] CipherTransformationBuildItem instances produced for all PQC algorithms
- [ ] JVM integration tests pass
- [ ] Native compilation succeeds
- [ ] Native integration tests (`PqcIT`) pass
- [ ] No GraalVM warnings about missing registrations in build output
- [ ] Binary size increase is acceptable (document before/after)

### Test commands summary
```bash
# Fast build to verify compilation
./mvnw clean install -pl extensions/pqc -am -Dquickly

# JVM integration tests
./mvnw clean verify -pl integration-tests/pqc

# Native integration tests (slow)
./mvnw clean verify -pl integration-tests/pqc -Dnative -Ddocker

# Full verification with formatting
./mvnw clean install -pl extensions/pqc,integration-tests/pqc -am
./mvnw process-resources -Pformat
```
