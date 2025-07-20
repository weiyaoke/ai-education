package com.ai.digitTeacher.model;

import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

public class DashScope {
    @Value("${dashscope.chat-model.api-key}")
    private String apiKey;

    @Value("${dashscope.chat-model.model-name}")
    private String modelName;
    public StreamingChatModel streamingChatModel_dashScope(){
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
