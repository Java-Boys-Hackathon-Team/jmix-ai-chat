package ru.javaboys.jmixaichat.view.chat;

import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import ru.javaboys.jmixaichat.service.AiAssistantService;
import ru.javaboys.jmixaichat.view.main.MainView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Route(value = "chat", layout = MainView.class)
@ViewController("AiChatView")
@ViewDescriptor(path = "ai-chat-view.xml")
public class AiChatView extends StandardView {

    @Autowired
    private AiAssistantService aiAssistantService;

    private final List<MessageListItem> items = new ArrayList<>();
    private MessageList messageList;
    private MessageInput messageInput;

    @Subscribe
    public void onInit(InitEvent event) {
        var content = getContent();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);

        messageList = new MessageList();
        messageList.setSizeFull();
        messageList.setMarkdown(true);

        messageInput = new MessageInput();
        messageInput.setWidthFull();
        messageInput.addSubmitListener(this::onSubmit);

        content.add(messageList, messageInput);
        content.expand(messageList);
    }

    private void onSubmit(MessageInput.SubmitEvent event) {
        String text = event.getValue();

        var userMsg = new MessageListItem(text, Instant.now(), "You");
        userMsg.setUserColorIndex(0);
        items.add(userMsg);

        var botMsg = new MessageListItem("", Instant.now(), "AI Assistant");
        botMsg.setUserColorIndex(2);
        items.add(botMsg);
        messageList.setItems(items);

        messageInput.setEnabled(false);

        var ui = event.getSource().getUI().orElseThrow();
        aiAssistantService.chat(text)
                .subscribe(
                        token -> ui.access(() -> botMsg.appendText(token)),
                        error -> ui.access(() -> messageInput.setEnabled(true)),
                        () -> ui.access(() -> messageInput.setEnabled(true))
                );
    }
}
