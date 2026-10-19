package ru.javaboys.jmixaichat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.javaboys.jmixaichat.service.VectorStoreService;
import ru.javaboys.jmixaichat.service.VectorStoreService.SearchResult;
import ru.javaboys.jmixaichat.service.VectorStoreService.StoreResult;
import ru.javaboys.jmixaichat.service.VectorStoreService.VectorDocument;

import java.util.List;

@RestController
@RequestMapping("/public/api/vector-store")
public class VectorStoreController {

    private final VectorStoreService vectorStoreService;

    public VectorStoreController(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    @PostMapping("/documents")
    public StoreResult store(@RequestBody StoreRequest request) {
        return vectorStoreService.store(request.model(), request.dimensions(), request.vectors());
    }

    @PostMapping("/search")
    public SearchResult search(@RequestBody SearchRequest request) {
        return vectorStoreService.search(request.query(), request.limit());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Некорректный запрос к векторному хранилищу");
        return problem;
    }

    public record StoreRequest(String model, int dimensions, List<VectorDocument> vectors) {
    }

    public record SearchRequest(String query, int limit) {
    }
}
