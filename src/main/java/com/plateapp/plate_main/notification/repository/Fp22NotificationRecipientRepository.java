package com.plateapp.plate_main.notification.repository;

import com.plateapp.plate_main.notification.entity.Fp22NotificationRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Fp22NotificationRecipientRepository extends JpaRepository<Fp22NotificationRecipient, Long> {

    @Query("""
        select n
        from Fp22NotificationRecipient n
        where n.recipientUserId = :recipientUserId
          and n.isDeleted = false
        order by n.createdAt desc
    """)
    Page<Fp22NotificationRecipient> findActiveByRecipientUserId(@Param("recipientUserId") Integer recipientUserId, Pageable pageable);

    @Query("""
        select n
        from Fp22NotificationRecipient n
        where n.recipientUserId = :recipientUserId
          and n.isDeleted = false
          and n.isRead = false
        order by n.createdAt desc
    """)
    Page<Fp22NotificationRecipient> findUnreadActiveByRecipientUserId(@Param("recipientUserId") Integer recipientUserId, Pageable pageable);

    @Query("""
        select count(n)
        from Fp22NotificationRecipient n
        where n.recipientUserId = :recipientUserId
          and n.isDeleted = false
          and n.isRead = false
    """)
    long countUnreadActiveByRecipientUserId(@Param("recipientUserId") Integer recipientUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Fp22NotificationRecipient n
           set n.isRead = true,
               n.readAt = CURRENT_TIMESTAMP
         where n.notificationId = :notificationId
           and n.recipientUserId = :recipientUserId
           and n.isDeleted = false
    """)
    int markRead(@Param("notificationId") Long notificationId, @Param("recipientUserId") Integer recipientUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Fp22NotificationRecipient n
           set n.isRead = true,
               n.readAt = CURRENT_TIMESTAMP
         where n.recipientUserId = :recipientUserId
           and n.isDeleted = false
           and n.isRead = false
    """)
    int markAllRead(@Param("recipientUserId") Integer recipientUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Fp22NotificationRecipient n
           set n.isDeleted = true,
               n.deletedAt = CURRENT_TIMESTAMP
         where n.notificationId = :notificationId
           and n.recipientUserId = :recipientUserId
           and n.isDeleted = false
    """)
    int softDeleteOne(@Param("notificationId") Long notificationId, @Param("recipientUserId") Integer recipientUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Fp22NotificationRecipient n
           set n.isDeleted = true,
               n.deletedAt = CURRENT_TIMESTAMP
         where n.recipientUserId = :recipientUserId
           and n.isDeleted = false
    """)
    int softDeleteAll(@Param("recipientUserId") Integer recipientUserId);
}
