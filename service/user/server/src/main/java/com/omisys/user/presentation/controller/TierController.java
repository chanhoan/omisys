package com.omisys.user.presentation.controller;

import com.omisys.common.domain.response.ApiResponse;
import com.omisys.user.application.dto.TierResponse;
import com.omisys.user.application.service.TierService;
import com.omisys.user.presentation.request.TierRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tier")
@RequiredArgsConstructor
public class TierController {

    private final TierService tierService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping
    public ApiResponse<Void> createTier(@RequestBody TierRequest.Create request) {
        tierService.createTier(request);
        return ApiResponse.created(null);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping
    public ApiResponse<List<TierResponse.Get>> getTierList() {
        return ApiResponse.ok(tierService.getTierList());
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PatchMapping("/{tierId}")
    public ApiResponse<Void> updateTier(
            @PathVariable Long tierId,
            @RequestBody TierRequest.Update request
    ) {
        tierService.updateTier(tierId, request);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{tierId}")
    public ApiResponse<Void>  deleteTier(@PathVariable Long tierId) {
        tierService.deleteTier(tierId);
        return ApiResponse.ok();
    }

}
