package com.omisys.order.server.infrastructure.client;

import com.omisys.user_dto.infrastructure.AddressDto;
import com.omisys.user_dto.infrastructure.PointHistoryDto;
import com.omisys.user_dto.infrastructure.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user")
public interface UserClient {

    @GetMapping("/internal/users/user-id")
    UserDto getUser(@RequestParam(value = "userId") Long userId);

    @PostMapping("/internal/users/point")
    Long createPointHistory(@RequestBody PointHistoryDto request);

    @DeleteMapping("/internal/users/point/{pointHistoryId}")
    void rollbackPoint(@PathVariable Long pointHistoryId);

    @GetMapping("/internal/address/{addressId}")
    AddressDto getAddress(@PathVariable(name = "addressId") Long addressId);

}