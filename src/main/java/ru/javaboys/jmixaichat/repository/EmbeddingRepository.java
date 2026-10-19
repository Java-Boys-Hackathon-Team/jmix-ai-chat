package ru.javaboys.jmixaichat.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class EmbeddingRepository {

    private static final String INSERT_SQL = """
            INSERT INTO embedding_document
                (id, collection, label, content, embedding, model, created_at)
            VALUES (?, ?, ?, ?, CAST(? AS vector), ?, ?)
            """;

    private static final String SEARCH_SQL = """
            WITH query_vector AS (SELECT CAST(? AS vector) AS embedding)
            SELECT document.label,
                   document.content,
                   document.embedding <=> query_vector.embedding AS distance
              FROM embedding_document document
              CROSS JOIN query_vector
             WHERE document.collection = ?
               AND document.model = ?
             ORDER BY distance
             LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public EmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public int replaceCollection(String collection, String model, List<StoredEmbedding> documents) {
        jdbcTemplate.update("DELETE FROM embedding_document WHERE collection = ?", collection);
        LocalDateTime createdAt = LocalDateTime.now();
        jdbcTemplate.batchUpdate(INSERT_SQL, documents, documents.size(), (statement, document) -> {
            statement.setObject(1, UUID.randomUUID());
            statement.setString(2, collection);
            statement.setString(3, document.label());
            statement.setString(4, document.content());
            statement.setString(5, vectorLiteral(document.embedding()));
            statement.setString(6, model);
            statement.setTimestamp(7, Timestamp.valueOf(createdAt));
        });
        return documents.size();
    }

    public List<EmbeddingMatch> findNearest(
            String collection,
            String model,
            float[] queryEmbedding,
            int limit) {
        return jdbcTemplate.query(
                SEARCH_SQL,
                (resultSet, rowNumber) -> {
                    double distance = resultSet.getDouble("distance");
                    return new EmbeddingMatch(
                            resultSet.getString("label"),
                            resultSet.getString("content"),
                            distance,
                            1.0 - distance);
                },
                vectorLiteral(queryEmbedding),
                collection,
                model,
                limit);
    }

    static String vectorLiteral(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException("Embedding не должен быть пустым");
        }
        StringBuilder result = new StringBuilder(vector.length * 12).append('[');
        for (int index = 0; index < vector.length; index++) {
            float value = vector[index];
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Embedding содержит нечисловую координату");
            }
            if (index > 0) {
                result.append(',');
            }
            result.append(value);
        }
        return result.append(']').toString();
    }

    public record StoredEmbedding(String label, String content, float[] embedding) {
    }

    public record EmbeddingMatch(String label, String content, double distance, double similarity) {
    }
}
