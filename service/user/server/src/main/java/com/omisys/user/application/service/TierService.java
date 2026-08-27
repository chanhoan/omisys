package com.omisys.user.application.service;

import com.omisys.user.application.dto.TierResponse;
import com.omisys.user.domain.model.Tier;
import com.omisys.user.domain.repository.TierRepository;
import com.omisys.user.exception.UserException;
import com.omisys.user.presentation.request.TierRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.omisys.user.exception.UserErrorCode.TIER_CONFLICT;
import static com.omisys.user.exception.UserErrorCode.TIER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class TierService {

    private final TierRepository tierRepository;

    @Transactional
    public void createTier(TierRequest.Create request) {
        tierRepository
                .findByName(request.getName())
                .ifPresent(
                        tier -> {
                            throw new UserException(TIER_CONFLICT);
                        });

        tierRepository.save(Tier.create(request));
    }

    public List<TierResponse.Get> getTierList() {

        return tierRepository
                .findAll()
                .stream()
                .map(TierResponse.Get::of)
                .collect(Collectors.toList());

    }

    @Transactional
    public void updateTier(Long tierId, TierRequest.Update request) {

        Tier tier = tierRepository
                .findById(tierId)
                .orElseThrow(() -> new UserException(TIER_NOT_FOUND));

        tier.update(request);

    }

    @Transactional
    public void deleteTier(Long tierId) {

        Tier tier = tierRepository
                .findById(tierId)
                .orElseThrow(() -> new UserException(TIER_NOT_FOUND));

        tierRepository.delete(tier);

    }
}
