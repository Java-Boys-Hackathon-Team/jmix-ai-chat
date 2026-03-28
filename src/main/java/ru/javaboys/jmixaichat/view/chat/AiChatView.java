package ru.javaboys.jmixaichat.view.chat;

import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.javaboys.jmixaichat.entity.Conversation;
import ru.javaboys.jmixaichat.facade.ChatFacade;
import ru.javaboys.jmixaichat.view.main.MainView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "ai-chat", layout = MainView.class)
@ViewController(id = "AiChatView")
@ViewDescriptor(path = "ai-chat-view.xml")
public class AiChatView extends StandardView {

    @ViewComponent
    private VerticalLayout chatBox;

    @Autowired
    private ChatFacade chatFacade;

    private Conversation conversation;
    private final List<MessageListItem> messages = new ArrayList<>();
    private MessageList messageList;

    @Subscribe
    public void onInit(InitEvent event) {
        messageList = new MessageList();
        messageList.setWidthFull();
        messageList.getStyle().set("flex-grow", "1");

        MessageInput messageInput = new MessageInput();
        messageInput.setWidthFull();
        messageInput.addSubmitListener(this::onSendMessage);

        chatBox.add(messageList, messageInput);
    }

    private void onSendMessage(MessageInput.SubmitEvent event) {
        String userMessage = event.getValue();

        messages.add(new MessageListItem(userMessage, Instant.now(), "You"));
        messageList.setItems(messages);

        ChatFacade.ChatResponse chatResponse = chatFacade.chat(userMessage, conversation);
        conversation = chatResponse.conversation();

        // Blocking call - collect all chunks and join
        String aiResponse = chatResponse.response()
                .collectList()
                .block()
                .stream()
                .collect(Collectors.joining());

        messages.add(new MessageListItem(aiResponse, Instant.now(), "AI"));
        messageList.setItems(messages);
    }
}
