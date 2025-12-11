package com.omisys.user.application.service;

import com.omisys.user.application.dto.AddressResponse;
import com.omisys.user.domain.model.Address;
import com.omisys.user.domain.model.User;
import com.omisys.user.domain.repository.AddressRepository;
import com.omisys.user.domain.repository.UserRepository;
import com.omisys.user.exception.UserException;
import com.omisys.user.presentation.request.AddressRequest;
import com.omisys.user_dto.infrastructure.AddressDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.omisys.user.exception.UserErrorCode.ADDRESS_NOT_FOUND;
import static com.omisys.user.exception.UserErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public void createAddress(Long userId, AddressRequest.Create request) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        addressRepository.save(Address.create(user, request));

    }

    public List<AddressResponse.Get> getAddressByUserId(Long userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        return addressRepository
                .findAllByUserId(userId)
                .stream()
                .map(AddressResponse.Get::of)
                .collect(Collectors.toList());

    }

    public List<AddressResponse.Get> getAddressList() {

        return addressRepository
                .findAll()
                .stream()
                .map(AddressResponse.Get::of)
                .collect(Collectors.toList());

    }

    @Transactional
    public void updateAddress(Long userId, Long addressId, AddressRequest.Update request) {

        userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        Address address = addressRepository
                .findById(addressId)
                .orElseThrow(() -> new UserException(ADDRESS_NOT_FOUND));

        address.update(request);

    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {

        userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));

        Address address = addressRepository
                .findById(addressId)
                .orElseThrow(() -> new UserException(ADDRESS_NOT_FOUND));

        addressRepository.delete(address);

    }

}
