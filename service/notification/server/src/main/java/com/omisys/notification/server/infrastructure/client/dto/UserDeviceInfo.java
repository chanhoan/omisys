package com.omisys.notification.server.infrastructure.client.dto;

public record UserDeviceInfo(String deviceId, String platform, String pushToken) {
}
