package vn.io.sanmaymac.modules.complaint.service;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.common.enums.ComplaintStatus;
import vn.io.sanmaymac.common.enums.DisputeStatus;
import vn.io.sanmaymac.common.utils.SecurityUtils;
import vn.io.sanmaymac.modules.complaint.dto.DisputeRequestInfoRequest;
import vn.io.sanmaymac.modules.complaint.dto.DisputeResponseRecord;
import vn.io.sanmaymac.modules.complaint.dto.DisputeRulingRequest;
import vn.io.sanmaymac.modules.complaint.dto.ViolationRecordRequest;
import vn.io.sanmaymac.modules.complaint.entity.ComplaintEntity;
import vn.io.sanmaymac.modules.complaint.entity.ComplaintImageEntity;
import vn.io.sanmaymac.modules.complaint.entity.DisputeEntity;
import vn.io.sanmaymac.modules.complaint.entity.WorkshopViolationRecordEntity;
import vn.io.sanmaymac.modules.complaint.repository.ComplaintImageRepository;
import vn.io.sanmaymac.modules.complaint.repository.ComplaintRepository;
import vn.io.sanmaymac.modules.complaint.repository.DisputeRepository;
import vn.io.sanmaymac.modules.complaint.repository.WorkshopViolationRecordRepository;
import vn.io.sanmaymac.modules.finance.service.FinanceService;
import vn.io.sanmaymac.modules.order.entity.OrderEntity;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.entity.WorkshopProfileEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopProfileRepository;

@Service
@Transactional
public class DisputeService {
    private final DisputeRepository disputeRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintImageRepository complaintImageRepository;
    private final WorkshopViolationRecordRepository violationRecordRepository;
    private final FinanceService financeService;
    private final UserRepository userRepository;
    private final WorkshopProfileRepository workshopProfileRepository;

    public DisputeService(
            DisputeRepository disputeRepository,
            ComplaintRepository complaintRepository,
            ComplaintImageRepository complaintImageRepository,
            WorkshopViolationRecordRepository violationRecordRepository,
            FinanceService financeService,
            UserRepository userRepository,
            WorkshopProfileRepository workshopProfileRepository) {
        this.disputeRepository = disputeRepository;
        this.complaintRepository = complaintRepository;
        this.complaintImageRepository = complaintImageRepository;
        this.violationRecordRepository = violationRecordRepository;
        this.financeService = financeService;
        this.userRepository = userRepository;
        this.workshopProfileRepository = workshopProfileRepository;
    }

    public DisputeResponseRecord escalateFromComplaint(Long complaintId) {
        ComplaintEntity complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));

        if (disputeRepository.findByComplaintId(complaintId).isPresent()) {
            throw new IllegalStateException("Dispute already exists for this complaint");
        }

        complaint.setStatus(ComplaintStatus.ESCALATED);
        complaintRepository.save(complaint);

        DisputeEntity dispute = DisputeEntity.builder()
                .complaint(complaint)
                .order(complaint.getOrder())
                .customer(complaint.getCustomer())
                .workshop(complaint.getWorkshop())
                .status(DisputeStatus.OPEN)
                .refundProcessed(false)
                .escrowReleased(false)
                .violationRecorded(false)
                .build();

        return toResponse(disputeRepository.save(dispute));
    }

    public Page<DisputeResponseRecord> listDisputesForAdmin(DisputeStatus status, Pageable pageable) {
        if (status == null) {
            return disputeRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
        }
        return disputeRepository.findByStatusOrderByCreatedAtDesc(status, pageable).map(this::toResponse);
    }

    public DisputeResponseRecord getDisputeForAdmin(Long disputeId) {
        DisputeEntity dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
        return toResponse(dispute);
    }

    public DisputeResponseRecord requestAdditionalInfo(Long disputeId, DisputeRequestInfoRequest request) {
        DisputeEntity dispute = getDisputeOrThrow(disputeId);
        dispute.setAdminRequestInfo(request.message().trim());
        dispute.setStatus(DisputeStatus.AWAITING_INFO);
        return toResponse(disputeRepository.save(dispute));
    }

    public DisputeResponseRecord submitRuling(Long disputeId, DisputeRulingRequest request) {
        DisputeEntity dispute = getDisputeOrThrow(disputeId);
        dispute.setRuling(request.ruling().trim());
        dispute.setStatus(DisputeStatus.JUDGED);
        return toResponse(disputeRepository.save(dispute));
    }

    public DisputeResponseRecord refundCustomer(Long disputeId) {
        DisputeEntity dispute = getDisputeOrThrow(disputeId);
        if (Boolean.TRUE.equals(dispute.getRefundProcessed())) {
            throw new IllegalStateException("Refund already processed");
        }
        OrderEntity order = dispute.getOrder();
        financeService.refundOrder(order);
        dispute.setRefundProcessed(true);
        dispute.setStatus(DisputeStatus.REFUNDED);
        dispute.setResolvedAt(Instant.now());
        return toResponse(disputeRepository.save(dispute));
    }

    public DisputeResponseRecord releaseToWorkshop(Long disputeId) {
        DisputeEntity dispute = getDisputeOrThrow(disputeId);
        if (Boolean.TRUE.equals(dispute.getEscrowReleased())) {
            throw new IllegalStateException("Escrow already released to workshop");
        }
        OrderEntity order = dispute.getOrder();
        financeService.releaseEscrowForOrder(order);
        dispute.setEscrowReleased(true);
        dispute.setStatus(DisputeStatus.RELEASED_TO_WORKSHOP);
        dispute.setResolvedAt(Instant.now());
        return toResponse(disputeRepository.save(dispute));
    }

    public DisputeResponseRecord saveViolationRecord(Long disputeId, ViolationRecordRequest request) {
        DisputeEntity dispute = getDisputeOrThrow(disputeId);
        UserEntity admin = getCurrentUser();

        WorkshopViolationRecordEntity record = WorkshopViolationRecordEntity.builder()
                .workshop(dispute.getWorkshop())
                .dispute(dispute)
                .order(dispute.getOrder())
                .description(request.description().trim())
                .createdByAdmin(admin)
                .build();
        violationRecordRepository.save(record);

        dispute.setViolationRecorded(true);
        dispute.setViolationNote(request.description().trim());
        if (dispute.getResolvedAt() == null) {
            dispute.setStatus(DisputeStatus.CLOSED);
            dispute.setResolvedAt(Instant.now());
        }
        return toResponse(disputeRepository.save(dispute));
    }

    public DisputeResponseRecord closeDispute(Long disputeId) {
        DisputeEntity dispute = getDisputeOrThrow(disputeId);
        dispute.setStatus(DisputeStatus.CLOSED);
        dispute.setResolvedAt(Instant.now());
        return toResponse(disputeRepository.save(dispute));
    }

    DisputeResponseRecord toResponse(DisputeEntity dispute) {
        ComplaintEntity complaint = dispute.getComplaint();
        List<String> imageUrls = complaintImageRepository.findByComplaintIdOrderByIdAsc(complaint.getId()).stream()
                .map(ComplaintImageEntity::getImageUrl)
                .toList();

        UserEntity customer = dispute.getCustomer();
        UserEntity workshop = dispute.getWorkshop();

        return new DisputeResponseRecord(
                dispute.getId(),
                complaint.getId(),
                dispute.getOrder().getId(),
                customer != null ? customer.getId() : null,
                customer != null ? customer.getFullName() : null,
                workshop != null ? workshop.getId() : null,
                workshop != null ? resolveWorkshopName(workshop) : null,
                complaint.getReason(),
                imageUrls,
                complaint.getStatus(),
                dispute.getStatus(),
                dispute.getAdminRequestInfo(),
                dispute.getCustomerSupplement(),
                dispute.getRuling(),
                dispute.getRefundProcessed(),
                dispute.getEscrowReleased(),
                dispute.getViolationRecorded(),
                dispute.getViolationNote(),
                dispute.getResolvedAt(),
                dispute.getCreatedAt(),
                dispute.getUpdatedAt());
    }

    private DisputeEntity getDisputeOrThrow(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));
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
