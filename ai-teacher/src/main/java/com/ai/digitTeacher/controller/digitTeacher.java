package com.ai.digitTeacher.controller;


import com.ai.digitTeacher.service.ChatWithAI;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@Slf4j
public class digitTeacher {
    @Resource
    private ChatModel qwenChatModel;

    @Resource
    private ChatWithAI chatWithAI;

    //TODO 总结视频内容，并且返回内容给前端

    /**
     * 用户上传视频 mp4
     *    ↓
     * 后端用 FFmpeg 拆帧（每 N 秒一帧，压缩到 512×512，jpg）
     *    ↓
     * 把每一帧转成 base64
     *    ↓
     * 组装 DashScope 规定的 JSON payload（model=qwen-vl-plus）
     *    ↓
     * HTTP POST https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation
     *    ↓
     * 解析返回的 summary
     * @param file
     * @return
     */
    @PostMapping("/summary")
    public String summary(@RequestParam("file") MultipartFile file) throws Exception {
        return null;
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam("file") MultipartFile file,
                          @RequestParam(value = "q", defaultValue = "请描述这张图片") String question) throws IOException {
        return this.chatWithAI.analyze(file,question);
    }
    //聊天记忆
    @RequestMapping("/chat")
    public String chatWithText(String userId, String messages) {
        return this.chatWithAI.chatWithText(userId, messages);
    }

    //流式聊天记忆
    @RequestMapping("/stream/chat")
    public String chatWithTextStream(@RequestParam("userId") String userId, @RequestParam("messages") String messages) {
        return this.chatWithAI.chatWithTextStream(userId, messages);
    }
    //Rag功能
    @RequestMapping("/rag/chat")
    public String chatRag(String userId, String messages){
        return this.chatWithAI.chatRag(userId,messages);
    }
    /**
     * 处理PDF文件上传的接口
     * @param file 前端发送过来的文件，@RequestParam("file")中的"file"必须和前端form表单中input标签的name属性一致
     * @return 返回上传结果
     */
    @PostMapping("/upload/pdf")
    public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file) {
        return this.uploadPdf(file);
    }

}
