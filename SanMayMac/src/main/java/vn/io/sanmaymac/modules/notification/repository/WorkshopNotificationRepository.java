package vn.io.sanmaymac.modules.notification.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.io.sanmaymac.modules.notification.entity.WorkshopNotificationEntity;

public interface WorkshopNotificationRepository extends JpaRepository<WorkshopNotificationEntity, Long> {
    List<WorkshopNotificationEntity> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    Optional<WorkshopNotificationEntity> findByIdAndRecipientId(Long id, Long recipientId);
}