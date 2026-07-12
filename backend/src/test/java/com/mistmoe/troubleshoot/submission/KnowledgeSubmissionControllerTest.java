package com.mistmoe.troubleshoot.submission;

import com.mistmoe.troubleshoot.llm.LlmAdviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KnowledgeSubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private LlmAdviceService llmAdviceService;

    @Test
    void draftCreatesStructuredCandidateFromRawExperience() throws Exception {
        when(llmAdviceService.generateCaseDraft(any())).thenReturn(new KnowledgeSubmissionDraft(
                "MySQL 账号密码错误",
                "Access denied,using password",
                "数据库连接失败，提示 Access denied",
                "MySQL 用户名或密码配置错误",
                "检查 application.yml 中 datasource username/password",
                "application.yml",
                "mysql -uroot -p",
                "不要把真实密码提交到代码仓库"
        ));

        mockMvc.perform(post("/api/submissions/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submitUserId": 3,
                                  "rawLog": "Access denied for user root using password: YES",
                                  "techStack": "MySQL",
                                  "stage": "数据库连接",
                                  "userSolution": "后来发现 application.yml 密码写错了"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draft.errorName").value("MySQL 账号密码错误"))
                .andExpect(jsonPath("$.draft.relatedFile").value("application.yml"));
    }

    @Test
    void createListApproveAndRejectSubmission() throws Exception {
        String createResponse = mockMvc.perform(post("/api/submissions/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submitUserId": 3,
                                  "rawLog": "Access denied for user root using password: YES",
                                  "techStack": "MySQL",
                                  "stage": "数据库连接",
                                  "userSolution": "修改 datasource 密码后恢复",
                                  "draft": {
                                    "errorName": "MySQL 账号密码错误",
                                    "keyword": "Access denied,using password",
                                    "symptom": "数据库连接失败，提示 Access denied",
                                    "cause": "MySQL 用户名或密码配置错误",
                                    "solution": "检查 application.yml 中 datasource username/password",
                                    "relatedFile": "application.yml",
                                    "relatedCommand": "mysql -uroot -p",
                                    "riskTip": "不要把真实密码提交到代码仓库"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.draftErrorName").value("MySQL 账号密码错误"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number createdId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");
        long submissionId = createdId.longValue();

        mockMvc.perform(get("/api/submissions/list").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/submissions/{id}", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawLog").value("Access denied for user root using password: YES"));

        mockMvc.perform(put("/api/submissions/{id}/approve", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewerId": 2,
                                  "reviewComment": "内容完整，可以入库"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedErrorCaseId").isNumber());

        Integer caseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM error_case WHERE error_name = 'MySQL 账号密码错误'",
                Integer.class
        );
        assertThat(caseCount).isEqualTo(1);

        String rejectResponse = mockMvc.perform(post("/api/submissions/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "submitUserId": 3,
                                  "rawLog": "unknown error",
                                  "techStack": "Vue",
                                  "stage": "前端构建",
                                  "userSolution": "暂不清楚",
                                  "draft": {
                                    "errorName": "信息不足",
                                    "keyword": "unknown",
                                    "symptom": "描述不足",
                                    "cause": "无法判断",
                                    "solution": "补充日志",
                                    "relatedFile": "",
                                    "relatedCommand": "",
                                    "riskTip": "需补充上下文"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number createdRejectId = com.jayway.jsonpath.JsonPath.read(rejectResponse, "$.id");
        long rejectedId = createdRejectId.longValue();

        mockMvc.perform(put("/api/submissions/{id}/reject", rejectedId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewerId": 1,
                                  "reviewComment": "日志太少，无法沉淀"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewComment").value("日志太少，无法沉淀"));
    }
}
