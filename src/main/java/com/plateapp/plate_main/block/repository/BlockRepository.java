package com.plateapp.plate_main.block.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.plateapp.plate_main.block.entity.Fp160Block;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlockRepository extends JpaRepository<Fp160Block, Integer> {
    boolean existsByBlockerUsernameAndBlockedUsername(String blockerUsername, String blockedUsername);
    long deleteByBlockerUsernameAndBlockedUsername(String blockerUsername, String blockedUsername);
    Page<Fp160Block> findByBlockerUsername(String blockerUsername, Pageable pageable);

    @Query("""
        select b.blockedUsername
        from Fp160Block b
        where b.blockerUsername = :username
    """)
    List<String> findBlockedUsernames(@Param("username") String username);
}
