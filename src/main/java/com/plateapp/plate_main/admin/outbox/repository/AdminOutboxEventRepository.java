package com.plateapp.plate_main.admin.outbox.repository;

import com.plateapp.plate_main.admin.outbox.entity.AdminOutboxEvent;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminOutboxEventRepository extends JpaRepository<AdminOutboxEvent, Long> {

    List<AdminOutboxEvent> findByStatusAndAvailableAtLessThanEqualOrderByIdAsc(
            String status,
            OffsetDateTime availableAt,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from AdminOutboxEvent event where event.id = :id")
    AdminOutboxEvent findByIdForUpdate(@Param("id") Long id);
}
