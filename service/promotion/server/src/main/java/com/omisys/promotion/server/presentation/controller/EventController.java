package com.omisys.promotion.server.presentation.controller;

import com.omisys.auth.server.auth_dto.jwt.JwtClaim;
import com.omisys.common.domain.response.ApiResponse;
import com.omisys.promotion.server.application.service.EventService;
import com.omisys.promotion.server.presentation.request.EventRequest;
import com.omisys.promotion.server.presentation.response.EventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("")
    public ApiResponse<Long> createEvent(@RequestBody EventRequest.Create request,
                                      @AuthenticationPrincipal
                                      JwtClaim jwtClaim) {
        return ApiResponse.created(eventService.createEvent(request, jwtClaim.getUserId()));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PatchMapping("/{eventId}")
    public ApiResponse<Long> updateEvent(
            @RequestBody EventRequest.Update request,
            @PathVariable Long eventId,
            @AuthenticationPrincipal JwtClaim jwtClaim) {
        return ApiResponse.ok(eventService.updateEvent(eventId, request, jwtClaim.getUserId()));
    }

    @GetMapping("")
    public ApiResponse<Page<EventResponse.Get>> getEvents(Pageable pageable) {
        return ApiResponse.ok(eventService.getEvents(pageable));
    }

    @GetMapping("/{eventId}")
    public ApiResponse<EventResponse.Get> getEvent(@PathVariable Long eventId) {
        return ApiResponse.ok(eventService.getEvent(eventId));
    }

    @DeleteMapping("/{eventId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<Void> deleteEvent(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId);
        return ApiResponse.ok();
    }

}
