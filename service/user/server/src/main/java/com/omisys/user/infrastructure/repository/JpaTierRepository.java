package com.omisys.user.infrastructure.repository;

import com.omisys.user.domain.model.Tier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaTierRepository extends JpaRepository<Tier, Long> {

    Optional<Tier> findByName(String name);

}
