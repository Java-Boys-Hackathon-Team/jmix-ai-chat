package ru.javaboys.jmixaichat.view.chat;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
    private VerticalLayout conversationListPanel;

    @Subscribe
    public void onInit(InitEvent event) {
        // Sidebar - conversations
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setWidth("280px");
        sidebar.getStyle().set("border-right", "1px solid var(--lumo-contrast-10pct)");

        Button newChatBtn = new Button("+ New Chat", e -> startNewChat());
        newChatBtn.setWidthFull();
        newChatBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        conversationListPanel = new VerticalLayout();
        conversationListPanel.setPadding(false);
        conversationListPanel.setSpacing(false);

        sidebar.add(newChatBtn, conversationListPanel);

        // Chat panel
        VerticalLayout chatPanel = new VerticalLayout();
        chatPanel.setSizeFull();
        chatPanel.setPadding(false);

        messageList = new MessageList();
        messageList.setWidthFull();
        messageList.getStyle().set("flex-grow", "1");

        MessageInput messageInput = new MessageInput();
        messageInput.setWidthFull();
        messageInput.addSubmitListener(this::onSendMessage);

        chatPanel.add(messageList, messageInput);

        // Main layout
        HorizontalLayout mainLayout = new HorizontalLayout(sidebar, chatPanel);
        mainLayout.setSizeFull();
        mainLayout.setSpacing(false);

        chatBox.add(mainLayout);

        loadConversations();
    }

    private void loadConversations() {
        conversationListPanel.removeAll();
        for (Conversation conv : chatFacade.getAllConversations()) {
            Button btn = new Button(conv.getTitle(), e -> selectConversation(conv));
            btn.setWidthFull();
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btn.getStyle().set("justify-content", "flex-start");
            conversationListPanel.add(btn);
        }
    }

    private void selectConversation(Conversation conv) {
        this.conversation = conv;
        this.messages.clear();
        messageList.setItems(messages);
    }

    private void startNewChat() {
        this.conversation = null;
        this.messages.clear();
        messageList.setItems(messages);
    }

    private void onSendMessage(MessageInput.SubmitEvent event) {
        String userMessage = event.getValue();

        messages.add(new MessageListItem(userMessage, Instant.now(), "You"));
        messageList.setItems(messages);

        ChatFacade.ChatResponse chatResponse = chatFacade.chat(userMessage, conversation);
        conversation = chatResponse.conversation();

        UI ui = UI.getCurrent();
        MessageListItem aiMessage = new MessageListItem("", Instant.now(), "AI");
        messages.add(aiMessage);
        StringBuilder sb = new StringBuilder();

        chatResponse.response()
                .doOnNext(chunk -> {
                    sb.append(chunk);
                    ui.access(() -> {
                        aiMessage.setText(sb.toString());
                        messageList.setItems(messages);
                    });
                })
                .doOnError(error -> ui.access(() -> {
                    aiMessage.setText("Error: " + error.getMessage());
                    messageList.setItems(messages);
                }))
                .doOnComplete(() -> ui.access(this::loadConversations))
                .subscribe();
    }
}
