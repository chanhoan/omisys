package com.omisys.user.presentation.controller;

import com.omisys.user.application.service.PointHistoryInternalService;
import com.omisys.user.application.service.UserInternalService;
import com.omisys.user_dto.infrastructure.PointHistoryDto;
import com.omisys.user_dto.infrastructure.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserInternalController {

    private final UserInternalService userInternalService;
    private final PointHistoryInternalService pointHistoryInternalService;

    @GetMapping
    public UserDto getUserByUsername(@RequestParam String username) {
        return userInternalService.getUserByUsername(username);
    }

    @GetMapping("/user-id")
    public UserDto getUserById(@RequestParam Long userId) {
        return userInternalService.getUserByUserId(userId);
    }

    @PostMapping("/point")
    public Long createPointHistory(@RequestBody PointHistoryDto request) {
        return pointHistoryInternalService.createPointHistory(request);
    }

    @DeleteMapping("/point/{pointHistoryId}")
    public void rollbackPointHistory(@PathVariable Long pointHistoryId) {
        pointHistoryInternalService.rollbackPointHistory(pointHistoryId);
    }

}
