# ADR-0001: Microservices architecture

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Adriana Stanescu

## Context and problem
Covena is an AI assistant for contracts and internal policies: the company uploads documents (PDF, DOCX) and users ask questions, receiving answers with precise references to the document and page.

The project is part of my Upwork portfolio to demonstrate event-driven integration, reactive streaming, and retrieval-augmented generation (RAG) in a realistic, production-grade architecture.

Document ingestion (PDF parsing, text extraction, chunking, embeddings) is a relatively slow and CPU-intensive pipeline that must run asynchronously in the background, with the UI only confirming receipt and exposing results once processing completes.

In contrast, chat is interactive, long-lived work: responses stream gradually over connections that remain open for many seconds, and many users may be connected at the same time. This creates a strong need for a non-blocking programming model that can handle numerous concurrent, long-duration streams without dedicating a thread per connection.

Part of the AI stack (such as reranking and answer-quality evaluation) has its strongest ecosystem in Python. At the same time, the core application logic, APIs, and integration with enterprise systems rely on Java and Spring Boot. These constraints shape the decision about how the two environments should coexist.

The project is developed and maintained by a single engineer, which limits the operational complexity that can be supported.

## Decision drivers
- Isolation of ingestion workloads from interactive chat flows
- Per-component freedom to choose the appropriate programming model
- A clear polyglot boundary between Java and Python components
- Operational simplicity for single-engineer development and debugging
- Ability to demonstrate event-driven patterns, reactive streaming, and RAG to clients

## Considered options
1. Monolith: a single Spring Boot application, one deployment, one programming language.
2. Modular monolith: a single deployment, but with strictly separated internal modules (packages, boundaries enforced with ArchUnit).
3. Microservices: independently deployed services, each with its own data store, communicating through HTTP and Kafka events.

## Decision outcome
Chosen option: "Microservices", because it is the only option that satisfies four of the five drivers.
This choice allows us to isolate ingestion workloads from interactive chat flows, keep each component free to use its own programming model, maintain a clear boundary between Java and Python, and demonstrate event-driven patterns, reactive streaming, and RAG to clients.
The chosen architecture will be composed of microservices, each owning a specific domain capability, resulting in the following services: gateway, document-service, knowledge-service, chat-service and ai-lab-service.

This option does not satisfy operational simplicity for single-engineer development and debugging; the trade-off is accepted and mitigated (see Consequences).

## Consequences
### Positive
- A large or slow document upload cannot degrade chat: ingestion runs in its own process with its own CPU and memory budget.
- Each service uses the model that fits it: blocking Spring MVC with virtual threads for document-service, reactive WebFlux with SSE for chat-service.
- The Python ai-lab-service can be changed, redeployed or replaced without touching the Java services, as long as its API contract is kept.
- Each service can be scaled independently (for example, more knowledge-service instances during bulk ingestion).
- Event-driven integration (Kafka, transactional outbox), reactive streaming and RAG appear naturally in the code, which serves the portfolio goal.

### Negative / trade-offs
- Many containers to run locally; mitigated by a single docker-compose file that starts the whole infrastructure.
- A request passes through multiple services and is harder to debug; mitigated by distributed tracing (Micrometer Tracing + Zipkin) with a shared trace id in the logs.
- Configuration and build setup could be duplicated; mitigated by the shared parent POM and a future `common` module.
- Integration testing is harder; mitigated by Testcontainers, which starts real PostgreSQL and Kafka instances during tests.
- No shared database transaction across services; data is only eventually consistent. Mitigated by the transactional outbox (a database write and its event are committed together) and idempotent consumers (an event delivered twice is processed once).
- Calls between services can fail or be slow; mitigated by Resilience4j (timeouts, retries, circuit breakers).
- More moving parts mean more to learn and maintain; mitigated by keeping the number of services small (one per bounded context) and recording each further decision in an ADR.

## Pros and cons of the options
### Monolith
- ❌ **Workload isolation**: ingestion and chat share the same JVM, CPU and memory; a large PDF being parsed slows down every open chat stream.
- ❌ **Per-component programming model**: one Spring Boot application runs a single web stack; with both MVC and WebFlux starters on the classpath, MVC wins, so blocking and reactive components cannot each use their natural model.
- ❌ **Java/Python boundary**: Python code cannot run inside the JVM process; reranking and evaluation would have to live in a separate service anyway, so the "monolith" is no longer one deployable.
- ✅ **Operational simplicity**: one process, one database, one log stream; a failing request has a single stack trace and can be debugged with a breakpoint.
- ⚠️ **Demonstrating patterns**: RAG and streaming can be shown, but event-driven integration (Kafka, outbox) between modules of the same process would be artificial.

### Modular monolith
- ❌ **Workload isolation**: module boundaries separate the code, not the resources; ingestion and chat still share one JVM, so heavy ingestion still slows down chat.
- ❌ **Per-component programming model**: the application still runs a single web stack (MVC or WebFlux), so the modules cannot each choose their own model.
- ❌ **Java/Python boundary**: Python cannot run inside the modular monolith; AI-related Python tasks (reranking, evaluation) still require a separate service, breaking the "single deployable" assumption.
- ✅ **Operational simplicity**: one deployment, one process, one debugging context; modules remain isolated through ArchUnit rules, while development and local testing stay simple.
- ⚠️ **Demonstrating patterns**: RAG works well and modules can communicate through in-process events (e.g. Spring Modulith), but Kafka-based integration is less meaningful because modules are not independently deployed.

### Microservices
- ✅ **Workload isolation**: ingestion and chat run in completely separate services; a large PDF ingestion spike affects only the ingestion service, not the interactive chat flow.
- ✅ **Per-component programming model**: each service chooses its own model (reactive WebFlux for chat, blocking MVC with virtual threads for documents) without affecting the others.
- ✅ **Java/Python boundary**: Python AI components (reranking, evaluation) run as independent services with clear API boundaries; Java and Python communicate over HTTP or events, not through shared runtime.
- ❌ **Operational simplicity**: multiple services, multiple containers, multiple logs; debugging requires tracing across service boundaries rather than a single stack trace.
- ✅ **Demonstrating patterns**: event-driven integration (Kafka), outbox, reactive streaming, and RAG pipelines are all first-class citizens in a microservices architecture and can be shown naturally, not artificially.
