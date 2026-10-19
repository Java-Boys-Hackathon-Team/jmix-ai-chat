package ru.javaboys.jmixaichat.service;

import org.junit.jupiter.api.Test;
import ru.javaboys.jmixaichat.repository.EmbeddingRepository;
import ru.javaboys.jmixaichat.repository.EmbeddingRepository.EmbeddingMatch;
import ru.javaboys.jmixaichat.service.VectorStoreService.VectorDocument;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VectorStoreServiceTest {

    @Test
    void storesExactlyTheCalculatedVectors() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        EmbeddingRepository repository = mock(EmbeddingRepository.class);
        VectorStoreService service = new VectorStoreService(embeddingService, repository, 3);
        List<VectorDocument> vectors = List.of(
                new VectorDocument("A", "first", new float[]{1, 0, 0}),
                new VectorDocument("B", "second", new float[]{0, 1, 0}),
                new VectorDocument("C", "third", new float[]{0, 0, 1}));
        when(repository.replaceCollection(eq("embeddings"), eq("model"), anyList())).thenReturn(3);

        VectorStoreService.StoreResult result = service.store("model", 3, vectors);

        assertEquals(3, result.stored());
        verify(repository).replaceCollection(
                eq("embeddings"),
                eq("model"),
                argThat(documents -> documents.size() == 3 && "second".equals(documents.get(1).content())));
    }

    @Test
    void embedsQueryAndDelegatesCosineSearchToRepository() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        EmbeddingRepository repository = mock(EmbeddingRepository.class);
        VectorStoreService service = new VectorStoreService(embeddingService, repository, 3);
        float[] queryVector = {0.8f, 0.2f, 0.0f};
        when(embeddingService.embedQuery("vacation"))
                .thenReturn(new EmbeddingService.QueryEmbedding("model", queryVector));
        when(repository.findNearest("embeddings", "model", queryVector, 3))
                .thenReturn(List.of(new EmbeddingMatch("A", "first", 0.1, 0.9)));

        VectorStoreService.SearchResult result = service.search("vacation", 3);

        assertEquals(1, result.matches().size());
        assertEquals(0.9, result.matches().getFirst().similarity(), 0.000001);
    }

    @Test
    void rejectsVectorsWithUnexpectedDimensions() {
        VectorStoreService service = new VectorStoreService(
                mock(EmbeddingService.class),
                mock(EmbeddingRepository.class),
                3);

        assertThrows(IllegalArgumentException.class, () -> service.store(
                "model",
                2,
                List.of(
                        new VectorDocument("A", "first", new float[]{1, 0}),
                        new VectorDocument("B", "second", new float[]{0, 1}),
                        new VectorDocument("C", "third", new float[]{1, 1}))));
    }
}
