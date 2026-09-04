package com.omisys.user.domain.model;

import com.omisys.common.domain.entity.BaseEntity;
import com.omisys.user.presentation.request.AddressRequest;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "p_address")
@Entity
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String alias;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String zipcode;

    @Column(nullable = false)
    private String address;

    private String roadAddress;

    private String jibunAddress;

    private String detailAddress;

    private String sido;

    private String sigungu;

    @Column(nullable = false)
    private Boolean isDefault;

    public static Address create(User user, AddressRequest.Create request) {
        return Address.builder()
                .user(user)
                .alias(request.getAlias())
                .recipient(request.getRecipient())
                .phoneNumber(request.getPhoneNumber())
                .zipcode(request.getZipcode())
                .address(resolveAddress(request.getAddress(), request.getRoadAddress(), request.getDetailAddress()))
                .roadAddress(request.getRoadAddress())
                .jibunAddress(request.getJibunAddress())
                .detailAddress(request.getDetailAddress())
                .sido(request.getSido())
                .sigungu(request.getSigungu())
                .isDefault(request.getIsDefault())
                .build();
    }

    public void update(AddressRequest.Update request) {
        this.alias = request.getAlias();
        this.recipient = request.getRecipient();
        this.phoneNumber = request.getPhoneNumber();
        this.zipcode = request.getZipcode();
        this.address = resolveAddress(request.getAddress(), request.getRoadAddress(), request.getDetailAddress());
        this.roadAddress = request.getRoadAddress();
        this.jibunAddress = request.getJibunAddress();
        this.detailAddress = request.getDetailAddress();
        this.sido = request.getSido();
        this.sigungu = request.getSigungu();
        this.isDefault = request.getIsDefault();
    }

    private static String resolveAddress(String explicitAddress, String roadAddress, String detailAddress) {
        if (explicitAddress != null && !explicitAddress.isBlank()) {
            return explicitAddress;
        }

        return (roadAddress == null ? "" : roadAddress)
                + (detailAddress == null || detailAddress.isBlank() ? "" : " " + detailAddress);
    }

}
