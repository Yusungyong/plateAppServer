package com.plateapp.plate_main.qna.repository;

import com.plateapp.plate_main.qna.entity.Fp901Qna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QnaRepository extends JpaRepository<Fp901Qna, Integer> {

    @Query("""
        select q
        from Fp901Qna q
        where q.isPublic = true
          and (:category is null or q.category = :category)
    """)
    Page<Fp901Qna> findPublicQna(
        @Param("category") String category,
        Pageable pageable
    );

    @Query("""
        select q
        from Fp901Qna q
        where (:category is null or q.category = :category)
          and (:statusCode is null or q.statusCode = :statusCode)
    """)
    Page<Fp901Qna> findAdminQna(
        @Param("category") String category,
        @Param("statusCode") String statusCode,
        Pageable pageable
    );

    @Query("""
        select q
        from Fp901Qna q
        where q.qnaId = :qnaId
          and q.isPublic = true
    """)
    Fp901Qna findPublicByQnaId(@Param("qnaId") Integer qnaId);
}
