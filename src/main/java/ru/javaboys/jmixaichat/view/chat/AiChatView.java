package ru.javaboys.jmixaichat.view.chat;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import ru.javaboys.jmixaichat.entity.ChatMessage;
import ru.javaboys.jmixaichat.entity.Conversation;
import ru.javaboys.jmixaichat.service.AiAssistantService;
import ru.javaboys.jmixaichat.view.main.MainView;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Route(value = "chat", layout = MainView.class)
@ViewController("AiChatView")
@ViewDescriptor(path = "ai-chat-view.xml")
public class AiChatView extends StandardView {

    @Autowired
    private AiAssistantService aiAssistantService;
    @Autowired
    private DataManager dataManager;

    private ListBox<Conversation> conversationList;
    private MessageList messageList;
    private MessageInput messageInput;
    private Conversation currentConversation;
    private final List<MessageListItem> displayedItems = new ArrayList<>();
    private boolean ignoreSelection = false;

    @Subscribe
    public void onInit(InitEvent event) {
        var content = getContent();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);

        // Left sidebar - conversations
        var sidebar = new VerticalLayout();
        sidebar.setWidth("280px");
        sidebar.setHeightFull();
        sidebar.setPadding(true);
        sidebar.setSpacing(true);
        sidebar.getStyle().set("border-right", "1px solid var(--lumo-contrast-10pct)");

        var newChatBtn = new Button("+ New Chat", e -> createNewConversation());
        newChatBtn.setWidthFull();
        newChatBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        conversationList = new ListBox<>();
        conversationList.setWidthFull();
        conversationList.setHeightFull();
        conversationList.setItemLabelGenerator(Conversation::getTitle);
        conversationList.addValueChangeListener(e -> {
            if (!ignoreSelection && e.getValue() != null) {
                selectConversation(e.getValue());
            }
        });

        sidebar.add(newChatBtn, conversationList);
        sidebar.expand(conversationList);

        // Right side - chat
        var chatArea = new VerticalLayout();
        chatArea.setSizeFull();
        chatArea.setPadding(false);
        chatArea.setSpacing(false);

        messageList = new MessageList();
        messageList.setSizeFull();
        messageList.setMarkdown(true);

        messageInput = new MessageInput();
        messageInput.setWidthFull();
        messageInput.addSubmitListener(this::onSubmit);

        chatArea.add(messageList, messageInput);
        chatArea.expand(messageList);

        // Split layout
        var splitLayout = new HorizontalLayout(sidebar, chatArea);
        splitLayout.setSizeFull();
        splitLayout.setSpacing(false);
        splitLayout.expand(chatArea);

        content.add(splitLayout);
        content.expand(splitLayout);

        loadConversations();
    }

    private void loadConversations() {
        List<Conversation> conversations = dataManager.load(Conversation.class)
                .query("select c from Conversation c order by c.createdAt desc")
                .list();
        conversationList.setItems(conversations);

        if (!conversations.isEmpty()) {
            conversationList.setValue(conversations.getFirst());
        }
    }

    private void refreshConversationList() {
        ignoreSelection = true;
        try {
            List<Conversation> conversations = dataManager.load(Conversation.class)
                    .query("select c from Conversation c order by c.createdAt desc")
                    .list();
            conversationList.setItems(conversations);
            conversations.stream()
                    .filter(c -> c.getId().equals(currentConversation.getId()))
                    .findFirst()
                    .ifPresent(c -> {
                        currentConversation = c;
                        conversationList.setValue(c);
                    });
        } finally {
            ignoreSelection = false;
        }
    }

    private void createNewConversation() {
        var conversation = dataManager.create(Conversation.class);
        conversation.setTitle("New Chat");
        conversation.setCreatedAt(LocalDateTime.now());
        conversation = dataManager.save(conversation);

        currentConversation = conversation;
        displayedItems.clear();
        messageList.setItems(displayedItems);

        refreshConversationList();
    }

    private void selectConversation(Conversation conversation) {
        currentConversation = conversation;
        displayedItems.clear();

        List<ChatMessage> messages = dataManager.load(ChatMessage.class)
                .query("select m from ChatMessage m where m.conversation = :conv order by m.createdAt")
                .parameter("conv", conversation)
                .list();

        for (ChatMessage msg : messages) {
            boolean isUser = "user".equals(msg.getRole());
            var item = new MessageListItem(
                    msg.getContent(),
                    msg.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                    isUser ? "You" : "AI Assistant"
            );
            item.setUserColorIndex(isUser ? 0 : 2);
            displayedItems.add(item);
        }
        messageList.setItems(displayedItems);
    }

    private void onSubmit(MessageInput.SubmitEvent event) {
        if (currentConversation == null) {
            createNewConversation();
        }

        String text = event.getValue();
        boolean isFirstMessage = displayedItems.isEmpty();

        // Display immediately
        var userMsg = new MessageListItem(text, Instant.now(), "You");
        userMsg.setUserColorIndex(0);
        displayedItems.add(userMsg);

        var botMsg = new MessageListItem("", Instant.now(), "AI Assistant");
        botMsg.setUserColorIndex(2);
        displayedItems.add(botMsg);
        messageList.setItems(displayedItems);

        messageInput.setEnabled(false);

        // Save user message to DB
        saveMessage("user", text);

        // Update conversation title from first message
        if (isFirstMessage) {
            currentConversation.setTitle(text.length() > 40 ? text.substring(0, 40) + "..." : text);
            currentConversation = dataManager.save(currentConversation);
            refreshConversationList();
        }

        // Stream AI response
        var ui = event.getSource().getUI().orElseThrow();
        var responseBuilder = new StringBuilder();

        aiAssistantService.chat(text, currentConversation)
                .subscribe(
                        token -> {
                            responseBuilder.append(token);
                            ui.access(() -> botMsg.appendText(token));
                        },
                        error -> ui.access(() -> messageInput.setEnabled(true)),
                        () -> ui.access(() -> {
                            saveMessage("assistant", responseBuilder.toString());
                            messageInput.setEnabled(true);
                        })
                );
    }

    private void saveMessage(String role, String content) {
        var msg = dataManager.create(ChatMessage.class);
        msg.setConversation(currentConversation);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        dataManager.save(msg);
    }
}
