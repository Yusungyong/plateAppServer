package com.plateapp.plate_main.admin.feedmoderation.repository;

import com.plateapp.plate_main.admin.feedmoderation.entity.AdminFeedModeration;
import com.plateapp.plate_main.feed.entity.Fp400Feed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminFeedModerationRepository extends JpaRepository<AdminFeedModeration, Integer> {
    @Query("""
        select f from Fp400Feed f
        left join AdminFeedModeration m on m.feedId = f.feedNo
        where (:keyword is null or lower(f.content) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(f.feedTitle, '')) like lower(concat('%', :keyword, '%'))
               or lower(f.username) like lower(concat('%', :keyword, '%')))
          and (:visibility is null or f.useYn = :visibility)
          and (:recommended is null or coalesce(m.recommended, false) = :recommended)
        """)
    Page<Fp400Feed> searchFeeds(
            @Param("keyword") String keyword,
            @Param("visibility") String visibility,
            @Param("recommended") Boolean recommended,
            Pageable pageable
    );
}
