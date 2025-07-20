package com.ai.digitTeacher.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@TableName("chat_memory")
public class UserMessage implements ChatMessage {
    @TableId("memory_id")
    private String memoryId;
    private String messages;
    private LocalDateTime updatedAt;

    public String getMemoryId() {
        return memoryId;
    }

    public void setMemoryId(String memoryId) {
        this.memoryId = memoryId;
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public ChatMessageType type() {
        return null;
    }
}
