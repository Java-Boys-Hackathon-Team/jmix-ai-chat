package ru.javaboys.jmixaichat.facade;

import io.jmix.core.security.SystemAuthenticator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.javaboys.jmixaichat.entity.Conversation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ChatFacadeTest {

    @Autowired
    private ChatFacade chatFacade;

    @Autowired
    private SystemAuthenticator systemAuthenticator;

    @Test
    void chatShouldReturnStreamingResponseAndCreateConversation() {
        systemAuthenticator.withSystem(() -> {
            String message = "Привет! Как дела?";

            ChatFacade.ChatResponse chatResponse = chatFacade.chat(message, null);
            String result = chatResponse.response().collectList().map(list -> String.join("", list)).block();

            assertNotNull(result);
            assertFalse(result.isBlank());

            System.out.println("LLM response: " + result);
            return null;
        });
    }

    @Test
    void shouldRememberContext() {
        systemAuthenticator.withSystem(() -> {
            String firstMessage = "Привет! Меня Зовут Рустам!";
            String secondMessage = "Как меня зовут?";

            // первый запрос
            ChatFacade.ChatResponse firstResponse = chatFacade.chat(firstMessage, null);
            firstResponse.response().collectList().block();

            Conversation conversation = firstResponse.conversation();

            // второй запрос
            ChatFacade.ChatResponse secondResponse = chatFacade.chat(secondMessage, conversation);
            String result = secondResponse.response()
                                          .collectList()
                                          .map(list -> String.join("", list))
                                          .block();

            // проверка
            assertNotNull(result);
            assertTrue(result.contains("Рустам"));

            return null;
        });
    }
}