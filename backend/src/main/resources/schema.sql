CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS product_embeddings (
    product_id BIGINT PRIMARY KEY,
    embedding vector(128) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_product_embeddings_vector
    ON product_embeddings USING hnsw (embedding vector_cosine_ops);

ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_version BIGINT NOT NULL DEFAULT 0;

