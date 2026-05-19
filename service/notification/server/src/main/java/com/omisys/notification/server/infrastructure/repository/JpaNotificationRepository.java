package com.omisys.notification.server.infrastructure.repository;

import com.omisys.notification.server.domain.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNotificationRepository extends JpaRepository<Notification, Long> {
}
