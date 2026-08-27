package com.omisys.user.infrastructure.repository;

import com.omisys.user.domain.model.UserTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaUserTierRepository extends JpaRepository<UserTier, Long> {

    Optional<UserTier> findByUserId(Long userId);

}
