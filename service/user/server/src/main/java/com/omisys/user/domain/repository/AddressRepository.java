package com.omisys.user.domain.repository;

import com.omisys.user.domain.model.Address;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository {

    Address save(Address address);

    Optional<Address> findById(Long addressId);

    List<Address> findAllByUserId(Long userId);

    List<Address> findAll();

    void delete(Address address);

}
