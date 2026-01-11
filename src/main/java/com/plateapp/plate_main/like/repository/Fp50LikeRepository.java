package com.plateapp.plate_main.like.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.like.entity.Fp50Like;
import com.plateapp.plate_main.like.entity.Fp50LikeId;

public interface Fp50LikeRepository extends JpaRepository<Fp50Like, Fp50LikeId> {

  // ✅ 기존 CRUD/조회용 (LikeService에서 사용)
  Optional<Fp50Like> findByIdUsernameAndIdStoreId(String username, Integer storeId);

  long countByIdStoreIdAndUseYn(Integer storeId, String useYn);

  boolean existsByIdUsernameAndIdStoreIdAndUseYn(String username, Integer storeId, String useYn);

  List<Fp50Like> findByIdUsernameAndUseYn(String username, String useYn);

  // ✅ (A) storeIds별 좋아요 카운트 배치
  @Query("""
    select l.id.storeId as storeId, count(l) as cnt
    from Fp50Like l
    where l.useYn = 'Y'
      and l.id.storeId in :storeIds
    group by l.id.storeId
  """)
  List<StoreLikeCount> countActiveByStoreIds(@Param("storeIds") List<Integer> storeIds);

  interface StoreLikeCount {
    Integer getStoreId();
    Long getCnt();
  }

  // ✅ (B) 내가 좋아요한 storeId 목록 배치 (피드 storeIds 중에서만)
  @Query("""
    select l.id.storeId
    from Fp50Like l
    where l.useYn = 'Y'
      and l.id.username = :username
      and l.id.storeId in :storeIds
  """)
  List<Integer> findMyActiveLikedStoreIds(
      @Param("username") String username,
      @Param("storeIds") List<Integer> storeIds
  );

  @Query(value = """
      select
        u.user_id           as userId,
        u.username          as username,
        u.nick_name         as nickname,
        u.profile_image_url as profileImageUrl,
        u.active_region     as activeRegion,
        l.created_at        as likedAt
      from fp_50 l
      join fp_100 u on u.username = l.username
      where l.store_id = :storeId
        and l.use_yn = 'Y'
        and l.deleted_at is null
      order by l.created_at desc nulls last, u.user_id desc
      limit :limit offset :offset
      """, nativeQuery = true)
  List<LikeUserRow> findActiveLikeUsers(
          @Param("storeId") Integer storeId,
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
