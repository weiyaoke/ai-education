package com.ai.digitTeacher.domain.dto;

import java.util.List;

public class StreamUserChatMessage {
    private List<UserRequest> contexts;
    private String type;

    public List<UserRequest> getContexts() {
        return contexts;
    }

    public void setContexts(List<UserRequest> contexts) {
        this.contexts = contexts;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
