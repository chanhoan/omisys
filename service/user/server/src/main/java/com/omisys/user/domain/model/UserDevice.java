package com.omisys.user.domain.model;

import com.omisys.common.domain.entity.BaseEntity;
import com.omisys.user.domain.model.vo.DevicePlatform;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_devices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_devices_device_id", columnNames = "device_id"),
        @UniqueConstraint(name = "uk_user_devices_push_token", columnNames = "push_token")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDevice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(name = "push_token", nullable = false, length = 512)
    private String pushToken;

    @Column(name = "app_version", nullable = false, length = 32)
    private String appVersion;

    public static UserDevice create(User user, String deviceId, DevicePlatform platform,
                                    String pushToken, String appVersion) {
        UserDevice device = new UserDevice();
        device.deviceId = deviceId;
        device.update(user, platform, pushToken, appVersion);
        return device;
    }

    public void update(User user, DevicePlatform platform, String pushToken, String appVersion) {
        this.user = user;
        this.platform = platform;
        this.pushToken = pushToken;
        this.appVersion = appVersion;
    }
}
