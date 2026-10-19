package ru.javaboys.jmixaichat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.javaboys.jmixaichat.service.EmbeddingService;
import ru.javaboys.jmixaichat.service.EmbeddingService.EmbeddingAnalysis;

import java.util.List;

@RestController
@RequestMapping("/public/api/embeddings")
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping
    public EmbeddingAnalysis embed(@RequestBody EmbeddingRequest request) {
        return embeddingService.embed(request.texts());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Некорректные фразы");
        return problem;
    }

    public record EmbeddingRequest(List<String> texts) {
    }
}
