package com.omisys.user.domain.repository;

import com.omisys.user.domain.model.Tier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TierRepository {

    Tier save(Tier tier);

    Optional<Tier> findByName(String name);

    List<Tier> findAll();

    Optional<Tier> findById(Long tierId);

    void delete(Tier tier);

}
