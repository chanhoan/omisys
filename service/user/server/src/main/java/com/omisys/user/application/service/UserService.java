package com.omisys.user.application.service;

import com.omisys.user.application.dto.UserResponse;
import com.omisys.user.application.dto.UserTierResponse;
import com.omisys.user.domain.model.Tier;
import com.omisys.user.domain.model.User;
import com.omisys.user.domain.model.UserTier;
import com.omisys.user.domain.repository.TierRepository;
import com.omisys.user.domain.repository.UserRepository;
import com.omisys.user.domain.repository.UserTierRepository;
import com.omisys.user.exception.UserException;
import com.omisys.user.presentation.request.UserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.omisys.user.exception.UserErrorCode.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TierRepository tierRepository;
    private final UserTierRepository userTierRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createUser(UserRequest.Create request) {

        userRepository
                .findByUsername(request.getUsername())
                .ifPresent(user -> {
                    throw new UserException(USER_CONFLICT);
                });

        Tier defaultTier = tierRepository.findByName("아이언").orElseThrow(
                () -> new UserException(TIER_NOT_FOUND)
        );

        User user = userRepository.save(
                User.create(request, passwordEncoder.encode(request.getPassword()))
        );

        userTierRepository.save(UserTier.create(user, defaultTier));

    }

    public UserResponse.Info getUserById(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        return UserResponse.Info.of(user);

    }

    public List<UserResponse.Info> getUserList() {

        return userRepository
                .findAllByIsDeletedFalse()
                .stream()
                .map(UserResponse.Info::of)
                .collect(Collectors.toList());

    }

    @Transactional
    public void deleteUser(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserException(USER_NOT_FOUND));
        user.delete(true);
    }

    @Transactional
    public void updateUserPassword(Long userId, UserRequest.UpdatePassword request) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UserException(INVAILD_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.getUpdatePassword()));

    }

    @Transactional
    public UserTierResponse.Get getUserTierByUserId(Long userId) {

        UserTier userTier = userTierRepository
                .findByUserId(userId)
                .orElseThrow(() -> new UserException(USER_TIER_NOT_FOUND));

        User user = userRepository
                .findById(userTier.getId())
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        Tier tier = tierRepository
                .findById(userTier.getTier().getId())
                .orElseThrow(() -> new UserException(TIER_NOT_FOUND));

        return UserTierResponse.Get.of(user, tier);

    }

    @Transactional
    public Page<UserTierResponse.Get> getUserTierList(Pageable pageable) {

        Page<UserTier> userTiers = userTierRepository.findAll(pageable);

        return userTiers.map(userTier -> {
            User user = userRepository
                    .findById(userTier.getUser().getId())
                    .orElseThrow(() -> new UserException(USER_NOT_FOUND));
            Tier tier = tierRepository
                    .findById(userTier.getTier().getId())
                    .orElseThrow(() -> new UserException(TIER_NOT_FOUND));
            return UserTierResponse.Get.of(user, tier);
        });

    }

    @Transactional
    public void updateUserTier(Long userId, UserRequest.UpdateTier request) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        UserTier userTier = userTierRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new UserException(USER_TIER_NOT_FOUND));

        Tier tier = tierRepository
                .findByName(request.getTier())
                .orElseThrow(() -> new UserException(TIER_NOT_FOUND));

        userTier.update(tier);

    }

}
