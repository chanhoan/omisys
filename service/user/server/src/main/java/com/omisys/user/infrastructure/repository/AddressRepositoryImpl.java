package com.omisys.user.infrastructure.repository;

import com.omisys.user.domain.model.Address;
import com.omisys.user.domain.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AddressRepositoryImpl implements AddressRepository {

    private final JpaAddressRepository jpaAddressRepository;


    @Override
    public Address save(Address address) {
        return jpaAddressRepository.save(address);
    }

    @Override
    public Optional<Address> findById(Long addressId) {
        return jpaAddressRepository.findById(addressId);
    }

    @Override
    public List<Address> findAllByUserId(Long userId) {
        return jpaAddressRepository.findAllByUserId(userId);
    }

    @Override
    public List<Address> findAll() {
        return jpaAddressRepository.findAll();
    }

    @Override
    public void delete(Address address) {
        jpaAddressRepository.delete(address);
    }

}