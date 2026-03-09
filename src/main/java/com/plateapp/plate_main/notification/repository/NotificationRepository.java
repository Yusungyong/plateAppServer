package com.plateapp.plate_main.notification.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.notification.entity.Fp20Notification;

public interface NotificationRepository extends JpaRepository<Fp20Notification, Long> {

  Page<Fp20Notification> findByReceiverId(String receiverId, Pageable pageable);

  @Query("""
      select n
      from Fp20Notification n
      where n.receiverId = :receiverId
        and (n.isRead = false or n.isRead is null)
  """)
  Page<Fp20Notification> findUnreadByReceiverId(@Param("receiverId") String receiverId, Pageable pageable);

  @Query("""
      select count(n)
      from Fp20Notification n
      where n.receiverId = :receiverId
        and (n.isRead = false or n.isRead is null)
  """)
  long countUnreadByReceiverId(@Param("receiverId") String receiverId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Fp20Notification n
         set n.isRead = true
       where n.id = :id
         and n.receiverId = :receiverId
  """)
  int markRead(@Param("id") Long id,
               @Param("receiverId") String receiverId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Fp20Notification n
         set n.isRead = true
       where n.receiverId = :receiverId
         and (n.isRead = false or n.isRead is null)
  """)
  int markAllRead(@Param("receiverId") String receiverId);

  int deleteByIdAndReceiverId(Long id, String receiverId);

  int deleteByReceiverId(String receiverId);
}
