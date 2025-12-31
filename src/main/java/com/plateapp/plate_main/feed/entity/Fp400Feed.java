package com.plateapp.plate_main.feed.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fp_400")
public class Fp400Feed {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "feed_no")
  private Integer feedNo;

  @Column(name = "username", nullable = false, length = 50)
  private String username;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  // 콤마 구분 파일명 문자열
  @Column(name = "images", columnDefinition = "text")
  private String images;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "feed_title", length = 255)
  private String feedTitle;

  @Column(name = "location", length = 50)
  private String location;

  @Column(name = "store_name", length = 50)
  private String storeName;

  @Column(name = "place_id", length = 255)
  private String placeId;

  @Column(name = "use_yn", nullable = false, length = 1)
  private String useYn = "Y";

  // 현재 데이터 없음(그래도 컬럼은 매핑 유지)
  @Column(name = "thumbnail", length = 255)
  private String thumbnail;

  public Integer getFeedNo() { return feedNo; }
  public String getUsername() { return username; }
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
