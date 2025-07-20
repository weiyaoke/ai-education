package com.ai.digitTeacher.utils;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StreamMessageWindowChatMemory {

    public  Object id;
    public Integer maxMessages;
    private final StreamPersistentChatMemoryStore store;

    //1、获取内容
    public List<ChatMessage> messages() {
        List<ChatMessage> messages = new LinkedList(this.store.getMessages(this.id));
        ensureCapacity(messages, this.maxMessages);
        return messages;
    }

    //2、插入更新
    public void add(ChatMessage message) {
        List<ChatMessage> messages = this.messages();
        if (message instanceof SystemMessage) {
            Optional<SystemMessage> systemMessage = findSystemMessage(messages);
            if (systemMessage.isPresent()) {
                if (((SystemMessage)systemMessage.get()).equals(message)) {
                    return;
                }
                messages.remove(systemMessage.get());
            }
        }

        messages.add(message);
        ensureCapacity(messages, this.maxMessages);
        this.store.updateMessages(this.id, messages);
    }
    //判断容量
    private static void ensureCapacity(List<ChatMessage> messages, int maxMessages) {
        label27:
        while(true) {
            if (messages.size() > maxMessages) {
                int messageToEvictIndex = 0;
                if (messages.get(0) instanceof SystemMessage) {
                    messageToEvictIndex = 1;
                }

                ChatMessage evictedMessage = (ChatMessage)messages.remove(messageToEvictIndex);
                if (!(evictedMessage instanceof AiMessage)) {
                    continue;
                }

                AiMessage aiMessage = (AiMessage)evictedMessage;
                if (!aiMessage.hasToolExecutionRequests()) {
                    continue;
                }

                while(true) {
                    if (messages.size() <= messageToEvictIndex || !(messages.get(messageToEvictIndex) instanceof ToolExecutionResultMessage)) {
                        continue label27;
                    }

                    messages.remove(messageToEvictIndex);
                }
            }
            return;
        }
    }
    private static Optional<SystemMessage> findSystemMessage(List<ChatMessage> messages) {
        return messages.stream().filter((message) -> {
            return message instanceof SystemMessage;
        }).map((message) -> {
            return (SystemMessage)message;
        }).findAny();
    }
    public void clear() {
        this.store.deleteMessages(this.id);
    }
}
