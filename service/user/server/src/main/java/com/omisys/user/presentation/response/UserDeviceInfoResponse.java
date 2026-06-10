package com.omisys.user.presentation.response;

public record UserDeviceInfoResponse(String deviceId, String platform, String pushToken) {
}
