package com.omisys.user.domain.repository;

import com.omisys.user.domain.model.User;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface UserRepository {

    User save(User user);

    Optional<User> findByUsername(String username);

    Optional<User> findById(Long userId);

    Collection<User> findAllByIsDeletedFalse();

}
