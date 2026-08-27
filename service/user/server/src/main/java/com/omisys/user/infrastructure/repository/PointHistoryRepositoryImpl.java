package com.omisys.user.infrastructure.repository;

import com.omisys.user.domain.model.PointHistory;
import com.omisys.user.domain.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

    private final JpaPointHistoryRepository jpaPointHistoryRepository;

    @Override
    public PointHistory save(PointHistory pointHistory) {
        return jpaPointHistoryRepository.save(pointHistory);
    }

    @Override
    public Page<PointHistory> findAllByUserId(Long userId, Pageable pageable) {
        return jpaPointHistoryRepository.findAllByUserId(userId, pageable);
    }

    @Override
    public Optional<PointHistory> findById(Long pointHistoryId) {
        return jpaPointHistoryRepository.findById(pointHistoryId);
    }

    @Override
    public void rollback(PointHistory pointHistory) {
        jpaPointHistoryRepository.delete(pointHistory);
    }

    @Override
    public Page<PointHistory> findAll(Pageable pageable) {
        return jpaPointHistoryRepository.findAll(pageable);
    }

    @Override
    public void delete(PointHistory pointHistory) {
        jpaPointHistoryRepository.delete(pointHistory);
    }

}
