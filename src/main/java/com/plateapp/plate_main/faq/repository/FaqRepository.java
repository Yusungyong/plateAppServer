package com.plateapp.plate_main.faq.repository;

import com.plateapp.plate_main.faq.entity.Fp900Faq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FaqRepository extends JpaRepository<Fp900Faq, Integer> {

    @Query("""
        select f
        from Fp900Faq f
        where f.statusCode = 'published'
          and (:category is null or f.category = :category)
          and (:keyword is null or :keyword = '' or lower(f.title) like lower(concat('%', :keyword, '%')))
    """)
    Page<Fp900Faq> findPublishedFaqs(
        @Param("category") String category,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Query("""
        select f
        from Fp900Faq f
        where f.faqId = :faqId
          and f.statusCode = 'published'
    """)
    Fp900Faq findPublishedByFaqId(@Param("faqId") Integer faqId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Fp900Faq f
        set f.viewCount = coalesce(f.viewCount, 0) + 1
        where f.faqId = :faqId
          and f.statusCode = 'published'
    """)
    int incrementViewCount(@Param("faqId") Integer faqId);
}
