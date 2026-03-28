package com.plateapp.plate_main.faq.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(name = "fp_900")
@Getter
public class Fp900Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_id")
    private Integer faqId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "answer", columnDefinition = "text")
    private String answer;

    @Column(name = "is_pinned")
    private Boolean isPinned;

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "status_code", nullable = false, length = 50)
    private String statusCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Fp900Faq() {
    }

    public static Fp900Faq create(
        String username,
        String category,
        String title,
        String answer,
        Boolean isPinned,
        Integer displayOrder,
        String statusCode
    ) {
        Fp900Faq faq = new Fp900Faq();
        LocalDateTime now = LocalDateTime.now();
        faq.username = username;
        faq.category = category;
        faq.title = title;
        faq.answer = answer;
        faq.isPinned = Boolean.TRUE.equals(isPinned);
        faq.viewCount = 0;
        faq.displayOrder = displayOrder;
        faq.statusCode = statusCode;
        faq.createdAt = now;
        faq.updatedAt = now;
        return faq;
    }

    public void update(
        String category,
        String title,
        String answer,
        Boolean isPinned,
        Integer displayOrder,
        String statusCode
    ) {
        this.category = category;
        this.title = title;
        this.answer = answer;
        this.isPinned = Boolean.TRUE.equals(isPinned);
        this.displayOrder = displayOrder;
        this.statusCode = statusCode;
        this.updatedAt = LocalDateTime.now();
    }
}
