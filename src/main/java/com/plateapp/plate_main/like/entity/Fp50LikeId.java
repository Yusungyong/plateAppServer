package com.plateapp.plate_main.like.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class Fp50LikeId implements Serializable {

  @Column(name = "username", nullable = false, length = 50)
  private String username;

  @Column(name = "store_id", nullable = false)
  private Integer storeId;
}
