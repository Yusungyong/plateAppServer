package com.plateapp.plate_main.report.entity;

import java.time.LocalDateTime;
import java.sql.Types;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

@Getter
@Setter
@Entity
@Table(name = "fp_40")
public class Fp40Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "reporter_username", nullable = false)
    private String reporterUsername;

    @Column(name = "target_username")
    private String targetUsername;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Integer targetId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "target_flag", length = 1, nullable = false)
    private String targetFlag;

    @Column(name = "unflagged_at")
    private LocalDateTime unflaggedAt;
}
