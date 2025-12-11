package com.omisys.user.application.service;

import com.omisys.user.domain.model.Address;
import com.omisys.user.domain.repository.AddressRepository;
import com.omisys.user.exception.UserException;
import com.omisys.user_dto.infrastructure.AddressDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.omisys.user.exception.UserErrorCode.ADDRESS_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AddressInternalService {

    private final AddressRepository addressRepository;

    public AddressDto getAddress(Long addressId) {
        Address address = addressRepository
                .findById(addressId)
                .orElseThrow(() -> new UserException(ADDRESS_NOT_FOUND));

        return new AddressDto(
                address.getId(),
                address.getUser().getId(),
                address.getAlias(),
                address.getRecipient(),
                address.getPhoneNumber(),
                address.getZipcode(),
                address.getAddress(),
                address.getIsDefault()
        );
    }

}
