package com.omisys.notification.server.domain.model;

import com.omisys.common.domain.entity.BaseEntity;
import com.omisys.notification.server.domain.model.vo.NotificationChannel;
import com.omisys.notification.server.domain.model.vo.NotificationStatus;
import com.omisys.notification.server.domain.model.vo.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationStatus status;

    @Column(length = 500)
    private String errorMessage;

    @Column
    private LocalDateTime sentAt;

    public static Notification sent(Long userId, Long orderId,
                                    NotificationType type, NotificationChannel channel) {
        Notification n = new Notification();
        n.userId = userId;
        n.orderId = orderId;
        n.type = type;
        n.channel = channel;
        n.status = NotificationStatus.SENT;
        n.sentAt = LocalDateTime.now();
        return n;
    }

    public static Notification failed(Long userId, Long orderId,
                                      NotificationType type, NotificationChannel channel,
                                      String errorMessage) {
        Notification n = new Notification();
        n.userId = userId;
        n.orderId = orderId;
        n.type = type;
        n.channel = channel;
        n.status = NotificationStatus.FAILED;
        n.errorMessage = errorMessage;
        return n;
    }
}
