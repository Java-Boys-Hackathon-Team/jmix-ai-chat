package ru.javaboys.jmixaichat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
public class AiAssistantService {

    private final ChatClient chatClient;

    public AiAssistantService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Flux<String> chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .stream()
                .content();
    }
}
