package com.omisys.user.application.dto;

import com.omisys.user.domain.model.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AddressResponse {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Get {

        private Long id;
        private Long userId;
        private String alias;
        private String recipient;
        private String phoneNumber;
        private String zipcode;
        private String address;
        private String roadAddress;
        private String jibunAddress;
        private String detailAddress;
        private String sido;
        private String sigungu;
        private Boolean isDefault;

        public static AddressResponse.Get of(Address address) {

            return Get.builder()
                    .id(address.getId())
                    .userId(address.getUser().getId())
                    .alias(address.getAlias())
                    .recipient(address.getRecipient())
                    .phoneNumber(address.getPhoneNumber())
                    .zipcode(address.getZipcode())
                    .address(address.getAddress())
                    .roadAddress(address.getRoadAddress())
                    .jibunAddress(address.getJibunAddress())
                    .detailAddress(address.getDetailAddress())
                    .sido(address.getSido())
                    .sigungu(address.getSigungu())
                    .isDefault(address.getIsDefault())
                    .build();

        }
    }
}
