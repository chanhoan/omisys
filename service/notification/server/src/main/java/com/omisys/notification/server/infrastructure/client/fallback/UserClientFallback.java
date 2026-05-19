package com.omisys.notification.server.infrastructure.client.fallback;

import com.omisys.notification.server.infrastructure.client.UserClient;
import com.omisys.notification.server.infrastructure.client.dto.UserNotificationInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserClientFallback implements UserClient {

    @Override
    public UserNotificationInfo getNotificationInfo(Long userId) {
        log.warn("UserClient fallback for userId={}", userId);
        return null;
    }
}
