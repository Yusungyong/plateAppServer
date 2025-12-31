// src/main/java/com/plateapp/plate_main/auth/domain/UserHistory.java
package com.plateapp.plate_main.auth.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fp_101")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "before_ex")
    private String beforeEx;

    @Column(name = "after_ex")
    private String afterEx;

    @Column(name = "change_tp", nullable = false)
    private String changeTp;

    @Column(name = "created_dt")
    private LocalDateTime createdDt;
}
