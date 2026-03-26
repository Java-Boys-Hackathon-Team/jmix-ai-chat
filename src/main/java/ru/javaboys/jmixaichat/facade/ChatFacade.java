package ru.javaboys.jmixaichat.facade;

import io.jmix.core.DataManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import ru.javaboys.jmixaichat.entity.Conversation;
import ru.javaboys.jmixaichat.service.AiAssistantService;

import java.sql.Date;
import java.time.Instant;
import java.util.List;

@Service
public class ChatFacade {

    private final DataManager dataManager;
    private final AiAssistantService aiAssistantService;

    public ChatFacade(DataManager dataManager, AiAssistantService aiAssistantService) {
        this.dataManager = dataManager;
        this.aiAssistantService = aiAssistantService;
    }

    // 1. Создание conversation
    public Conversation createConversation(String title) {
        Conversation conversation = dataManager.create(Conversation.class);
        conversation.setTitle(title != null ? title : "Новый чат");
        conversation.setCreatedAt(Date.from(Instant.now()));
        return dataManager.save(conversation);
    }

    // 2. Запрос в LLM
    public ChatResponse chat(String message, Conversation conversation) {
        if (conversation == null) {
            conversation = createConversation(null);
        }

        Conversation finalConversation = conversation;

        Flux<String> response =  aiAssistantService.chat(message, finalConversation.getId())
                                 .doOnSubscribe(subscription -> {
                                     if (finalConversation.getTitle() == null
                                             || "Новый чат".equals(finalConversation.getTitle())) {
                                         String title = generateTitle(message);
                                         finalConversation.setTitle(title);
                                         dataManager.save(finalConversation);
                                     }
                                 });
        return new ChatResponse(response, conversation);
    }

    // 3. Получить все Conversation
    public List<Conversation> getAllConversations() {
        return dataManager.load(Conversation.class).all().list();
    }

    private String generateTitle(String message) {
        return message.length() > 40
                ? message.substring(0, 40) + "..."
                : message;
    }

    public static record ChatResponse(
            Flux<String> response,
            Conversation conversation
    ) {
    }
}
