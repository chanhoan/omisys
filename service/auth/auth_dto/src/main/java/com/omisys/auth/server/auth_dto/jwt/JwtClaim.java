package com.omisys.auth.server.auth_dto.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class JwtClaim {

    private Long userId;
    private String username;
    private String role;

    public static JwtClaim create(Long userId, String username, String role) {
        return new JwtClaim(userId, username, role);
    }

}
