package com.ai.digitTeacher.domain.dto;

import java.util.List;

public class StreamAiChatMessage<T> {
    private String text;
    private List<T> toolExecutionRequests;
    private String type;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<T> getToolExecutionRequests() {
        return toolExecutionRequests;
    }

    public void setToolExecutionRequests(List<T> toolExecutionRequests) {
        this.toolExecutionRequests = toolExecutionRequests;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
