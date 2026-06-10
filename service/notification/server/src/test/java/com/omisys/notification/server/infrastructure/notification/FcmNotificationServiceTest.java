package com.omisys.notification.server.infrastructure.notification;

import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FcmNotificationServiceTest {

    @Test
    void onlyUnregisteredMeansTheDeviceTokenIsInvalid() {
        assertThat(FcmNotificationService.isInvalidToken(MessagingErrorCode.UNREGISTERED)).isTrue();
        assertThat(FcmNotificationService.isInvalidToken(MessagingErrorCode.INVALID_ARGUMENT)).isFalse();
    }
}
