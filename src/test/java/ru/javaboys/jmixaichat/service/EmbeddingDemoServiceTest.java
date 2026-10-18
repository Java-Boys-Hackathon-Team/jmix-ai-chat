package ru.javaboys.jmixaichat.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmbeddingDemoServiceTest {

    @Test
    void calculatesVectorMetrics() {
        float[] first = {1.0f, 0.0f};
        float[] sameDirection = {2.0f, 0.0f};
        float[] orthogonal = {0.0f, 1.0f};

        assertEquals(1.0, EmbeddingDemoService.cosineSimilarity(first, sameDirection), 0.000001);
        assertEquals(0.0, EmbeddingDemoService.cosineSimilarity(first, orthogonal), 0.000001);
        assertEquals(2.0, EmbeddingDemoService.dotProduct(first, sameDirection), 0.000001);
        assertEquals(Math.sqrt(2.0), EmbeddingDemoService.euclideanDistance(first, orthogonal), 0.000001);
        assertEquals(2.0, EmbeddingDemoService.manhattanDistance(first, orthogonal), 0.000001);
    }

    @Test
    void rejectsVectorsWithDifferentDimensions() {
        assertThrows(IllegalArgumentException.class,
                () -> EmbeddingDemoService.cosineSimilarity(new float[]{1.0f}, new float[]{1.0f, 2.0f}));
    }

    @Test
    void embedsBatchResponseAndThenRanksPairsRelatively() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        List<String> texts = List.of("A text", "B text", "C text");
        EmbeddingResponse response = new EmbeddingResponse(
                List.of(
                        new Embedding(new float[]{1.0f, 0.0f}, 0),
                        new Embedding(new float[]{0.9f, 0.1f}, 1),
                        new Embedding(new float[]{0.0f, 1.0f}, 2)),
                new EmbeddingResponseMetadata("provider/model", new DefaultUsage(9, 0)));
        when(model.embedForResponse(texts)).thenReturn(response);

        EmbeddingDemoService service = new EmbeddingDemoService(model, "configured/model");
        EmbeddingDemoService.EmbeddingAnalysis analysis = service.embed(texts);
        List<EmbeddingDemoService.Comparison> comparisons = service.compare(analysis.vectors());

        assertEquals("provider/model", analysis.model());
        assertEquals(2, analysis.dimensions());
        assertEquals(9, analysis.promptTokens());
        assertEquals("наиболее близкая пара", comparisons.get(0).description());
        assertEquals("наименее близкая пара", comparisons.get(1).description());
        assertEquals("средняя среди выбранных", comparisons.get(2).description());
    }
}
