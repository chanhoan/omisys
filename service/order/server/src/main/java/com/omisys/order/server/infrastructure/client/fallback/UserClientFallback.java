package com.omisys.order.server.infrastructure.client.fallback;

import com.omisys.order.server.exception.OrderErrorCode;
import com.omisys.order.server.exception.OrderException;
import com.omisys.order.server.infrastructure.client.UserClient;
import com.omisys.user_dto.infrastructure.AddressDto;
import com.omisys.user_dto.infrastructure.PointHistoryDto;
import com.omisys.user_dto.infrastructure.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserClientFallback implements UserClient {

    @Override
    public UserDto getUser(Long userId) {
        log.error("[CB] UserClient.getUser 호출 실패 - userId={}", userId);
        throw new OrderException(OrderErrorCode.SERVICE_UNAVAILABLE);
    }

    @Override
    public Long createPointHistory(PointHistoryDto request) {
        log.error("[CB] UserClient.createPointHistory 호출 실패 - userId={}", request.getUserId());
        throw new OrderException(OrderErrorCode.SERVICE_UNAVAILABLE);
    }

    @Override
    public void rollbackPoint(Long pointHistoryId) {
        // 보상 경로: 예외를 던지면 OrderRollbackService의 후속 보상 단계가 중단됨
        log.error("[CB] UserClient.rollbackPoint 호출 실패 (CB open) — 수동 복구 필요. pointHistoryId={}", pointHistoryId);
    }

    @Override
    public AddressDto getAddress(Long addressId) {
        log.error("[CB] UserClient.getAddress 호출 실패 - addressId={}", addressId);
        throw new OrderException(OrderErrorCode.SERVICE_UNAVAILABLE);
    }
}
