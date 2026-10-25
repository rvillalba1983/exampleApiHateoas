package com.rvillalba.hateoas.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rvillalba.hateoas.security.TokenService.TokenResponse;
import com.rvillalba.hateoas.security.TokenService.UserInfo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
                          TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
    }

    /** Usuario y contraseña → token de acceso + token de refresco. */
    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        return tokenService.issue((UserDetails) authentication.getPrincipal());
    }

    /** Refresh token → nuevo par de tokens. Se vuelve a cargar el usuario por si ha cambiado o ya no existe. */
    @PostMapping("/auth/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        var username = tokenService.validateRefreshToken(request.refreshToken());
        return tokenService.issue(userDetailsService.loadUserByUsername(username));
    }

    /** Datos del usuario del token. */
    @GetMapping("/me")
    public UserInfo me(JwtAuthenticationToken authentication) {
        return new UserInfo(authentication.getName(),
                authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
    }

    @ExceptionHandler({AuthenticationException.class, JwtException.class, UsernameNotFoundException.class})
    ProblemDetail unauthorized(Exception e) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciales o token no válidos");
        problem.setTitle("Unauthorized");
        return problem;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }
}
