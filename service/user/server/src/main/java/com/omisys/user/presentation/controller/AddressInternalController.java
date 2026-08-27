package com.omisys.user.presentation.controller;

import com.omisys.user.application.service.AddressInternalService;
import com.omisys.user_dto.infrastructure.AddressDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/address")
@RequiredArgsConstructor
public class AddressInternalController {

    private final AddressInternalService addressInternalService;

    @GetMapping("/{addressId}")
    public AddressDto getAddress(@PathVariable Long addressId) {
        return addressInternalService.getAddress(addressId);
    }

}
