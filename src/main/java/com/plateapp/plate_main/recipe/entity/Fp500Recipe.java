package com.plateapp.plate_main.recipe.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "fp_500")
public class Fp500Recipe {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "author_id", nullable = false)
  private Integer authorId;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "slug", length = 255)
  private String slug;

  @Column(name = "summary", length = 500)
  private String summary;

  @Column(name = "content", columnDefinition = "text")
  private String content;

  @Column(name = "servings")
  private Integer servings;

  @Column(name = "cook_time_min")
  private Integer cookTimeMin;

  @Column(name = "difficulty", length = 10)
  private String difficulty;

  @Column(name = "thumbnail_url", length = 1024)
  private String thumbnailUrl;

  @Column(name = "cover_url", length = 1024)
  private String coverUrl;

  @Column(name = "view_count")
  private Integer viewCount;

  @Column(name = "like_count")
  private Integer likeCount;

  @Column(name = "is_published")
  private Boolean isPublished;

  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public Long getId() { return id; }

  public Integer getAuthorId() { return authorId; }
  public void setAuthorId(Integer authorId) { this.authorId = authorId; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getSlug() { return slug; }
  public void setSlug(String slug) { this.slug = slug; }

  public String getSummary() { return summary; }
  public void setSummary(String summary) { this.summary = summary; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public Integer getServings() { return servings; }
  public void setServings(Integer servings) { this.servings = servings; }

  public Integer getCookTimeMin() { return cookTimeMin; }
  public void setCookTimeMin(Integer cookTimeMin) { this.cookTimeMin = cookTimeMin; }

  public String getDifficulty() { return difficulty; }
  public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

  public String getThumbnailUrl() { return thumbnailUrl; }
  public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

  public String getCoverUrl() { return coverUrl; }
  public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

  public Integer getViewCount() { return viewCount; }
  public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

  public Integer getLikeCount() { return likeCount; }
  public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

  public Boolean getIsPublished() { return isPublished; }
  public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }

  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
