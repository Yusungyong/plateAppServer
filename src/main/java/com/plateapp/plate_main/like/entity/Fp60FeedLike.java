package com.plateapp.plate_main.like.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "fp_60")
@IdClass(Fp60FeedLike.Pk.class)
public class Fp60FeedLike {

  @Id
  @Column(name = "username", nullable = false, length = 255)
  private String username;

  @Id
  @Column(name = "feed_id", nullable = false)
  private Integer feedId;

  @Column(name = "use_yn", nullable = false, columnDefinition = "bpchar(1)")
  private String useYn = "Y";

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
    if (useYn == null) useYn = "Y";
  }

  @PreUpdate
  void preUpdate() { updatedAt = LocalDateTime.now(); }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public Integer getFeedId() { return feedId; }
  public void setFeedId(Integer feedId) { this.feedId = feedId; }

  public static class Pk implements Serializable {
    private String username;
    private Integer feedId;
    public Pk() {}
    public Pk(String username, Integer feedId) { this.username = username; this.feedId = feedId; }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Pk)) return false;
      Pk pk = (Pk) o;
      return Objects.equals(username, pk.username) && Objects.equals(feedId, pk.feedId);
    }
    @Override public int hashCode() { return Objects.hash(username, feedId); }
  }
}
