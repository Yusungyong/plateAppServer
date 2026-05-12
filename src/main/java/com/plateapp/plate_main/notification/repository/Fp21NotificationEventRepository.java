package com.plateapp.plate_main.notification.repository;

import com.plateapp.plate_main.notification.entity.Fp21NotificationEvent;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Fp21NotificationEventRepository extends JpaRepository<Fp21NotificationEvent, Long> {
    List<Fp21NotificationEvent> findByEventIdIn(Collection<Long> eventIds);
}
