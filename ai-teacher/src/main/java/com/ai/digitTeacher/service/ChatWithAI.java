package com.ai.digitTeacher.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ChatWithAI {
    String analyze(MultipartFile file,String question) throws IOException;
    String chatWithText(String userId, String messages);
    String chatWithTextStream(String userId, String messages);
    String chatRag(String userId, String messages);
    ResponseEntity<String> uploadPdf(MultipartFile file);

}
