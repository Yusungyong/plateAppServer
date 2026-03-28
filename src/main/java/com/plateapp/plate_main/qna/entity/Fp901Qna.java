package com.plateapp.plate_main.qna.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(name = "fp_901")
@Getter
public class Fp901Qna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qna_id")
    private Integer qnaId;

    @Column(name = "username", length = 20)
    private String username;

    @Column(name = "guest_name", length = 100)
    private String guestName;

    @Column(name = "guest_email", length = 255)
    private String guestEmail;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "question", nullable = false, columnDefinition = "text")
    private String question;

    @Column(name = "answer", columnDefinition = "text")
    private String answer;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    protected Fp901Qna() {
    }

    public static Fp901Qna create(
        String username,
        String guestName,
        String guestEmail,
        String category,
        String question,
        Boolean isPublic
    ) {
        Fp901Qna qna = new Fp901Qna();
        LocalDateTime now = LocalDateTime.now();
        qna.username = username;
        qna.guestName = guestName;
        qna.guestEmail = guestEmail;
        qna.category = category;
        qna.question = question;
        qna.answer = null;
        qna.statusCode = "received";
        qna.isPublic = !Boolean.FALSE.equals(isPublic);
        qna.createdAt = now;
        qna.updatedAt = now;
        qna.answeredAt = null;
        return qna;
    }

    public void answer(String answer, String statusCode, Boolean isPublic) {
        this.answer = answer;
        this.statusCode = statusCode;
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
        this.updatedAt = LocalDateTime.now();
        this.answeredAt = "answered".equals(statusCode) ? this.updatedAt : null;
    }
}
