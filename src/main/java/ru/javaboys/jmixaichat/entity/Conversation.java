package ru.javaboys.jmixaichat.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@JmixEntity
@Entity
@Table(name = "CONVERSATION")
public class Conversation {

    @Id
    @Column(name = "ID")
    @JmixGeneratedValue
    private UUID id;

    @InstanceName
    @Column(name = "TITLE", nullable = false)
    private String title;

    @OrderBy("createdAt")
    @OneToMany(mappedBy = "conversation")
    private List<ChatMessage> messages;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
