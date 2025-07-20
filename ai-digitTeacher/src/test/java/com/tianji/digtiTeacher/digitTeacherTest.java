package com.tianji.digtiTeacher;

import dev.langchain4j.model.openai.OpenAiChatModel;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.concurrent.DelayQueue;

/**
 * Unit test for simple App.
 */
@SpringBootTest
public class digitTeacherTest {
    @Test
    void testAi() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey("27a558fcd7f149e7a63d22b1572cae7f.HcXi8Ent9FYksyTP")
                .modelName("gpt-4o-mini")
                .build();
        String answer = model.chat("Say 'Hello World'");
        System.out.println(answer); // Hello World
    }
}
