// src/main/java/com/plateapp/plate_main/auth/repository/LoginHistoryRepository.java
package com.plateapp.plate_main.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.auth.domain.LoginHistory;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    LoginHistory findTop1ByUsernameOrderByLoginDatetimeDesc(String username);
}
