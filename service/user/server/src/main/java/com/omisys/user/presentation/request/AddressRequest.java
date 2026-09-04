package com.omisys.user.presentation.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AddressRequest {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        @NotBlank
        private String alias;
        @NotBlank
        private String recipient;
        @NotBlank
        @Pattern(regexp = "^01[016-9]-?\\d{3,4}-?\\d{4}$", message = "연락처 형식이 올바르지 않습니다.")
        private String phoneNumber;
        @NotBlank
        @Pattern(regexp = "\\d{5}", message = "우편번호는 5자리 숫자입니다.")
        private String zipcode;
        // roadAddress is required when address is omitted.
        @Size(max = 255)
        private String address;
        @NotNull
        private Boolean isDefault;
        @Size(max = 255)
        private String roadAddress;
        @Size(max = 255)
        private String jibunAddress;
        @Size(max = 100)
        private String detailAddress;
        @Size(max = 40)
        private String sido;
        @Size(max = 40)
        private String sigungu;

        @AssertTrue(message = "Either address or roadAddress must be provided.")
        @JsonIgnore
        public boolean isAddressSourceProvided() {
            return isNotBlank(address) || isNotBlank(roadAddress);
        }

        @AssertTrue(message = "Resolved address must not exceed 255 characters.")
        @JsonIgnore
        public boolean isResolvedAddressWithinColumnLength() {
            return isNotBlank(address) || resolvedAddressLength() <= 255;
        }

        private int resolvedAddressLength() {
            return roadAddress == null ? 0 : roadAddress.length()
                    + (detailAddress == null || detailAddress.isBlank() ? 0 : 1 + detailAddress.length());
        }

    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {

        @NotBlank
        private String alias;
        @NotBlank
        private String recipient;
        @NotBlank
        @Pattern(regexp = "^01[016-9]-?\\d{3,4}-?\\d{4}$", message = "연락처 형식이 올바르지 않습니다.")
        private String phoneNumber;
        @NotBlank
        @Pattern(regexp = "\\d{5}", message = "우편번호는 5자리 숫자입니다.")
        private String zipcode;
        // roadAddress is required when address is omitted.
        @Size(max = 255)
        private String address;
        @NotNull
        private Boolean isDefault;
        @Size(max = 255)
        private String roadAddress;
        @Size(max = 255)
        private String jibunAddress;
        @Size(max = 100)
        private String detailAddress;
        @Size(max = 40)
        private String sido;
        @Size(max = 40)
        private String sigungu;

        @AssertTrue(message = "Either address or roadAddress must be provided.")
        @JsonIgnore
        public boolean isAddressSourceProvided() {
            return isNotBlank(address) || isNotBlank(roadAddress);
        }

        @AssertTrue(message = "Resolved address must not exceed 255 characters.")
        @JsonIgnore
        public boolean isResolvedAddressWithinColumnLength() {
            return isNotBlank(address) || resolvedAddressLength() <= 255;
        }

        private int resolvedAddressLength() {
            return roadAddress == null ? 0 : roadAddress.length()
                    + (detailAddress == null || detailAddress.isBlank() ? 0 : 1 + detailAddress.length());
        }

    }
    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
