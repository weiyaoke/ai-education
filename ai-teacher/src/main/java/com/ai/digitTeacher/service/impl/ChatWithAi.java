package com.ai.digitTeacher.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.ai.digitTeacher.service.Assistant;
import com.ai.digitTeacher.service.ChatWithAI;
import com.ai.digitTeacher.utils.PersistentChatMemoryStore;
import com.ai.digitTeacher.utils.aiTools.queryPointsRecord;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatWithAi implements ChatWithAI {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final QwenChatModel chatModel;
    private final PersistentChatMemoryStore persistentChatMemoryStore;
    private final QwenStreamingChatModel qwenStreamingChatModel;
    @Override
    public String analyze(MultipartFile imageFile, String question) throws IOException {
        //获取图像的二进制编码
        byte[] imageBytes = imageFile.getBytes();
        String base64Data = Base64.getEncoder().encodeToString(imageBytes);
        //转化为图像内容传到大模型
        ImageContent imageContent = ImageContent.from(base64Data, "image/jpg");
        //判断用户对这张图像有没有特殊需求，每天的话就使用默认值
        String imageText = "请描述这张图片，并对图像的内容进行总结";
        if (ObjectUtil.isNotEmpty(question)){
            imageText = question;
        }
        UserMessage userMessage = UserMessage.from(
                TextContent.from(imageText),
                imageContent
        );
        ChatResponse chatResponse = chatModel.chat(userMessage);
        AiMessage aiMessage = chatResponse.aiMessage();
        return aiMessage.text();
    }

    /**
     * 非流式长记忆聊天
     * @param message
     * @return
     */
    @Override
    public String chatWithText(String userId, String message) {
        //初始化长记忆
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(userId)
                .maxMessages(10)
                .chatMemoryStore(persistentChatMemoryStore)
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .tools(new queryPointsRecord())
                .chatMemory(chatMemory)
                .build();
        //与大模型交互
//        String answer = assistant.chat(message);
        Result<String> answer = assistant.modelChat(message);
        System.out.println("大模型返回的内容 " + answer.content());
        System.out.println("answer.toolExecutions() = " + answer.toolExecutions());
        return answer.content();
    }

    /**
     * 流式输出
     * @param messages
     * @return
     */
    @Override
    public String chatWithTextStream(String userId, String messages) {
        //todo 实现长记忆存储聊天
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(userId)
                .maxMessages(10)
                .chatMemoryStore(persistentChatMemoryStore)
                .build();

        this.qwenStreamingChatModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                //1、建立SSE长连接
                //2、写一个循环，其中循环条件是partialResponse是否为空，如果是空的话就break，说明大模型已经输出完毕
                System.out.println("onPartialResponse: " + partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                String answer = completeResponse.aiMessage().text();

                System.out.println("onCompleteResponse: " + completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        });
        return "200";
    }

    @Override
    public String chatRag(String userId, String messages) {
        //初始化长记忆
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(userId)
                .maxMessages(10)
                .chatMemoryStore(persistentChatMemoryStore)
                .build();
        List<Document> documents = FileSystemDocumentLoader.loadDocuments("E:\\workspace\\paper");

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .tools(new queryPointsRecord())
                .contentRetriever(createContentRetriever(documents)) // it should have access to our documents
                .chatMemory(chatMemory)
                .build();

        String answer = assistant.chat("Who is the author of  Physical Adversarial Attack Meets Computer\n" +
                "Vision: A Decade Survey");
        System.out.println("answer = " + answer);

        return null;
    }

    @Override
    public ResponseEntity<String> uploadPdf(MultipartFile file) {
        // 1. 检查文件是否为空
        if (file.isEmpty()) {
            return new ResponseEntity<>("上传失败：文件不能为空。", HttpStatus.BAD_REQUEST);
        }

        // 2. 检查文件类型是否为PDF
        if (!Objects.equals(file.getContentType(), "application/pdf")) {
            return new ResponseEntity<>("上传失败：只允许上传PDF格式的文件。", HttpStatus.BAD_REQUEST);
        }

        try {
            // 3. 清理文件名，防止路径遍历攻击
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());

            // 4. 创建上传目录的Path对象
            Path uploadPath = Paths.get(uploadDir);

            // 5. 如果目录不存在，则创建它
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 6. 解析文件的目标存放路径
            Path filePath = uploadPath.resolve(fileName);

            // 7. 将文件输入流复制到目标路径
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 8. 返回成功的响应
            String successMessage = "文件上传成功: " + fileName + "，已保存至: " + filePath.toAbsolutePath().toString();
            return ResponseEntity.ok(successMessage);

        } catch (IOException e) {
            // 9. 捕获IO异常，返回失败的响应
            e.printStackTrace();
            String failureMessage = "上传失败：无法保存文件。错误: " + e.getMessage();
            return new ResponseEntity<>(failureMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static ContentRetriever createContentRetriever(List<Document> documents) {

        // Here, we create an empty in-memory store for our documents and their embeddings.
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // Here, we are ingesting our documents into the store.
        // Under the hood, a lot of "magic" is happening, but we can ignore it for now.
        EmbeddingStoreIngestor.ingest(documents, embeddingStore);

        // Lastly, let's create a content retriever from an embedding store.
        return EmbeddingStoreContentRetriever.from(embeddingStore);
    }


}
