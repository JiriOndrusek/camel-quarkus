# langchain4j-ingest delegation — PoC orientation guide

PoC status (2026-09-03): lives only on the local branch `feature/ingest-delegation-poc`
(based on origin/main, camel-quarkus 3.40.0-SNAPSHOT / Camel 4.22.x). The extension now
delegates its ENGINE (split/embed/store, dedup semantics) to the upstream `org.apache.camel:camel-langchain4j-ingest`
component (locally built 4.23.0-SNAPSHOT from `~/camel` branch `feature/camel-langchain4j-ingest`;
see the `POC-GUIDE.md` in that module). Verified: 23/23 deployment test suites and 12/12 JVM
integration tests (incl. Kafka and S3/MinIO containers) green.

## What moved where

```
 BEFORE (self-contained extension)          AFTER (thin wrapper over upstream)
 ─────────────────────────────────          ──────────────────────────────────
 core/IngestService   (engine)         ──>  deleted; upstream IngestService inside the
 core/IngestResult                          langchain4j-ingest producer; upstream IngestResult
 IngestRoutes 386 lines:                    IngestRoutes ~250 lines:
   route topology (file endpoint,             extends the INTERNAL IngestPipelineRouteBuilder,
   idempotentConsumer EIP, processors)        overrides pipelines(): pure config translation
   + CDI bean resolution                      + CDI bean resolution (kept, same messages)
 metadata keys camel_quarkus_*         ──>  camel_ingest_* (upstream-neutral)
 route ids camel-quarkus-l4j-ingest-*  ──>  langchain4j-ingest-<name>
```

Net: −345/+117 lines in this repo. What deliberately stays CQ-side: the two `@ConfigMapping`
roots, `@Ingest`/`IngestPipeline`/`Source` builder API + Jandex discovery, build-time
validation in `Langchain4jIngestProcessor` (all locked messages untouched), the pre-start
missing-component hint (`IngestComponentPresence` + recorder), and native-image registrations.

## Communication graph — from configuration to a running route

```
 build time (deployment module)
 ──────────────────────────────
 Langchain4jIngestProcessor
   ├─ validates config/@Ingest shapes  -> ValidationErrorBuildItem (messages unchanged)
   ├─ AdditionalBeanBuildItem(IngestRoutes)
   ├─ RoutesBuilderClassExcludeBuildItem(upstream IngestPipelineRouteBuilder)   <── NEW, see below
   └─ recorder: pre-start component presence check

 runtime start
 ─────────────
 ArC creates IngestRoutes (@ApplicationScoped, extends the internal IngestPipelineRouteBuilder)
   camel-main adds it as a routes builder -> configure() [internal builder] calls:
   │
   ├─ pipelines()   [overridden HERE — the whole delegation]
   │    ├─ union of quarkus.camel.langchain4j.ingest.<name>.* config roots
   │    │    enabled? source.uri vs source.directory conflict? (CQ messages kept)
   │    ├─ @Ingest builder entries (enabled check, source-override guard — CQ messages kept)
   │    ├─ CDI resolution: named -> unique-by-type -> actionable errors (CQ messages kept)
   │    │    Instance<EmbeddingStore<TextSegment>> / Instance<EmbeddingModel>
   │    ├─ idempotent repo: existence check + trySetCamelContext (CQ message kept)
   │    └─ -> List<IngestPipelineDefinition>   (store/model passed as INSTANCES)
   │
   └─ internal configurePipeline() per definition (topology package-private in CQ; option-4 split:
        engine-only upstream delegation - the topology is replaceable by an upstream artifact or kamelets)
        directory  -> file endpoint w/ safe defaults + register
        consumer   -> any URI; dedup now INSIDE the upstream producer (no idempotentConsumer EIP)
        both       -> setProperty(document id) -> to("langchain4j-ingest:<name>?...")
                                                        │
                                                        v
                                          upstream producer/engine
                                          (split -> batched embed -> store,
                                           claim/SKIPPED/EMPTY semantics preserved)
```

## The routes-discovery trap (important finding)

camel-quarkus discovers `RouteBuilder` classes via Jandex and instantiates them reflectively
(filtering to *public and non-abstract* classes). The upstream `IngestPipelineRouteBuilder`
was originally a concrete public `RouteBuilder` in a dependency jar, so discovery tried to
boot it as a second, empty routes builder and crashed on its protected constructor
(`IllegalAccessException` inside `CamelMainRecorder.addRoutesBuilder`). First fixed with a
`RoutesBuilderClassExcludeBuildItem` (precedent: camel-lra), then **resolved structurally**:
the upstream class is now an *abstract* base with an `of(...)` factory — the shape of every
framework-provided `RouteBuilder` — so discovery skips it by construction, in every CQ
application, and the exclusion build step was removed again. Kept here as the PoC's most
instructive finding: a component must never ship a concrete public `RouteBuilder`.

## Inherited upstream evolutions

The upstream component kept moving after the initial delegation; the extension inherits the
engine changes for free (EmbeddingStoreIngestor orchestration, single store write, tika's
plain-text output with the pinned-charset decode) and exposes the new knobs through its
own surface: `embedding-batch-size` (build-time, default 32, validated at build),
`document-splitter` (a `DocumentSplitter` bean name replacing the recursive default) and
`max-document-size` (default 0 = no limit; caps the text about to be split in characters —
the whole-document-in-memory DoS guard), each with an `IngestPipeline` builder twin. Note for the eventual #9078 reconciliation: the
"camel-tika text output loses small documents behind an unflushed writer" claim in that PR's
comments and this extension's released usage.adoc was disproven against the Tika 2.9/3.3/4.0
sources — correct it when the parser support is delegated.

## PoC liberties to undo in the real PR

1. `poms/bom/pom.xml` pins `camel-langchain4j-ingest:4.23.0-SNAPSHOT` (CQ's sanity-check
   forbids versions in extension poms). Becomes `${camel.version}` after the Camel 4.23
   upgrade. The flattened BOM poms were regenerated.
2. Behavior changes shipped without a migration note yet: metadata keys (`camel_quarkus_*`
   -> `camel_ingest_*`, affects already-written vectors' queryability by key), route id
   prefix, and the missing-document-id runtime message now uses the upstream producer's
   endpoint-option wording (`documentIdHeader`) instead of the quarkus property path.
3. Docs (`usage.adoc`, generated reference page) not yet updated for the delegation.

## Build & test

```
cd ~/camel-quarkus
./mvnw clean install -f extensions/langchain4j-ingest       # runtime + 23 deployment suites
./mvnw clean test -f integration-tests/langchain4j-ingest   # 12 JVM ITs (needs Docker)
```

The upstream jar must be in the local repo first:
`cd ~/camel && mvn -f components/camel-ai/camel-langchain4j-ingest/pom.xml clean install`.
