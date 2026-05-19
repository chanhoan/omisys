package com.omisys.notification.server.domain.repository;

import com.omisys.notification.server.domain.model.Notification;

public interface NotificationRepository {
    Notification save(Notification notification);
}
