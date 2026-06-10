package com.omisys.user.presentation.request;

import com.omisys.user.domain.model.vo.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserDeviceRequest(
        @NotNull DevicePlatform platform,
        @NotBlank @Size(max = 512) String pushToken,
        @NotBlank @Size(max = 32) String appVersion) {
}
