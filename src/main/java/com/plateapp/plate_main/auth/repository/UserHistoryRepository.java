// src/main/java/com/plateapp/plate_main/auth/repository/UserHistoryRepository.java
package com.plateapp.plate_main.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.auth.domain.UserHistory;

public interface UserHistoryRepository extends JpaRepository<UserHistory, Long> {
}
