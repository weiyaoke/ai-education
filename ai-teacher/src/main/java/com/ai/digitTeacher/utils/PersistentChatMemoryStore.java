package com.ai.digitTeacher.utils;

import com.ai.digitTeacher.entity.UserMessage;
import com.ai.digitTeacher.mapper.MemoryStoreMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final MemoryStoreMapper memoryStoreMapper;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = memoryStoreMapper.getMessages((String) memoryId);
        return json == null ? Collections.emptyList()
                : ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = ChatMessageSerializer.messagesToJson(messages);
        //组装entity
        UserMessage userMessage = UserMessage.builder()
                .memoryId((String) memoryId)
                .messages(json)
                .updatedAt(LocalDateTime.now()).build();
        memoryStoreMapper.insertOrUpdate(userMessage);
    }

    @Override
    public void deleteMessages(Object memoryId) {

    }
}
