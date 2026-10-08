# ADR-0002: Separate persistence model from domain aggregate

- **Status:** Accepted
- **Date:** 2026-10-08
- **Deciders:** Adriana Stanescu
- **Scope:** all services with a DDD/hexagonal domain (document, knowledge, chat); first applied in document-service

## Context and problem

Covena uses DDD/hexagonal principles.
It is implemented by a single IT engineer.
In this document we refer to the Document Service microservice. The same considerations apply to the Knowledge and Chat
services.  
The persistence model is implemented using different approaches for each microservice.  
All three microservices store their domain aggregates in a PostgreSQL database.  
For the Document Service, a JPA persistence model implemented with Spring Data JPA is used.  
For the Chat Service, R2DBC (reactive, without JPA) is used.  
For the Knowledge Service, pgvector with Spring AI VectorStore / JDBC is used.

Following DDD, the Document Service models an aggregate called `Document`.  
The aggregate has the following characteristics:

- `Document` has a private constructor.
- Almost all fields are immutable except for `status` and `failureReason`. These fields are modified only by transition
  methods.
- Instances are created through the `upload()` factory method.
- Value objects are implemented as records.
- Domain classes do not depend on JPA or any other persistence framework.

Spring Data JPA requires a class annotated with `@Entity` that has a no-argument constructor, fields settable through
reflection, and mappings for value objects (`@Embeddable` or converters).

These requirements conflict with the aggregate design: a no-argument constructor and reflection-settable fields
would bypass `upload()` and the state-transition rules that protect its invariants.

We must decide where the JPA mapping should live: on the aggregate itself, or in a separate persistence model.

## Decision drivers

- Domain model free of persistence-framework dependencies.
- Aggregate invariant protection:
    - Creation is allowed only through `upload()`.
    - State changes are allowed only through explicit transition methods.
    - Validation rules are enforced in the private constructor.
- Database schema and domain model can evolve independently.
- Low implementation and maintenance effort for a single engineer.

## Considered options

1. **JPA annotations on the aggregate**: The `Document` aggregate plays the role of a JPA entity.
2. **Separate JPA entity + mapper**: `DocumentJpaEntity` lives in the
   `adapter/out/persistence` package and is mapped to the domain model with `DocumentMapper`.

## Decision outcome

Chosen option: "Separate JPA entity + mapper", because the domain model is free of
persistence-framework dependencies and can evolve independently from the database schema.
It also keeps the aggregate's invariants protected.
It does not satisfy low implementation effort for a single engineer; the trade-off is accepted and mitigated (see
Consequences).

## Consequences

### Positive

- The domain unit tests run very fast because they have no Spring or database dependencies.
- When adding a new column to the database schema using Flyway, only `adapter/out/persistence` classes need to be
  updated. The domain model remains unchanged.

### Negative / trade-offs

- The mapper introduces a small amount of duplication and requires some maintenance; accepted because of the narrow
  mapping scope: only a few fields are mapped for a single aggregate. Because the mapping logic is simple and the
  aggregate has a private constructor and no setters, introducing a mapping framework such as MapStruct is
  not needed.
- The mapper can become outdated when the domain model changes. For example, if a new field is added to the domain
  model, no compilation error is raised when the mapper is not updated accordingly; mitigated by implementing a
  round-trip test that verifies all relevant fields are mapped correctly.
- Recreating a `Document` from the database must not use the `upload()` factory method, because `upload()` generates a
  new aggregate instance with a new `DocumentId` in the `UPLOADED` state. Persistence restoration has a different
  purpose: rebuilding an existing aggregate from stored data. For this reason, the domain model provides a dedicated
  static factory method, `restore()`, used only when reconstructing a persisted `Document`. The risk of this method is
  that when invoked outside the persistence adapter it bypasses the transition rules and can lead to
  inconsistencies; mitigated by a Javadoc note that the method is used only for reconstitution from DB and an ArchUnit
  rule to ensure that the method is only called from within the `adapter/out/persistence` package.

## Pros and cons of the options

### JPA annotations on the aggregate

- ❌ Bad, because the domain model depends on the persistence framework: `jakarta.persistence`.
- ❌ Bad, because it violates the aggregate's invariant rules. The JPA entity is not created through the `upload()`
  factory method but through a no-args constructor.
- ❌ Bad, because renaming a column requires changing `Document`.
- ✅ Good, because it is the simplest option and it is easy to implement for a single developer.

### Separate JPA entity + mapper

- ✅ Good, because the domain model is free of persistence framework dependencies: no `jakarta.persistence` imports.
- ✅ Good, because the `Document` aggregate class is not created through reflection (JPA entity) but using the `upload()`
  or `restore()` factory methods.
- ✅ Good, because renaming a column does not require changing `Document`.
- ❌ Bad, because it does not satisfy low implementation effort and an extra entity class and a mapper are required.
