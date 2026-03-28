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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FaqControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FaqRepository faqRepository;

    @BeforeEach
    void setUp() {
        faqRepository.deleteAll();
    }

    @Test
    void listFaqsReturnsOnlyPublishedAndAppliesRequestedSort() throws Exception {
        insertFaq("authorA", "account", "normal post", "content", false, 20, 2, "published", now(), now());
        insertFaq("authorB", "notice", "pinned post", "content", true, 30, 5, "published", now(), now());
        insertFaq("authorC", "account", "draft post", "content", true, 40, 1, "draft", now(), now());
        insertFaq("authorD", "account", "same order newer post", "content", false, 50, 2, "published", now(), now());

        mockMvc.perform(get("/api/faqs")
                .param("category", "account")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].title").value("same order newer post"))
            .andExpect(jsonPath("$.content[0].isPinned").value(false))
            .andExpect(jsonPath("$.content[1].title").value("normal post"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void getFaqIncrementsViewCountAndAllowsAnonymousAccess() throws Exception {
        Integer faqId = insertFaq("admin", "notice", "usage guide", "FAQ body", true, 7, 1, "published", now(), now());

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
        Fp900Faq faq = Fp900Faq.create(
            username,
            category,
            title,
            answer,
            isPinned,
            displayOrder,
            statusCode
        );
        ReflectionTestUtils.setField(faq, "viewCount", viewCount);
        ReflectionTestUtils.setField(faq, "createdAt", createdAt);
        ReflectionTestUtils.setField(faq, "updatedAt", updatedAt);
        return faqRepository.save(faq).getFaqId();
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 3, 22, 10, 0);
    }
}
