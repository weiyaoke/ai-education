package com.ai.digitTeacher.service;

import dev.langchain4j.service.Result;

public interface Assistant {
    String chat(String message);

    Result<String> modelChat(String userMessage);
}
