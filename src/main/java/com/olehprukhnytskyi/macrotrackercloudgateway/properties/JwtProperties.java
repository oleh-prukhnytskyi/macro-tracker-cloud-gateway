package com.olehprukhnytskyi.macrotrackercloudgateway.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "spring.security.oauth2.resourceserver.jwt")
public class JwtProperties {
    @NotBlank
    private String jwkSetUri;
}
