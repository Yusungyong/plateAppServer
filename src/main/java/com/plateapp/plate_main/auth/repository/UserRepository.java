// src/main/java/com/plateapp/plate_main/auth/repository/UserRepository.java
package com.plateapp.plate_main.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.auth.domain.User;

public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameAndEmail(String username, String email);

    /** fp_110.user_id -> fp_100.user_id 로 유저 조회 */
    @Query(value = "SELECT * FROM fp_100 WHERE user_id = :userId", nativeQuery = true)
    Optional<User> findByUserId(@Param("userId") Integer userId);

    /** 새 유저 생성 후 username 으로 user_id 가져오기 */
    @Query(value = "SELECT user_id FROM fp_100 WHERE username = :username", nativeQuery = true)
    Integer findUserIdByUsername(@Param("username") String username);
}
