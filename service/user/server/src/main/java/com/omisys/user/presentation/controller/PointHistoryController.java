package com.omisys.user.presentation.controller;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.common.domain.response.ApiResponse;
import com.omisys.user.application.dto.PointResponse;
import com.omisys.user.application.service.PointHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/point")
@RequiredArgsConstructor
public class PointHistoryController {

    private final PointHistoryService pointHistoryService;

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/me")
    public ApiResponse<Page<PointResponse.Get>> getPointHistoryByUser(
            @AuthenticationPrincipal JwtClaim claim,
            Pageable pageable
    ) {
        return ApiResponse.ok(pointHistoryService.getPointHistoryByUserId(claim.getUserId(), pageable));
    }

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @GetMapping("/{userId}")
    public ApiResponse<Page<PointResponse.Get>> getPointHistoryByUserId(
            @PathVariable Long userId,
            Pageable pageable
    ) {
        return ApiResponse.ok(pointHistoryService.getPointHistoryByUserId(userId, pageable));
    }

    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @GetMapping
    public ApiResponse<Page<PointResponse.Get>> getPointHistoryList(Pageable pageable) {
        return ApiResponse.ok(pointHistoryService.getPointHistoryList(pageable));
    }

    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    @DeleteMapping("/{pointHistoryId}")
    public ApiResponse<Void> deletePointHistory(
            @PathVariable Long pointHistoryId,
            @AuthenticationPrincipal JwtClaim claim
    ) {
        pointHistoryService.deletePointHistory(pointHistoryId, claim.getUserId());
        return ApiResponse.ok();
    }

}
