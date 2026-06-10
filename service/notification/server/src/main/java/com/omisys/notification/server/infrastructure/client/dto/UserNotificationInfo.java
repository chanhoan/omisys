package com.omisys.notification.server.infrastructure.client.dto;

import java.util.List;

public record UserNotificationInfo(String email, List<UserDeviceInfo> devices) {
}
