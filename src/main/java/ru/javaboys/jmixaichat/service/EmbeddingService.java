package ru.javaboys.jmixaichat.service;

import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final String configuredModel;

    public EmbeddingService(
            EmbeddingModel embeddingModel,
            @Value("${spring.ai.openai.embedding.options.model}") String configuredModel) {
        this.embeddingModel = embeddingModel;
        this.configuredModel = configuredModel;
    }

    public EmbeddingAnalysis embed(List<String> texts) {
        if (texts == null || texts.size() != 3) {
            throw new IllegalArgumentException("Для демонстрации нужны ровно три предложения");
        }
        if (texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException("Заполните все три предложения");
        }

        EmbeddingResponse response = embeddingModel.embedForResponse(texts);
        List<float[]> embeddings = response.getResults().stream()
                .map(Embedding::getOutput)
                .toList();

        if (embeddings.size() != texts.size()) {
            throw new IllegalStateException("Embedding-модель вернула неожиданное количество векторов");
        }

        int dimensions = embeddings.getFirst().length;
        if (dimensions == 0 || embeddings.stream().anyMatch(vector -> vector.length != dimensions)) {
            throw new IllegalStateException("Получены векторы разной размерности");
        }

        List<VectorInfo> vectors = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            float[] vector = embeddings.get(i);
            vectors.add(new VectorInfo(
                    String.valueOf((char) ('A' + i)),
                    texts.get(i),
                    vector,
                    norm(vector),
                    min(vector),
                    max(vector)));
        }

        String responseModel = response.getMetadata().getModel();
        String model = responseModel == null || responseModel.isBlank() ? configuredModel : responseModel;

        return new EmbeddingAnalysis(
                model,
                dimensions,
                response.getMetadata().getUsage().getPromptTokens(),
                vectors);
    }

    public QueryEmbedding embedQuery(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Введите поисковый запрос");
        }
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        if (response.getResults().size() != 1) {
            throw new IllegalStateException("Embedding-модель вернула неожиданный ответ для запроса");
        }
        float[] embedding = response.getResults().getFirst().getOutput();
        if (embedding.length == 0) {
            throw new IllegalStateException("Embedding-модель вернула пустой вектор");
        }
        String responseModel = response.getMetadata().getModel();
        String model = responseModel == null || responseModel.isBlank() ? configuredModel : responseModel;
        return new QueryEmbedding(model, embedding);
    }

    public List<Comparison> compare(List<VectorInfo> vectors) {
        if (vectors == null || vectors.size() != 3) {
            throw new IllegalArgumentException("Для сравнения нужны ровно три вектора");
        }
        List<float[]> values = vectors.stream().map(VectorInfo::values).toList();
        return rankComparisons(List.of(
                comparePair(0, 1, values),
                comparePair(0, 2, values),
                comparePair(1, 2, values)));
    }

    private static Comparison comparePair(int left, int right, List<float[]> vectors) {
        float[] first = vectors.get(left);
        float[] second = vectors.get(right);
        double cosine = cosineSimilarity(first, second);
        return new Comparison(
                String.valueOf((char) ('A' + left)),
                String.valueOf((char) ('A' + right)),
                cosine,
                dotProduct(first, second),
                euclideanDistance(first, second),
                manhattanDistance(first, second),
                "");
    }

    private static List<Comparison> rankComparisons(List<Comparison> comparisons) {
        double highest = comparisons.stream().mapToDouble(Comparison::cosine).max().orElseThrow();
        double lowest = comparisons.stream().mapToDouble(Comparison::cosine).min().orElseThrow();
        if (Math.abs(highest - lowest) < 0.000001) {
            return comparisons.stream()
                    .map(comparison -> withDescription(comparison, "одинаковая близость"))
                    .toList();
        }
        return comparisons.stream()
                .map(comparison -> {
                    if (Math.abs(comparison.cosine() - highest) < 0.000001) {
                        return withDescription(comparison, "наиболее близкая пара");
                    }
                    if (Math.abs(comparison.cosine() - lowest) < 0.000001) {
                        return withDescription(comparison, "наименее близкая пара");
                    }
                    return withDescription(comparison, "средняя среди выбранных");
                })
                .toList();
    }

    private static Comparison withDescription(Comparison comparison, String description) {
        return new Comparison(
                comparison.left(),
                comparison.right(),
                comparison.cosine(),
                comparison.dotProduct(),
                comparison.euclideanDistance(),
                comparison.manhattanDistance(),
                description);
    }

    static double cosineSimilarity(float[] first, float[] second) {
        validateSameDimensions(first, second);
        double denominator = norm(first) * norm(second);
        return denominator == 0.0 ? 0.0 : dotProduct(first, second) / denominator;
    }

    static double dotProduct(float[] first, float[] second) {
        validateSameDimensions(first, second);
        double result = 0.0;
        for (int i = 0; i < first.length; i++) {
            result += first[i] * second[i];
        }
        return result;
    }

    static double euclideanDistance(float[] first, float[] second) {
        validateSameDimensions(first, second);
        double sum = 0.0;
        for (int i = 0; i < first.length; i++) {
            double difference = first[i] - second[i];
            sum += difference * difference;
        }
        return Math.sqrt(sum);
    }

    static double manhattanDistance(float[] first, float[] second) {
        validateSameDimensions(first, second);
        double sum = 0.0;
        for (int i = 0; i < first.length; i++) {
            sum += Math.abs(first[i] - second[i]);
        }
        return sum;
    }

    private static double norm(float[] vector) {
        double sum = 0.0;
        for (float value : vector) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    private static float min(float[] vector) {
        float result = Float.POSITIVE_INFINITY;
        for (float value : vector) {
            result = Math.min(result, value);
        }
        return result;
    }

    private static float max(float[] vector) {
        float result = Float.NEGATIVE_INFINITY;
        for (float value : vector) {
            result = Math.max(result, value);
        }
        return result;
    }

    private static void validateSameDimensions(float[] first, float[] second) {
        if (first == null || second == null || first.length != second.length) {
            throw new IllegalArgumentException("Векторы должны иметь одинаковую размерность");
        }
    }

    public record EmbeddingAnalysis(
            String model,
            int dimensions,
            Integer promptTokens,
            List<VectorInfo> vectors) {
    }

    public record VectorInfo(
            String label,
            String text,
            float[] values,
            double norm,
            float min,
            float max) {
    }

    public record Comparison(
            String left,
            String right,
            double cosine,
            double dotProduct,
            double euclideanDistance,
            double manhattanDistance,
            String description) {
    }

    public record QueryEmbedding(String model, float[] values) {
    }
}
