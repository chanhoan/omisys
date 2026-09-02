package com.omisys.gateway.server.application.dto;

public record QueueStatusResponse(
        QueueState state,
        Long rank,
        Long retryAfterSeconds) {

    public static QueueStatusResponse waiting(long rank, long retryAfterSeconds) {
        return new QueueStatusResponse(QueueState.WAITING, rank, retryAfterSeconds);
    }

    public static QueueStatusResponse ready() {
        return new QueueStatusResponse(QueueState.READY, null, null);
    }

    public static QueueStatusResponse expired() {
        return new QueueStatusResponse(QueueState.EXPIRED, null, null);
    }
}
