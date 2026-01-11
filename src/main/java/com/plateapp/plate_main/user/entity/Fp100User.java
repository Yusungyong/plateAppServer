package com.plateapp.plate_main.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fp_100")
public class Fp100User {

  @Id
  @Column(name = "username", length = 20)
  private String username;

  // user_id는 serial이지만 PK가 아니라면 generatedValue를 걸 수 없어서 읽기용으로 두는 게 안전
  @Column(name = "user_id", insertable = false, updatable = false)
  private Integer userId;

  @Column(name = "nick_name")
  private String nickName;

  @Column(name = "profile_image_url")
  private String profileImageUrl;

  @Column(name = "is_private")
  private Boolean isPrivate;

  @Column(name = "active_region")
  private String activeRegion;

  // getters/setters
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }

  public String getNickName() { return nickName; }
  public void setNickName(String nickName) { this.nickName = nickName; }

  public String getProfileImageUrl() { return profileImageUrl; }
  public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

  public Boolean getIsPrivate() { return isPrivate; }
  public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }

  public String getActiveRegion() { return activeRegion; }
  public void setActiveRegion(String activeRegion) { this.activeRegion = activeRegion; }
}
