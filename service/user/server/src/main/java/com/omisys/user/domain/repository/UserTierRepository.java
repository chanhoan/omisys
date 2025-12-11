package com.omisys.user.domain.repository;

import com.omisys.user.domain.model.UserTier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTierRepository {

    UserTier save(UserTier userTier);

    Optional<UserTier> findByUserId(Long userId);

    Page<UserTier> findAll(Pageable pageable);

}
