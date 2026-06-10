package com.omisys.notification.server.infrastructure.notification;

public class InvalidFcmTokenException extends RuntimeException {
    public InvalidFcmTokenException(String token) {
        super("Invalid FCM registration token");
    }
}
