package com.omisys.user.infrastructure.repository;

import com.omisys.user.domain.model.UserTier;
import com.omisys.user.domain.repository.UserTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserTierRepositoryImpl implements UserTierRepository {

    private final JpaUserTierRepository jpaUserTierRepository;


    @Override
    public UserTier save(UserTier userTier) {
        return jpaUserTierRepository.save(userTier);
    }

    @Override
    public Optional<UserTier> findByUserId(Long userId) {
        return jpaUserTierRepository.findByUserId(userId);
    }

    @Override
    public Page<UserTier> findAll(Pageable pageable) {
        return jpaUserTierRepository.findAll(pageable);
    }

}
