package com.omisys.user.infrastructure.repository;

import com.omisys.user.domain.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaAddressRepository extends JpaRepository<Address, Long> {

    List<Address> findAllByUserId(Long userId);

}
