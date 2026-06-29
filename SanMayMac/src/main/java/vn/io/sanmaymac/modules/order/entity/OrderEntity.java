package vn.io.sanmaymac.modules.order.entity;

import java.math.BigDecimal;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.io.sanmaymac.common.entity.BaseEntity;
import vn.io.sanmaymac.common.enums.OrderStatus;
import vn.io.sanmaymac.common.enums.OrderType;
import vn.io.sanmaymac.common.enums.PaymentStatus;
import vn.io.sanmaymac.modules.bidding.entity.QuoteEntity;
import vn.io.sanmaymac.modules.user.entity.UserAddressEntity;
import vn.io.sanmaymac.modules.user.entity.UserEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class OrderEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private UserEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id")
    private UserEntity workshop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private QuoteEntity quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private UserAddressEntity address;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "checkout_batch_id")
    private String checkoutBatchId;

    @Column(name = "shipping_fee")
    private BigDecimal shippingFee;

    @Column(name = "customer_note")
    private String customerNote;

    @Column(name = "ghn_order_code")
    private String ghnOrderCode;

    @Column(name = "tracking_code")
    private String trackingCode;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "front_design_url")
    private String frontDesignUrl;

    @Column(name = "back_design_url")
    private String backDesignUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type")
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;
}
