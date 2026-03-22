package ru.javaboys.jmixaichat.service;

import io.jmix.core.DataManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import ru.javaboys.jmixaichat.entity.ChatMessage;
import ru.javaboys.jmixaichat.entity.Conversation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiAssistantService {
    private static final String SYSTEM_PROMPT = "Ты отзывчивый, понимающий чат-помощник";

    private final ChatClient chatClient;
    private final DataManager dataManager;

    public AiAssistantService(ChatClient chatClient, DataManager dataManager) {
        this.chatClient = chatClient;
        this.dataManager = dataManager;
    }

    public Flux<String> chat(String userMessage, Conversation conversation) {
        List<ChatMessage> history = dataManager.load(ChatMessage.class)
                                               .query("select m from ChatMessage m where m.conversation = :conversation order by m.createdAt")
                                               .parameter("conversation", conversation).list();

        List<Message> messages = new ArrayList<>();

        messages.add(new SystemMessage(SYSTEM_PROMPT));
        history.forEach(msg -> {
            if ("user".equalsIgnoreCase(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equalsIgnoreCase(msg.getContent())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        });
        messages.add(new UserMessage(userMessage));

        Prompt prompt = new Prompt(messages);

        return chatClient.prompt(prompt)
                         .stream()
                         .content();
    }
}
