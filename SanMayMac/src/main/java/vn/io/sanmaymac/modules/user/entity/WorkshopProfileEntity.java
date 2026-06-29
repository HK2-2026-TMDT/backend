package vn.io.sanmaymac.modules.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.io.sanmaymac.common.entity.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "workshop_profiles")
public class WorkshopProfileEntity extends BaseEntity {
    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "shop_name")
    private String shopName;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "workshop_address")
    private String workshopAddress;

    @Column(name = "tax_code")
    private String taxCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "license_url")
    private String licenseUrl;

    @Column(name = "production_capacity")
    private Integer productionCapacity;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account_no")
    private String bankAccountNo;

    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Column(name = "is_verified")
    private Boolean isVerified;

    @Column(name = "rating_avg")
    private Double ratingAvg;
}
