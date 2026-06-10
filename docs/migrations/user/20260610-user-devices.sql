CREATE TABLE IF NOT EXISTS user_devices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    platform VARCHAR(10) NOT NULL,
    push_token VARCHAR(512) NOT NULL,
    app_version VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NULL,
    created_by VARCHAR(255) NULL,
    updated_at DATETIME(6) NULL,
    updated_by VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_devices_device_id UNIQUE (device_id),
    CONSTRAINT uk_user_devices_push_token UNIQUE (push_token),
    INDEX idx_user_devices_user_id (user_id),
    CONSTRAINT fk_user_devices_user FOREIGN KEY (user_id) REFERENCES p_user (user_id)
);

ALTER TABLE p_user DROP COLUMN IF EXISTS fcm_token;
