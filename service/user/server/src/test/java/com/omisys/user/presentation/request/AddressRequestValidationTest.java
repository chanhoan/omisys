package com.omisys.user.presentation.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createRequiresAnExplicitOrRoadAddress() {
        assertThat(validator.validate(createRequest(null, null, null, "Seoul", "Gangnam")))
                .extracting(violation -> violation.getMessage())
                .contains("Either address or roadAddress must be provided.");
    }

    @Test
    void updateRequiresAnExplicitOrRoadAddress() {
        assertThat(validator.validate(updateRequest(null, null, null, "Seoul", "Gangnam")))
                .extracting(violation -> violation.getMessage())
                .contains("Either address or roadAddress must be provided.");
    }

    @Test
    void rejectsAResolvedLegacyAddressLongerThanItsColumn() {
        String roadAddress = "r".repeat(255);
        String detailAddress = "d".repeat(100);

        assertThat(validator.validate(createRequest(null, roadAddress, detailAddress, "Seoul", "Gangnam")))
                .extracting(violation -> violation.getMessage())
                .contains("Resolved address must not exceed 255 characters.");
    }

    @Test
    void rejectsSidoAndSigunguLongerThanTheirColumns() {
        assertThat(validator.validate(createRequest("legacy", null, null, "s".repeat(41), "g".repeat(41))))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("sido", "sigungu");
    }

    private AddressRequest.Create createRequest(
            String address, String roadAddress, String detailAddress, String sido, String sigungu) {
        return new AddressRequest.Create(
                "Home", "Recipient", "01012345678", "12345", address, true,
                roadAddress, null, detailAddress, sido, sigungu);
    }

    private AddressRequest.Update updateRequest(
            String address, String roadAddress, String detailAddress, String sido, String sigungu) {
        return new AddressRequest.Update(
                "Home", "Recipient", "01012345678", "12345", address, true,
                roadAddress, null, detailAddress, sido, sigungu);
    }
}
