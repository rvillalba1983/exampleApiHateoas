package com.rvillalba.hateoas.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param issuer          valor del claim {@code iss} de los tokens
 * @param secret          clave HMAC en Base64 (mínimo 256 bits). En producción, por variable de entorno.
 * @param accessTokenTtl  vida del token de acceso
 * @param refreshTokenTtl vida del token de refresco
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("60m") Duration refreshTokenTtl) {
}
