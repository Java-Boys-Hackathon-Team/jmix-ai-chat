package ru.javaboys.jmixaichat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.javaboys.jmixaichat.repository.EmbeddingRepository;
import ru.javaboys.jmixaichat.repository.EmbeddingRepository.EmbeddingMatch;
import ru.javaboys.jmixaichat.repository.EmbeddingRepository.StoredEmbedding;

import java.util.List;

@Service
public class VectorStoreService {

    static final String COLLECTION = "embeddings";

    private final EmbeddingService embeddingService;
    private final EmbeddingRepository embeddingRepository;
    private final int expectedDimensions;

    public VectorStoreService(
            EmbeddingService embeddingService,
            EmbeddingRepository embeddingRepository,
            @Value("${embedding.store.dimensions}") int expectedDimensions) {
        this.embeddingService = embeddingService;
        this.embeddingRepository = embeddingRepository;
        this.expectedDimensions = expectedDimensions;
    }

    public StoreResult store(String model, int dimensions, List<VectorDocument> vectors) {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Не указана embedding-модель");
        }
        if (model.length() > 255) {
            throw new IllegalArgumentException("Название embedding-модели слишком длинное");
        }
        if (dimensions != expectedDimensions) {
            throw new IllegalArgumentException("Ожидался embedding размерности " + expectedDimensions);
        }
        if (vectors == null || vectors.size() != 3) {
            throw new IllegalArgumentException("Для демонстрации нужны ровно три вектора");
        }
        List<StoredEmbedding> documents = vectors.stream()
                .map(this::validateAndMap)
                .toList();
        int stored = embeddingRepository.replaceCollection(COLLECTION, model, documents);
        return new StoreResult(COLLECTION, stored, dimensions, model);
    }

    public SearchResult search(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Введите поисковый запрос");
        }
        if (query.length() > 1000) {
            throw new IllegalArgumentException("Поисковый запрос не должен быть длиннее 1000 символов");
        }
        if (limit < 1 || limit > 10) {
            throw new IllegalArgumentException("Количество результатов должно быть от 1 до 10");
        }
        EmbeddingService.QueryEmbedding queryEmbedding = embeddingService.embedQuery(query.trim());
        validateDimensions(queryEmbedding.values());
        List<EmbeddingMatch> matches = embeddingRepository.findNearest(
                COLLECTION,
                queryEmbedding.model(),
                queryEmbedding.values(),
                limit);
        return new SearchResult(query.trim(), queryEmbedding.model(), matches);
    }

    private StoredEmbedding validateAndMap(VectorDocument vector) {
        if (vector == null || vector.label() == null || vector.label().isBlank()) {
            throw new IllegalArgumentException("У каждого вектора должна быть метка");
        }
        if (vector.label().length() > 20) {
            throw new IllegalArgumentException("Метка вектора не должна быть длиннее 20 символов");
        }
        if (vector.content() == null || vector.content().isBlank()) {
            throw new IllegalArgumentException("Текст вектора не должен быть пустым");
        }
        if (vector.content().length() > 1000) {
            throw new IllegalArgumentException("Текст вектора не должен быть длиннее 1000 символов");
        }
        validateDimensions(vector.values());
        return new StoredEmbedding(vector.label().trim(), vector.content().trim(), vector.values());
    }

    private void validateDimensions(float[] vector) {
        if (vector == null || vector.length != expectedDimensions) {
            throw new IllegalArgumentException("Ожидался embedding размерности " + expectedDimensions);
        }
    }

    public record VectorDocument(String label, String content, float[] values) {
    }

    public record StoreResult(String collection, int stored, int dimensions, String model) {
    }

    public record SearchResult(String query, String model, List<EmbeddingMatch> matches) {
    }
}
