package com.omisys.auth.server.infrastructure.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {

    @NotBlank
    private String secretKey;

    @Min(60000)
    private int accessTokenExpiresIn;

}
