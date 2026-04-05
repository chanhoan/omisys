package com.omisys.auth.server.domain;

public record RefreshToken(String tokenValue, Long userId, String username, String role, String familyId) {
}
