package vn.io.sanmaymac.modules.complaint.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.io.sanmaymac.common.enums.ComplaintStatus;
import vn.io.sanmaymac.common.enums.OrderStatus;
import vn.io.sanmaymac.common.service.MediaStorageService;
import vn.io.sanmaymac.common.utils.SecurityUtils;
import vn.io.sanmaymac.modules.complaint.dto.AdminUpdateComplaintStatusRequest;
import vn.io.sanmaymac.modules.complaint.dto.ComplaintCreateRequest;
import vn.io.sanmaymac.modules.complaint.dto.ComplaintResponseRecord;
import vn.io.sanmaymac.modules.complaint.dto.DisputeResponseRecord;
import vn.io.sanmaymac.modules.complaint.dto.ImageUploadResponseRecord;
import vn.io.sanmaymac.modules.complaint.entity.ComplaintEntity;
import vn.io.sanmaymac.modules.complaint.entity.ComplaintImageEntity;
import vn.io.sanmaymac.modules.complaint.entity.DisputeEntity;
import vn.io.sanmaymac.modules.complaint.repository.ComplaintImageRepository;
import vn.io.sanmaymac.modules.complaint.repository.ComplaintRepository;
import vn.io.sanmaymac.modules.complaint.repository.DisputeRepository;
import vn.io.sanmaymac.modules.order.entity.OrderEntity;
import vn.io.sanmaymac.modules.order.repository.OrderRepository;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.entity.WorkshopProfileEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopProfileRepository;

@Service
@Transactional
public class ComplaintService {
    private final ComplaintRepository complaintRepository;
    private final ComplaintImageRepository complaintImageRepository;
    private final DisputeRepository disputeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MediaStorageService mediaStorageService;
    private final DisputeService disputeService;
    private final WorkshopProfileRepository workshopProfileRepository;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintImageRepository complaintImageRepository,
            DisputeRepository disputeRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            MediaStorageService mediaStorageService,
            DisputeService disputeService,
            WorkshopProfileRepository workshopProfileRepository) {
        this.complaintRepository = complaintRepository;
        this.complaintImageRepository = complaintImageRepository;
        this.disputeRepository = disputeRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.mediaStorageService = mediaStorageService;
        this.disputeService = disputeService;
        this.workshopProfileRepository = workshopProfileRepository;
    }

    public ComplaintResponseRecord createComplaint(ComplaintCreateRequest request) {
        UserEntity customer = getCurrentUser();
        OrderEntity order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        validateOrderForComplaint(order, customer);

        if (complaintRepository.existsByOrderId(order.getId())) {
            throw new IllegalStateException("Order already has a complaint");
        }

        ComplaintEntity complaint = ComplaintEntity.builder()
                .order(order)
                .customer(customer)
                .workshop(order.getWorkshop())
                .reason(request.reason().trim())
                .status(ComplaintStatus.PENDING)
                .build();

        ComplaintEntity saved = complaintRepository.save(complaint);
        saveImages(saved, request.imageUrls());
        return toResponse(saved);
    }

    public ImageUploadResponseRecord uploadEvidence(MultipartFile file) {
        String url = mediaStorageService.store(file, "complaint");
        return new ImageUploadResponseRecord(url);
    }

    public Page<ComplaintResponseRecord> getMyComplaints(Pageable pageable) {
        UserEntity customer = getCurrentUser();
        return complaintRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable)
                .map(this::toResponse);
    }

    public ComplaintResponseRecord getMyComplaint(Long complaintId) {
        UserEntity customer = getCurrentUser();
        ComplaintEntity complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        if (!Objects.equals(complaint.getCustomer().getId(), customer.getId())) {
            throw new IllegalStateException("No permission to view complaint");
        }
        return toResponse(complaint);
    }

    public ComplaintResponseRecord getComplaintByOrder(Long orderId) {
        UserEntity customer = getCurrentUser();
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!Objects.equals(order.getCustomer().getId(), customer.getId())) {
            throw new IllegalStateException("No permission to view complaint");
        }
        return complaintRepository.findByOrderId(orderId)
                .map(this::toResponse)
                .orElse(null);
    }

    public Page<ComplaintResponseRecord> listComplaintsForAdmin(ComplaintStatus status, Pageable pageable) {
        if (status == null) {
            return complaintRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
        }
        return complaintRepository.findByStatusOrderByCreatedAtDesc(status, pageable).map(this::toResponse);
    }

    public ComplaintResponseRecord getComplaintForAdmin(Long complaintId) {
        ComplaintEntity complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        return toResponse(complaint);
    }

    public ComplaintResponseRecord updateComplaintStatus(Long complaintId, AdminUpdateComplaintStatusRequest request) {
        ComplaintEntity complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

        if (ComplaintStatus.ESCALATED.equals(complaint.getStatus())) {
            throw new IllegalStateException("Complaint already escalated to dispute");
        }
        if (ComplaintStatus.ESCALATED.equals(request.status())) {
            throw new IllegalStateException("Use escalate endpoint to create dispute");
        }

        complaint.setStatus(request.status());
        if (request.adminNote() != null && !request.adminNote().isBlank()) {
            complaint.setAdminNote(request.adminNote().trim());
        }
        return toResponse(complaintRepository.save(complaint));
    }

    public DisputeResponseRecord escalateToDispute(Long complaintId) {
        return disputeService.escalateFromComplaint(complaintId);
    }

    private void validateOrderForComplaint(OrderEntity order, UserEntity customer) {
        if (order.getCustomer() == null || !Objects.equals(order.getCustomer().getId(), customer.getId())) {
            throw new IllegalStateException("Order does not belong to current user");
        }
        if (order.getWorkshop() == null) {
            throw new IllegalStateException("Order has no workshop to complain about");
        }
        OrderStatus status = order.getStatus();
        if (!OrderStatus.SHIPPED.equals(status) && !OrderStatus.COMPLETED.equals(status)) {
            throw new IllegalStateException("Complaint is only allowed after order is shipped");
        }
    }

    private void saveImages(ComplaintEntity complaint, List<String> imageUrls) {
        List<ComplaintImageEntity> images = new ArrayList<>();
        for (String url : imageUrls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            images.add(ComplaintImageEntity.builder()
                    .complaint(complaint)
                    .imageUrl(url.trim())
                    .build());
        }
        if (images.isEmpty()) {
            throw new IllegalArgumentException("At least one image is required");
        }
        complaintImageRepository.saveAll(images);
    }

    ComplaintResponseRecord toResponse(ComplaintEntity complaint) {
        List<String> imageUrls = complaintImageRepository.findByComplaintIdOrderByIdAsc(complaint.getId()).stream()
                .map(ComplaintImageEntity::getImageUrl)
                .toList();

        Long disputeId = disputeRepository.findByComplaintId(complaint.getId())
                .map(DisputeEntity::getId)
                .orElse(null);

        UserEntity customer = complaint.getCustomer();
        UserEntity workshop = complaint.getWorkshop();

        return new ComplaintResponseRecord(
                complaint.getId(),
                complaint.getOrder().getId(),
                customer != null ? customer.getId() : null,
                customer != null ? customer.getFullName() : null,
                workshop != null ? workshop.getId() : null,
                workshop != null ? resolveWorkshopName(workshop) : null,
                complaint.getReason(),
                complaint.getStatus(),
                imageUrls,
                complaint.getAdminNote(),
                disputeId,
                complaint.getCreatedAt(),
                complaint.getUpdatedAt());
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

    private UserEntity getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
