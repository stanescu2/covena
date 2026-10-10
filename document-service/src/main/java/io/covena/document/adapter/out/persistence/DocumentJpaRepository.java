package io.covena.document.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface DocumentJpaRepository extends JpaRepository<DocumentJpaEntity, UUID> {
}
