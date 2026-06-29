package vn.io.sanmaymac.modules.notification.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.common.utils.SecurityUtils;
import vn.io.sanmaymac.modules.notification.dto.WorkshopNotificationResponseRecord;
import vn.io.sanmaymac.modules.notification.entity.WorkshopNotificationEntity;
import vn.io.sanmaymac.modules.notification.repository.WorkshopNotificationRepository;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;

@Service
@Transactional
public class WorkshopNotificationService {
    private final WorkshopNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public WorkshopNotificationService(
            WorkshopNotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public WorkshopNotificationResponseRecord createNotification(
            Long recipientId,
            String type,
            String title,
            String body,
            String referenceUrl,
            Long referenceId) {
        UserEntity recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        WorkshopNotificationEntity entity = WorkshopNotificationEntity.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .body(body)
                .referenceUrl(referenceUrl)
                .referenceId(referenceId)
                .build();
        return toResponse(notificationRepository.save(entity));
    }

    public List<WorkshopNotificationResponseRecord> listMyNotifications() {
        UserEntity currentUser = getCurrentUser();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public long getUnreadCount() {
        UserEntity currentUser = getCurrentUser();
        return notificationRepository.countByRecipientIdAndReadAtIsNull(currentUser.getId());
    }

    public WorkshopNotificationResponseRecord markAsRead(Long notificationId) {
        UserEntity currentUser = getCurrentUser();
        WorkshopNotificationEntity entity = notificationRepository.findByIdAndRecipientId(notificationId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        entity.setReadAt(Instant.now());
        return toResponse(notificationRepository.save(entity));
    }

    public void deleteMyNotification(Long notificationId) {
        UserEntity currentUser = getCurrentUser();
        WorkshopNotificationEntity entity = notificationRepository.findByIdAndRecipientId(notificationId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notificationRepository.delete(entity);
    }

    private UserEntity getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Unauthenticated");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private WorkshopNotificationResponseRecord toResponse(WorkshopNotificationEntity entity) {
        return new WorkshopNotificationResponseRecord(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getBody(),
                entity.getReferenceUrl(),
                entity.getReferenceId(),
                entity.getReadAt(),
                entity.getCreatedAt());
    }
}