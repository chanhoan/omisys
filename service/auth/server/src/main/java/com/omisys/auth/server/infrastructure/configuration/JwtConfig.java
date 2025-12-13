package com.omisys.auth.server.infrastructure.configuration;

import com.omisys.auth.server.infrastructure.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {}
