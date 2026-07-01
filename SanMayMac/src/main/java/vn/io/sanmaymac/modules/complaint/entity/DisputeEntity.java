package vn.io.sanmaymac.modules.complaint.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.io.sanmaymac.common.entity.BaseEntity;
import vn.io.sanmaymac.common.enums.DisputeStatus;
import vn.io.sanmaymac.modules.order.entity.OrderEntity;
import vn.io.sanmaymac.modules.user.entity.UserEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "disputes")
public class DisputeEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false, unique = true)
    private ComplaintEntity complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id", nullable = false)
    private UserEntity workshop;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DisputeStatus status;

    @Column(name = "admin_request_info", columnDefinition = "TEXT")
    private String adminRequestInfo;

    @Column(name = "customer_supplement", columnDefinition = "TEXT")
    private String customerSupplement;

    @Column(name = "ruling", columnDefinition = "TEXT")
    private String ruling;

    @Column(name = "refund_processed")
    private Boolean refundProcessed;

    @Column(name = "escrow_released")
    private Boolean escrowReleased;

    @Column(name = "violation_recorded")
    private Boolean violationRecorded;

    @Column(name = "violation_note", columnDefinition = "TEXT")
    private String violationNote;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
