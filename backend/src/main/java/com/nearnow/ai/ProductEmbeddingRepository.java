package com.nearnow.ai;

import com.pgvector.PGvector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductEmbeddingRepository {
    private final JdbcTemplate jdbcTemplate;

    public ProductEmbeddingRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public void upsert(Long productId, float[] embedding) {
        jdbcTemplate.update(
                "INSERT INTO product_embeddings (product_id, embedding) VALUES (?, ?) " +
                        "ON CONFLICT (product_id) DO UPDATE SET embedding = EXCLUDED.embedding",
                productId, new PGvector(embedding));
    }

    public List<Long> findNearestProductIds(float[] queryEmbedding, int limit) {
        return jdbcTemplate.query(
                "SELECT pe.product_id FROM product_embeddings pe " +
                        "JOIN products p ON p.id = pe.product_id " +
                        "WHERE p.active = true ORDER BY pe.embedding <=> ? LIMIT ?",
                (rs, rowNum) -> rs.getLong("product_id"),
                new PGvector(queryEmbedding), limit);
    }
}
