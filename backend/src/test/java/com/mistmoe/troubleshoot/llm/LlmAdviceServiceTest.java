package com.mistmoe.troubleshoot.llm;

import com.mistmoe.troubleshoot.analyze.AnalyzeRequest;
import com.mistmoe.troubleshoot.errorcase.ErrorCase;
import com.mistmoe.troubleshoot.kg.GraphResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmAdviceServiceTest {

    @Test
    void disabledConfigReturnsKnowledgeBaseFallback() {
        LlmProperties properties = new LlmProperties();
        properties.setEnabled(false);
        LlmAdviceService service = new LlmAdviceService(properties);

        String advice = service.generateAdvice(
                new AnalyzeRequest("Port 8080 already in use", "Spring Boot", 3L),
                sampleErrorCase(),
                new GraphResponse(List.of(), List.of())
        );

        assertThat(advice).contains("知识库").contains("默认端口 8080");
    }

    @Test
    void qianfanProviderWithoutCredentialsReturnsKnowledgeBaseFallback() {
        LlmProperties properties = new LlmProperties();
        properties.setEnabled(true);
        properties.setProvider("qianfan");
        properties.setQianfanAccessKey("");
        properties.setQianfanSecretKey("");
        LlmAdviceService service = new LlmAdviceService(properties);

        String advice = service.generateAdvice(
                new AnalyzeRequest("Port 8080 already in use", "Spring Boot", 3L),
                sampleErrorCase(),
                new GraphResponse(List.of(), List.of())
        );

        assertThat(advice).contains("8080");
    }

    private ErrorCase sampleErrorCase() {
        return new ErrorCase(
                1L,
                "端口被占用",
                "后端启动",
                "Spring Boot",
                "Port 8080 already in use",
                "Spring Boot 启动失败，提示端口已被占用",
                "默认端口 8080 已被其他进程占用",
                "关闭占用进程，或在 application.yml 中修改 server.port",
                "application.yml",
                "netstat -ano | findstr 8080",
                "结束进程前确认不是重要服务",
                1L,
                1L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
