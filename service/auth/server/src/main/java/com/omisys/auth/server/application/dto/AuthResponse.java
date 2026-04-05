package com.omisys.auth.server.application.dto;

public class AuthResponse {

    public record TokenPair(String accessToken, String refreshToken) {}
}
