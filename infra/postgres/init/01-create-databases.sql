-- Câte o bază de date pentru fiecare serviciu (database per service)
CREATE DATABASE documents;
CREATE DATABASE knowledge;
CREATE DATABASE chat;
CREATE DATABASE keycloak;

-- pgvector se activează per bază; doar knowledge-service lucrează cu vectori
\connect knowledge
CREATE EXTENSION IF NOT EXISTS vector;