package com.omisys.user.domain.repository;

import com.omisys.user.domain.model.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointHistoryRepository {

    PointHistory save(PointHistory pointHistory);

    Page<PointHistory> findAllByUserId(Long userId, Pageable pageable);

    Optional<PointHistory> findById(Long pointHistoryId);

    void rollback(PointHistory pointHistory);

    Page<PointHistory> findAll(Pageable pageable);

    void delete(PointHistory pointHistory);

}
