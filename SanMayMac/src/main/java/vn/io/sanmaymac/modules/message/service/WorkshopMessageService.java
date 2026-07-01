package vn.io.sanmaymac.modules.message.service;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.common.enums.Role;
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
import vn.io.sanmaymac.modules.user.entity.WorkshopProfileEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopProfileRepository;

@Service
@Transactional
public class WorkshopMessageService {
    private final WorkshopMessageThreadRepository threadRepository;
    private final WorkshopMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final WorkshopProfileRepository workshopProfileRepository;
    private final WorkshopNotificationService notificationService;
    private final ChatEventPublisher chatEventPublisher;

    public WorkshopMessageService(
            WorkshopMessageThreadRepository threadRepository,
            WorkshopMessageRepository messageRepository,
            UserRepository userRepository,
            WorkshopProfileRepository workshopProfileRepository,
            WorkshopNotificationService notificationService,
            ChatEventPublisher chatEventPublisher) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.workshopProfileRepository = workshopProfileRepository;
        this.notificationService = notificationService;
        this.chatEventPublisher = chatEventPublisher;
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
        List<WorkshopMessageThreadEntity> threads = isAdmin(currentUser)
                ? threadRepository.findAllForAdmin()
                : threadRepository.findMyThreads(currentUser.getId());
        return threads.stream().map(thread -> toThreadResponse(thread, currentUser)).toList();
    }

    public WorkshopMessageThreadDetailResponseRecord getThread(Long threadId) {
        UserEntity currentUser = getCurrentUser();
        WorkshopMessageThreadEntity thread = getAccessibleThread(threadId, currentUser);
        return new WorkshopMessageThreadDetailResponseRecord(
                toThreadResponse(thread, currentUser),
                messageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId())
                        .stream()
                        .map(message -> toMessageResponse(message, thread.getId()))
                        .toList());
    }

    public WorkshopMessageThreadDetailResponseRecord sendMessage(Long threadId, String content) {
        UserEntity currentUser = getCurrentUser();
        WorkshopMessageThreadEntity thread = getAccessibleThread(threadId, currentUser);
        WorkshopMessageEntity message = WorkshopMessageEntity.builder()
                .thread(thread)
                .sender(currentUser)
                .content(content)
                .build();
        messageRepository.save(message);
        thread.setLastMessageAt(Instant.now());
        threadRepository.save(thread);

        WorkshopMessageResponseRecord messageDto = toMessageResponse(message, thread.getId());
        WorkshopMessageThreadResponseRecord threadDto = toThreadResponse(thread, currentUser);

        chatEventPublisher.publishMessage(threadId, messageDto);
        notifyParticipants(thread, currentUser, content, threadDto);

        return getThread(threadId);
    }

    public WorkshopMessageThreadDetailResponseRecord getThreadByOrderId(Long orderId) {
        UserEntity currentUser = getCurrentUser();
        WorkshopMessageThreadEntity thread = threadRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found"));
        getAccessibleThread(thread.getId(), currentUser);
        return getThread(thread.getId());
    }

    private void notifyParticipants(
            WorkshopMessageThreadEntity thread,
            UserEntity sender,
            String content,
            WorkshopMessageThreadResponseRecord threadDto) {
        UserEntity customer = thread.getCustomer();
        UserEntity workshop = thread.getWorkshop();

        if (customer != null && !customer.getId().equals(sender.getId())) {
            pushNotification(customer, thread, content);
            chatEventPublisher.publishThreadUpdate(customer.getEmail(), threadDto);
        }
        if (workshop != null && !workshop.getId().equals(sender.getId())) {
            pushNotification(workshop, thread, content);
            chatEventPublisher.publishThreadUpdate(workshop.getEmail(), threadDto);
        }

        userRepository.findByRole(Role.ADMIN).forEach(admin -> {
            if (!admin.getId().equals(sender.getId())) {
                chatEventPublisher.publishThreadUpdate(admin.getEmail(), threadDto);
            }
        });
    }

    private void pushNotification(UserEntity recipient, WorkshopMessageThreadEntity thread, String content) {
        notificationService.createNotification(
                recipient.getId(),
                "MESSAGE",
                "Tin nhắn mới",
                content,
                "/messages?thread=" + thread.getId(),
                thread.getId());
    }

    private UserEntity getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Unauthenticated");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private WorkshopMessageThreadEntity getAccessibleThread(Long threadId, UserEntity currentUser) {
        if (isAdmin(currentUser)) {
            return threadRepository.findById(threadId)
                    .orElseThrow(() -> new IllegalArgumentException("Thread not found"));
        }
        return threadRepository.findByIdAndParticipant(threadId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Thread not found"));
    }

    private boolean isAdmin(UserEntity user) {
        return Role.ADMIN.equals(user.getRole());
    }

    private WorkshopMessageThreadResponseRecord toThreadResponse(
            WorkshopMessageThreadEntity thread,
            UserEntity viewer) {
        List<WorkshopMessageEntity> messages = messageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId());
        String lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1).getContent();

        String customerName = thread.getCustomer() != null ? thread.getCustomer().getFullName() : null;
        String workshopName = resolveWorkshopName(thread.getWorkshop());
        String participantName;
        String participantAvatarUrl;

        if (isAdmin(viewer)) {
            participantName = (customerName != null ? customerName : "Khách") + " ↔ "
                    + (workshopName != null ? workshopName : "Xưởng");
            participantAvatarUrl = null;
        } else if (viewer.getId().equals(thread.getCustomer() != null ? thread.getCustomer().getId() : null)) {
            participantName = workshopName;
            participantAvatarUrl = thread.getWorkshop() != null ? thread.getWorkshop().getAvatarUrl() : null;
        } else {
            participantName = customerName;
            participantAvatarUrl = thread.getCustomer() != null ? thread.getCustomer().getAvatarUrl() : null;
        }

        String subject = thread.getOrder() != null ? "Đơn hàng #" + thread.getOrder().getId() : "Hội thoại #" + thread.getId();

        return new WorkshopMessageThreadResponseRecord(
                thread.getId(),
                thread.getOrder() != null ? thread.getOrder().getId() : null,
                thread.getCustomer() != null ? thread.getCustomer().getId() : null,
                customerName,
                thread.getWorkshop() != null ? thread.getWorkshop().getId() : null,
                workshopName,
                participantName,
                participantAvatarUrl,
                subject,
                lastMessage,
                thread.getLastMessageAt());
    }

    private WorkshopMessageResponseRecord toMessageResponse(WorkshopMessageEntity message, Long threadId) {
        UserEntity sender = message.getSender();
        return new WorkshopMessageResponseRecord(
                message.getId(),
                threadId,
                sender != null ? sender.getId() : null,
                sender != null ? sender.getFullName() : null,
                sender != null && sender.getRole() != null ? sender.getRole().name() : null,
                sender != null ? sender.getAvatarUrl() : null,
                message.getContent(),
                message.getCreatedAt());
    }

    private String resolveWorkshopName(UserEntity workshop) {
        if (workshop == null) {
            return null;
        }
        return workshopProfileRepository.findById(workshop.getId())
                .map(WorkshopProfileEntity::getShopName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(workshop.getFullName());
    }
}
