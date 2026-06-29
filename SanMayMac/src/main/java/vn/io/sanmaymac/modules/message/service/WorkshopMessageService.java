package vn.io.sanmaymac.modules.message.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.common.utils.SecurityUtils;
import vn.io.sanmaymac.modules.message.dto.WorkshopMessageResponseRecord;
import vn.io.sanmaymac.modules.message.dto.WorkshopMessageThreadDetailResponseRecord;
import vn.io.sanmaymac.modules.message.dto.WorkshopMessageThreadResponseRecord;
import vn.io.sanmaymac.modules.message.entity.WorkshopMessageEntity;
import vn.io.sanmaymac.modules.message.entity.WorkshopMessageThreadEntity;
import vn.io.sanmaymac.modules.message.repository.WorkshopMessageRepository;
import vn.io.sanmaymac.modules.message.repository.WorkshopMessageThreadRepository;
import vn.io.sanmaymac.modules.notification.service.WorkshopNotificationService;
import vn.io.sanmaymac.modules.order.entity.OrderEntity;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;

@Service
@Transactional
public class WorkshopMessageService {
    private final WorkshopMessageThreadRepository threadRepository;
    private final WorkshopMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final WorkshopNotificationService notificationService;

    public WorkshopMessageService(
            WorkshopMessageThreadRepository threadRepository,
            WorkshopMessageRepository messageRepository,
            UserRepository userRepository,
            WorkshopNotificationService notificationService) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public WorkshopMessageThreadEntity createThreadForOrder(OrderEntity order) {
        if (order == null || order.getId() == null || order.getCustomer() == null || order.getWorkshop() == null) {
            throw new IllegalArgumentException("Order is required");
        }
        return threadRepository.findByOrderId(order.getId())
                .orElseGet(() -> threadRepository.save(WorkshopMessageThreadEntity.builder()
                        .order(order)
                        .customer(order.getCustomer())
                        .workshop(order.getWorkshop())
                        .lastMessageAt(Instant.now())
                        .build()));
    }

    public List<WorkshopMessageThreadResponseRecord> listMyThreads() {
        UserEntity currentUser = getCurrentUser();
        return threadRepository.findMyThreads(currentUser.getId())
                .stream()
                .map(this::toThreadResponse)
                .toList();
    }

    public WorkshopMessageThreadDetailResponseRecord getThread(Long threadId) {
        WorkshopMessageThreadEntity thread = getThreadForCurrentUser(threadId);
        return new WorkshopMessageThreadDetailResponseRecord(
                toThreadResponse(thread),
                messageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId())
                        .stream()
                        .map(this::toMessageResponse)
                        .toList());
    }

    public WorkshopMessageThreadDetailResponseRecord sendMessage(Long threadId, String content) {
        UserEntity currentUser = getCurrentUser();
        WorkshopMessageThreadEntity thread = getThreadForCurrentUser(threadId);
        WorkshopMessageEntity message = WorkshopMessageEntity.builder()
                .thread(thread)
                .sender(currentUser)
                .content(content)
                .build();
        messageRepository.save(message);
        thread.setLastMessageAt(Instant.now());
        threadRepository.save(thread);

        UserEntity recipient = currentUser.getId().equals(thread.getCustomer().getId())
                ? thread.getWorkshop()
                : thread.getCustomer();
        notificationService.createNotification(
                recipient.getId(),
                "MESSAGE",
                "Tin nhắn mới",
                content,
                "/api/workshop/messages/threads/" + thread.getId(),
                thread.getId());

        return getThread(threadId);
    }

    private UserEntity getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Unauthenticated");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private WorkshopMessageThreadEntity getThreadForCurrentUser(Long threadId) {
        UserEntity currentUser = getCurrentUser();
        return threadRepository.findByIdAndParticipant(threadId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Thread not found"));
    }

    private WorkshopMessageThreadResponseRecord toThreadResponse(WorkshopMessageThreadEntity thread) {
        List<WorkshopMessageEntity> messages = messageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId());
        String lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1).getContent();
        return new WorkshopMessageThreadResponseRecord(
                thread.getId(),
                thread.getOrder() != null ? thread.getOrder().getId() : null,
                thread.getCustomer() != null ? thread.getCustomer().getId() : null,
                thread.getWorkshop() != null ? thread.getWorkshop().getId() : null,
                lastMessage,
                thread.getLastMessageAt());
    }

    private WorkshopMessageResponseRecord toMessageResponse(WorkshopMessageEntity message) {
        return new WorkshopMessageResponseRecord(
                message.getId(),
                message.getSender() != null ? message.getSender().getId() : null,
                message.getContent(),
                message.getCreatedAt());
    }
}