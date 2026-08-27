package com.omisys.user.application.service;

import com.omisys.user.application.dto.PointResponse;
import com.omisys.user.domain.model.PointHistory;
import com.omisys.user.domain.model.User;
import com.omisys.user.domain.model.vo.UserRole;
import com.omisys.user.domain.repository.PointHistoryRepository;
import com.omisys.user.domain.repository.UserRepository;
import com.omisys.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.omisys.user.exception.UserErrorCode.*;

@Service
@RequiredArgsConstructor
public class PointHistoryService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public Page<PointResponse.Get> getPointHistoryByUserId(Long userId, Pageable pageable) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        return pointHistoryRepository
                .findAllByUserId(user.getId(), pageable)
                .map(PointResponse.Get::of);

    }

    public Page<PointResponse.Get> getPointHistoryList(Pageable pageable) {
        return pointHistoryRepository
                .findAll(pageable)
                .map(PointResponse.Get::of);

    }

    @Transactional
    public void deletePointHistory(Long pointHistoryId, Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        PointHistory pointHistory = pointHistoryRepository
                .findById(pointHistoryId)
                .orElseThrow(() -> new UserException(POINT_HISTORY_NOT_FOUND));

        if (user.getRole() == UserRole.ROLE_USER && !Objects.equals(user.getId(), pointHistory.getUser().getId())) {
            throw new UserException((INVAILD_POINT_USER));
        }
        pointHistoryRepository.delete(pointHistory);

    }

}
