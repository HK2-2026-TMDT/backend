package vn.io.sanmaymac.modules.message.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import vn.io.sanmaymac.modules.message.dto.WorkshopMessageResponseRecord;
import vn.io.sanmaymac.modules.message.dto.WorkshopMessageThreadResponseRecord;

@Service
public class ChatEventPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public ChatEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishMessage(Long threadId, WorkshopMessageResponseRecord message) {
        messagingTemplate.convertAndSend("/topic/chat." + threadId, message);
    }

    public void publishThreadUpdate(String recipientEmail, WorkshopMessageThreadResponseRecord thread) {
        messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/thread-updates", thread);
    }
}
