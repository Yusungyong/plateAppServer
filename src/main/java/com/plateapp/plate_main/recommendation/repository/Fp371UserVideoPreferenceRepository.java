package com.plateapp.plate_main.recommendation.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.recommendation.entity.Fp371UserVideoPreference;

public interface Fp371UserVideoPreferenceRepository extends JpaRepository<Fp371UserVideoPreference, Long> {

    Optional<Fp371UserVideoPreference> findByUsernameAndModelVersionAndSubjectTypeAndSubjectKey(
            String username,
            String modelVersion,
            String subjectType,
            String subjectKey
    );

    Optional<Fp371UserVideoPreference> findByGuestIdAndModelVersionAndSubjectTypeAndSubjectKey(
            String guestId,
            String modelVersion,
            String subjectType,
            String subjectKey
    );

    @Query("""
        select p
        from Fp371UserVideoPreference p
        where p.username = :username
          and p.modelVersion = :modelVersion
          and p.subjectType in :subjectTypes
          and p.subjectKey in :subjectKeys
    """)
    List<Fp371UserVideoPreference> findMatchingUsernamePreferences(
            @Param("username") String username,
            @Param("modelVersion") String modelVersion,
            @Param("subjectTypes") Collection<String> subjectTypes,
            @Param("subjectKeys") Collection<String> subjectKeys
    );

    @Query("""
        select p
        from Fp371UserVideoPreference p
        where p.guestId = :guestId
          and p.modelVersion = :modelVersion
          and p.subjectType in :subjectTypes
          and p.subjectKey in :subjectKeys
    """)
    List<Fp371UserVideoPreference> findMatchingGuestPreferences(
            @Param("guestId") String guestId,
            @Param("modelVersion") String modelVersion,
            @Param("subjectTypes") Collection<String> subjectTypes,
            @Param("subjectKeys") Collection<String> subjectKeys
    );

    @Modifying
    @Query(value = """
        insert into fp_371 (
            user_id,
            username,
            is_guest,
            guest_id,
            subject_type,
            subject_key,
            score,
            positive_count,
            negative_count,
            impression_count,
            last_event_at,
            model_version,
            created_at,
            updated_at
        )
        values (
            :userId,
            :username,
            :isGuest,
            :guestId,
            :subjectType,
            :subjectKey,
            :scoreDelta,
            :positiveDelta,
            :negativeDelta,
            :impressionDelta,
            :lastEventAt,
            :modelVersion,
            now(),
            now()
        )
        on conflict (username, model_version, subject_type, subject_key)
            where username is not null
        do update set
            score = fp_371.score + excluded.score,
            positive_count = fp_371.positive_count + excluded.positive_count,
            negative_count = fp_371.negative_count + excluded.negative_count,
            impression_count = fp_371.impression_count + excluded.impression_count,
            last_event_at = greatest(coalesce(fp_371.last_event_at, excluded.last_event_at), excluded.last_event_at),
            updated_at = now()
        """, nativeQuery = true)
    int upsertUsernamePreference(
            @Param("userId") Integer userId,
            @Param("username") String username,
            @Param("isGuest") boolean isGuest,
            @Param("guestId") String guestId,
            @Param("subjectType") String subjectType,
            @Param("subjectKey") String subjectKey,
            @Param("scoreDelta") BigDecimal scoreDelta,
            @Param("positiveDelta") int positiveDelta,
            @Param("negativeDelta") int negativeDelta,
            @Param("impressionDelta") int impressionDelta,
            @Param("lastEventAt") LocalDateTime lastEventAt,
            @Param("modelVersion") String modelVersion
    );

    @Modifying
    @Query(value = """
        insert into fp_371 (
            user_id,
            username,
            is_guest,
            guest_id,
            subject_type,
            subject_key,
            score,
            positive_count,
            negative_count,
            impression_count,
            last_event_at,
            model_version,
            created_at,
            updated_at
        )
        values (
            :userId,
            :username,
            :isGuest,
            :guestId,
            :subjectType,
            :subjectKey,
            :scoreDelta,
            :positiveDelta,
            :negativeDelta,
            :impressionDelta,
            :lastEventAt,
            :modelVersion,
            now(),
            now()
        )
        on conflict (guest_id, model_version, subject_type, subject_key)
            where guest_id is not null
        do update set
            score = fp_371.score + excluded.score,
            positive_count = fp_371.positive_count + excluded.positive_count,
            negative_count = fp_371.negative_count + excluded.negative_count,
            impression_count = fp_371.impression_count + excluded.impression_count,
            last_event_at = greatest(coalesce(fp_371.last_event_at, excluded.last_event_at), excluded.last_event_at),
            updated_at = now()
        """, nativeQuery = true)
    int upsertGuestPreference(
            @Param("userId") Integer userId,
            @Param("username") String username,
            @Param("isGuest") boolean isGuest,
            @Param("guestId") String guestId,
            @Param("subjectType") String subjectType,
            @Param("subjectKey") String subjectKey,
            @Param("scoreDelta") BigDecimal scoreDelta,
            @Param("positiveDelta") int positiveDelta,
            @Param("negativeDelta") int negativeDelta,
            @Param("impressionDelta") int impressionDelta,
            @Param("lastEventAt") LocalDateTime lastEventAt,
            @Param("modelVersion") String modelVersion
    );
}
