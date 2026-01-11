package com.plateapp.plate_main.like.repository;

import com.plateapp.plate_main.like.entity.Fp60FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface FeedLikeRepository extends JpaRepository<Fp60FeedLike, Fp60FeedLike.Pk> {
  Optional<Fp60FeedLike> findByUsernameAndFeedIdAndUseYn(String username, Integer feedId, String useYn);
  long countByFeedIdAndUseYn(Integer feedId, String useYn);
  boolean existsByUsernameAndFeedIdAndUseYnAndDeletedAtIsNull(String username, Integer feedId, String useYn);

  @Query(value = """
      select
        u.user_id           as userId,
        u.username          as username,
        u.nick_name         as nickname,
        u.profile_image_url as profileImageUrl,
        u.active_region     as activeRegion,
        l.created_at        as likedAt
      from fp_60 l
      join fp_100 u on u.username = l.username
      where l.feed_id = :feedId
        and l.use_yn = 'Y'
        and l.deleted_at is null
      order by l.created_at desc nulls last, u.user_id desc
      limit :limit offset :offset
      """, nativeQuery = true)
  List<LikeUserRow> findActiveLikeUsers(
          @Param("feedId") Integer feedId,
          @Param("limit") int limit,
          @Param("offset") int offset
  );

  interface LikeUserRow {
    Integer getUserId();
    String getUsername();
    String getNickname();
    String getProfileImageUrl();
    String getActiveRegion();
    java.sql.Timestamp getLikedAt();
  }
}
