package vn.io.sanmaymac.modules.message.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.io.sanmaymac.common.dto.ApiResponse;
import vn.io.sanmaymac.modules.workshop.dto.WorkshopMessageCreateRequest;
import vn.io.sanmaymac.modules.message.dto.WorkshopMessageThreadDetailResponseRecord;
import vn.io.sanmaymac.modules.message.dto.WorkshopMessageThreadResponseRecord;
import vn.io.sanmaymac.modules.message.service.WorkshopMessageService;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final WorkshopMessageService messageService;

    public MessageController(WorkshopMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/threads")
    @PreAuthorize("hasAnyRole('CUSTOMER','WORKSHOP','ADMIN')")
    public ResponseEntity<ApiResponse<List<WorkshopMessageThreadResponseRecord>>> listThreads() {
        return ResponseEntity.ok(ApiResponse.success("OK", messageService.listMyThreads()));
    }

    @GetMapping("/threads/{threadId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','WORKSHOP','ADMIN')")
    public ResponseEntity<ApiResponse<WorkshopMessageThreadDetailResponseRecord>> getThread(@PathVariable Long threadId) {
        return ResponseEntity.ok(ApiResponse.success("OK", messageService.getThread(threadId)));
    }

    @GetMapping("/threads/by-order/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','WORKSHOP','ADMIN')")
    public ResponseEntity<ApiResponse<WorkshopMessageThreadDetailResponseRecord>> getThreadByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("OK", messageService.getThreadByOrderId(orderId)));
    }

    @PostMapping("/threads/{threadId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','WORKSHOP','ADMIN')")
    public ResponseEntity<ApiResponse<WorkshopMessageThreadDetailResponseRecord>> sendMessage(
            @PathVariable Long threadId,
            @Valid @RequestBody WorkshopMessageCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Sent", messageService.sendMessage(threadId, request.content())));
    }
}
