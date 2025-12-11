package com.omisys.user.infrastructure.repository;

import com.omisys.user.domain.model.User;
import com.omisys.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    @Override
    public User save(User user) {
        return jpaUserRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaUserRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return jpaUserRepository.findById(userId);
    }

    @Override
    public Collection<User> findAllByIsDeletedFalse() {
        return jpaUserRepository.findALlByIsDeletedFalse();
    }
}
