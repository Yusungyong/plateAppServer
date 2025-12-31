// src/main/java/com/plateapp/plate_main/feed/entity/Fp400ImageFeed.java
package com.plateapp.plate_main.feed.entity;

import java.time.LocalDateTime;

import com.plateapp.plate_main.user.entity.Fp100User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fp_400")
public class Fp400ImageFeed {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "feed_no")
  private Integer feedId; // DB는 feed_no지만, 코드에서는 feedId로 쓰자

  @Column(name = "username", nullable = false)
  private String username;

  // ✅ 작성자 정보 (fp_100)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "username", referencedColumnName = "username", insertable = false, updatable = false)
  private Fp100User writer;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "images", columnDefinition = "text")
  private String images; // 콤마 구분자

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "feed_title")
  private String feedTitle;

  @Column(name = "location")
  private String location;

  @Column(name = "store_name")
  private String storeName;

  @Column(name = "place_id")
  private String placeId;

  @Column(name = "use_yn", nullable = false)
  private String useYn;

  @Column(name = "thumbnail")
  private String thumbnail;

  public Integer getFeedId() { return feedId; }
  public String getUsername() { return username; }
  public Fp100User getWriter() { return writer; }
  public String getContent() { return content; }
  public String getImages() { return images; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public String getFeedTitle() { return feedTitle; }
  public String getLocation() { return location; }
  public String getStoreName() { return storeName; }
  public String getPlaceId() { return placeId; }
  public String getUseYn() { return useYn; }
  public String getThumbnail() { return thumbnail; }
}
