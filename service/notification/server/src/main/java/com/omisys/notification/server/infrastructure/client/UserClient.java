package com.omisys.notification.server.infrastructure.client;

import com.omisys.notification.server.infrastructure.client.dto.UserNotificationInfo;
import com.omisys.notification.server.infrastructure.client.fallback.UserClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/internal/users/{userId}/notification-info")
    UserNotificationInfo getNotificationInfo(@PathVariable Long userId);
}
