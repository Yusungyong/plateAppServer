package com.plateapp.plate_main.comment.repository;

import com.plateapp.plate_main.comment.entity.ImageComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageCommentRepository extends JpaRepository<ImageComment, Integer> {

    Page<ImageComment> findByImageFeedIdAndUseYn(Integer imageFeedId, String useYn, Pageable pageable);
}
