package com.ai.digitTeacher.utils;

import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StreamingChatResponseHandler implements dev.langchain4j.model.chat.response.StreamingChatResponseHandler {

    private final PersistentChatMemoryStore persistentChatMemoryStore;
    private final StringRedisTemplate redisTemplate;
    private Object memoryId;
    private static final String contextKey = "DigitTeacher:StreamChat:UserId:";
    @Override
    public void onPartialResponse(String s) {

    }

    @Override
    public void onCompleteResponse(ChatResponse chatResponse) {
        //把大模型返回的内容写进redis
        this.redisTemplate.opsForValue().set(contextKey + (String) memoryId,chatResponse.aiMessage().text());
    }

    @Override
    public void onError(Throwable throwable) {

    }
}
