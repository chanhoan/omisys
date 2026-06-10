package com.omisys.user.presentation.response;

import java.util.List;

public record UserNotificationInfoResponse(String email, List<UserDeviceInfoResponse> devices) {
}
