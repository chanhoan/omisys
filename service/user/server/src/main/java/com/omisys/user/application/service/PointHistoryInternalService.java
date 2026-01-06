package com.omisys.user.application.service;

import com.omisys.user.domain.model.PointHistory;
import com.omisys.user.domain.model.User;
import com.omisys.user.domain.model.vo.PointHistoryType;
import com.omisys.user.domain.repository.PointHistoryRepository;
import com.omisys.user.domain.repository.UserRepository;
import com.omisys.user.exception.UserErrorCode;
import com.omisys.user.exception.UserException;
import com.omisys.user_dto.infrastructure.PointHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.omisys.user.exception.UserErrorCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PointHistoryInternalService {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createPointHistory(PointHistoryDto request) {
        User user = userRepository
                .findById(request.getUserId())
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        log.info("Point History Type: {}", request.getType());

        switch (PointHistoryType.from(request.getType())) {
            case EARN:
            case REFUND:
                handlePointAdd(user, request.getPoint());
                break;
            case USE:
                handlePointUse(user, request.getPoint());
                break;
            default:
                throw new UserException(UserErrorCode.INVALID_POINT_HISTORY_TYPE);
        }
        return pointHistoryRepository.save(PointHistory.create(user, request)).getId();
    }

    @Transactional
    public void rollbackPointHistory(Long pointHistoryId) {

        PointHistory pointHistory = pointHistoryRepository
                .findById(pointHistoryId)
                .orElseThrow(() -> new UserException(POINT_HISTORY_NOT_FOUND));

        handlePointAdd(pointHistory.getUser(), pointHistory.getPoint());
        pointHistoryRepository.rollback(pointHistory);

    }

    private void handlePointAdd(User user, BigDecimal point) {
        user.updatePoint(user.getPoint().add(point));
        userRepository.save(user);
    }

    private void handlePointUse(User user, BigDecimal point) {
        user.updatePoint(user.getPoint().subtract(point));
        userRepository.save(user);
    }
}
