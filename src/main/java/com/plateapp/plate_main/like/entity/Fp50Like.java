package com.plateapp.plate_main.like.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fp_50")
public class Fp50Like {

  @EmbeddedId
  private Fp50LikeId id;

  // bpchar(1) 대응: 'Y' / 'N'
  @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "bpchar(1)")
  private String useYn;

  @Column(name = "deleted_at")
  private LocalDate deletedAt;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    if (useYn == null) useYn = "Y"; // 좋아요 생성은 기본 Y
    createdAt = LocalDateTime.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public boolean isActive() {
    return "Y".equalsIgnoreCase(useYn);
  }

  public void activate() {
    this.useYn = "Y";
    this.deletedAt = null;
  }

  public void deactivate() {
    this.useYn = "N";
    this.deletedAt = LocalDate.now();
  }
}
