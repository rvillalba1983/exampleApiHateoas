package com.rvillalba.hateoas.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

/**
 * Emite y valida los tokens. Hay dos tipos, distinguidos por el claim {@code token_type}:
 * <ul>
 *   <li><b>access</b>: corta duración, se envía en cada petición ({@code Authorization: Bearer ...})</li>
 *   <li><b>refresh</b>: larga duración, solo sirve para pedir un nuevo par de tokens</li>
 * </ul>
 * Los decoders rechazan el tipo equivocado, así que un refresh token no abre la API ni al revés.
 */
@Service
public class TokenService {

    static final String TOKEN_TYPE = "token_type";
    static final String ACCESS = "access";
    static final String REFRESH = "refresh";
    static final String ROLES = "roles";

    private final JwtEncoder encoder;
    private final JwtDecoder refreshTokenDecoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder encoder, @Qualifier("refreshTokenDecoder") JwtDecoder refreshTokenDecoder,
                        JwtProperties properties) {
        this.encoder = encoder;
        this.refreshTokenDecoder = refreshTokenDecoder;
        this.properties = properties;
    }

    public TokenResponse issue(UserDetails user) {
        var now = Instant.now();
        var roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        var access = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .claim(TOKEN_TYPE, ACCESS)
                .claim(ROLES, roles)
                .build();

        var refresh = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.refreshTokenTtl()))
                .claim(TOKEN_TYPE, REFRESH)
                .build();

        return new TokenResponse(encode(access), encode(refresh), "Bearer", properties.accessTokenTtl().toSeconds());
    }

    /**
     * Valida un refresh token (firma, caducidad, emisor y tipo) y devuelve el usuario al que pertenece.
     *
     * @throws org.springframework.security.oauth2.jwt.JwtException si no es válido
     */
    public String validateRefreshToken(String refreshToken) {
        Jwt jwt = refreshTokenDecoder.decode(refreshToken);
        return jwt.getSubject();
    }

    private String encode(JwtClaimsSet claims) {
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    }

    public record UserInfo(String username, List<String> roles) {
    }
}
