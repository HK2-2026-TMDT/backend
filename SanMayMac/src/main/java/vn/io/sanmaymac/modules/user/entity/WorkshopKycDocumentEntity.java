package vn.io.sanmaymac.modules.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import vn.io.sanmaymac.common.enums.KycStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "workshop_kyc_documents")
public class WorkshopKycDocumentEntity extends BaseEntity {
    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "tax_code")
    private String taxCode;

    @Column(name = "license_url")
    private String licenseUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private KycStatus status;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;
}
