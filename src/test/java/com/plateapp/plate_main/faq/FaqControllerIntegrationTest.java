package com.plateapp.plate_main.faq;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plateapp.plate_main.faq.entity.Fp900Faq;
import com.plateapp.plate_main.faq.repository.FaqRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FaqControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FaqRepository faqRepository;

    @BeforeEach
    void setUp() {
        faqRepository.deleteAll();
    }

    @Test
    void listFaqsReturnsOnlyPublishedAndAppliesRequestedSort() throws Exception {
        insertFaq("authorA", "계정", "일반 글", "본문", false, 20, 2, "published", now(), now());
        insertFaq("authorB", "공지", "고정 글", "본문", true, 30, 5, "published", now(), now());
        insertFaq("authorC", "계정", "비공개 글", "본문", true, 40, 1, "draft", now(), now());
        insertFaq("authorD", "계정", "같은 순서 최신 글", "본문", false, 50, 2, "published", now(), now());

        mockMvc.perform(get("/api/faqs")
                .param("category", "계정")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].title").value("같은 순서 최신 글"))
            .andExpect(jsonPath("$.content[0].isPinned").value(false))
            .andExpect(jsonPath("$.content[1].title").value("일반 글"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void getFaqIncrementsViewCountAndAllowsAnonymousAccess() throws Exception {
        Integer faqId = insertFaq("admin", "공지", "이용 안내", "FAQ 본문", true, 7, 1, "published", now(), now());

        mockMvc.perform(get("/api/faqs/{faqId}", faqId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.faqId").value(faqId))
            .andExpect(jsonPath("$.viewCount").value(8))
            .andExpect(jsonPath("$.isPinned").value(true));
    }

    private Integer insertFaq(
        String username,
        String category,
        String title,
        String answer,
        boolean isPinned,
        int viewCount,
        int displayOrder,
        String statusCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        return jdbcTemplate.queryForObject(
            """
                insert into fp_900
                    (username, category, title, answer, is_pinned, view_count, display_order, status_code, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                returning faq_id
            """,
            Integer.class,
            username,
            category,
            title,
            answer,
            isPinned,
            viewCount,
            displayOrder,
            statusCode,
            createdAt,
            updatedAt
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 3, 22, 10, 0);
    }
}
