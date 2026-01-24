package com.plateapp.plate_main.friend.repository;

import com.plateapp.plate_main.friend.entity.Fp150Friend;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Fp150FriendRepository extends JpaRepository<Fp150Friend, Integer> {

    List<Fp150Friend> findByUsername(String username);

    List<Fp150Friend> findByUsernameAndStatus(String username, String status);

    Page<Fp150Friend> findByUsernameAndStatus(String username, String status, Pageable pageable);

    long countByUsernameAndStatus(String username, String status);

    Optional<Fp150Friend> findByUsernameAndFriendName(String username, String friendName);

    boolean existsByUsernameAndFriendNameAndStatus(String username, String friendName, String status);

    @Modifying
    @Query("DELETE FROM Fp150Friend f WHERE f.username = :username AND f.friendName = :friendName")
    void deleteByUsernameAndFriendName(@Param("username") String username, @Param("friendName") String friendName);

    @Query(value = """
        SELECT
          f.id                 AS id,
          f.username           AS username,
          f.friend_name        AS friendName,
          COALESCE(u.nick_name, f.friend_name) AS friendNickname,
          f.status             AS status,
          u.profile_image_url  AS friendProfileImageUrl,
          u.active_region      AS friendActiveRegion,
          f.initiator_username AS initiatorUsername,
          f.message            AS message,
          f.created_at         AS createdAt,
          f.updated_at         AS updatedAt,
          f.accepted_at        AS acceptedAt
        FROM fp_150 f
        LEFT JOIN fp_100 u ON u.username = f.friend_name
        WHERE f.username = :username
          AND f.friend_name <> :username
          AND (:status IS NULL OR LOWER(f.status) = LOWER(:status))
          AND (
            f.friend_name ILIKE CONCAT('%', :keyword, '%')
            OR COALESCE(u.nick_name, '') ILIKE CONCAT('%', :keyword, '%')
          )
          AND NOT EXISTS (
            SELECT 1 FROM fp_160 b
             WHERE (b.blocker_username = :username AND b.blocked_username = f.friend_name)
                OR (b.blocker_username = f.friend_name AND b.blocked_username = :username)
          )
        ORDER BY f.id DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<FriendSuggestRow> suggestFriends(
            @Param("username") String username,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    interface FriendSuggestRow {
        Integer getId();
        String getUsername();
        String getFriendName();
        String getFriendNickname();
        String getStatus();
        String getFriendProfileImageUrl();
        String getFriendActiveRegion();
        String getInitiatorUsername();
        String getMessage();
        java.time.LocalDateTime getCreatedAt();
        java.time.LocalDateTime getUpdatedAt();
        java.time.LocalDateTime getAcceptedAt();
    }
}
