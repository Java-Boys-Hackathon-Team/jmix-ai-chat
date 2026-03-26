package ru.javaboys.jmixaichat.service;

import io.jmix.core.security.SystemAuthenticator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AiAssistantServiceTest {

    @Autowired
    private AiAssistantService aiAssistantService;

    @Autowired
    private SystemAuthenticator systemAuthenticator;

    @Test
    void testReturnResponseFromLlmSuccess() {

        systemAuthenticator.withSystem(() -> {

            String conversationId = UUID.randomUUID().toString();

            String result = aiAssistantService
                    .chat("Привет! Ответь одним словом: ок", UUID.fromString(conversationId))
                    .collectList()
                    .map(list -> String.join("", list))
                    .block();

            assertNotNull(result);
            assertFalse(result.isBlank());

            System.out.println("LLM response: " + result);

            return null;
        });
    }
}