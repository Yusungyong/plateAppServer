package com.plateapp.plate_main.notification.repository;

import com.plateapp.plate_main.notification.entity.Fp23NotificationTarget;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Fp23NotificationTargetRepository extends JpaRepository<Fp23NotificationTarget, Long> {
    List<Fp23NotificationTarget> findByEventIdIn(Collection<Long> eventIds);
}
