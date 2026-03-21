package ru.javaboys.jmixaichat.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiAssistantServiceTest {

    @Autowired
    private AiAssistantService aiAssistantService;

    @Test
    void testReturnResponseFromLlmSuccess() {
        String result = aiAssistantService.chat("Привет! дай пример unit теста для языка java")
                .collectList()
                .map(list -> String.join("", list))
                .block();

        assertNotNull(result);
        assertFalse(result.isBlank());

        System.out.println(result);
    }
}