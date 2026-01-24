package com.plateapp.plate_main.video.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "fp_305")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fp305WatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "username", nullable = false, length = 20)
    private String username;

    @Column(name = "store_id", nullable = false)
    private Integer storeId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "timestamp")
    private OffsetDateTime timestamp;

    @Column(name = "duration_watched")
    private Integer durationWatched;

    @Column(name = "completion_status")
    private Boolean completionStatus;

    @Column(name = "video_quality", length = 10)
    private String videoQuality;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "comments", columnDefinition = "text")
    private String comments;

    @Builder.Default
    @JdbcTypeCode(Types.CHAR)
    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "Y";

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = OffsetDateTime.now();
        }
        if (useYn == null) {
            useYn = "Y";
        }
        if (durationWatched == null) {
            durationWatched = 0;
        }
        if (completionStatus == null) {
            completionStatus = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        timestamp = OffsetDateTime.now();
    }
}
