package com.rvillalba.hateoas;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.jayway.jsonpath.JsonPath;

/**
 * Test de integración de la API completa: login, CRUD HATEOAS con token, permisos y refresco.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExampleApiTest {

    @Autowired
    private MockMvcTester mvc;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void login() {
        userToken = loginAndGet("user", "password", "$.accessToken");
        adminToken = loginAndGet("admin", "admin", "$.accessToken");
    }

    @Test
    void loginReturnsAccessAndRefreshTokens() {
        var result = login("user", "password");

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
        assertThat(result).bodyJson().extractingPath("$.expiresIn").isEqualTo(900);
        assertThat(result).bodyJson().extractingPath("$.refreshToken").asString().isNotBlank();
    }

    @Test
    void loginWithWrongPasswordIs401() {
        assertThat(login("user", "wrong")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void apiWithoutTokenIs401() {
        assertThat(mvc.get().uri("/api/examples")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meReturnsUserAndRoles() {
        var result = mvc.get().uri("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(adminToken));

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.username").isEqualTo("admin");
        assertThat(result).bodyJson().extractingPath("$.roles").asArray().containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void fullCrudWithHateoasLinks() {
        // Crear
        var created = mvc.post().uri("/api/examples")
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"primer ejemplo\"}")
                .exchange();
        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat(created).bodyJson().extractingPath("$.name").isEqualTo("primer ejemplo");
        assertThat(created).bodyJson().extractingPath("$.id").isNotNull();
        assertThat(created).bodyJson().extractingPath("$._links.self.href").asString().contains("/api/examples/");
        var location = URI.create(created.getResponse().getHeader(HttpHeaders.LOCATION)).getPath();

        // Leer
        var read = mvc.get().uri(location).header(HttpHeaders.AUTHORIZATION, bearer(userToken));
        assertThat(read).hasStatus(HttpStatus.OK);
        assertThat(read).bodyJson().extractingPath("$.name").isEqualTo("primer ejemplo");

        // Listar (colección paginada con enlaces)
        var list = mvc.get().uri("/api/examples").header(HttpHeaders.AUTHORIZATION, bearer(userToken));
        assertThat(list).hasStatus(HttpStatus.OK);
        assertThat(list).bodyJson().extractingPath("$._embedded.examples").asArray().isNotEmpty();
        assertThat(list).bodyJson().extractingPath("$.page.size").isEqualTo(20);

        // Buscar
        var search = mvc.get().uri("/api/examples/search/by-name?name=PRIMER")
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken));
        assertThat(search).bodyJson().extractingPath("$._embedded.examples[0].name").isEqualTo("primer ejemplo");

        // Modificar
        var updated = mvc.put().uri(location)
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"modificado\"}");
        assertThat(updated).hasStatus(HttpStatus.OK);
        assertThat(updated).bodyJson().extractingPath("$.name").isEqualTo("modificado");

        // Borrar: USER no puede, ADMIN sí
        assertThat(mvc.delete().uri(location).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.delete().uri(location).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri(location).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidEntityIs400() {
        var result = mvc.post().uri("/api/examples")
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void refreshTokenGivesNewAccessToken() throws Exception {
        var refreshToken = loginAndGet("user", "password", "$.refreshToken");

        var result = mvc.post().uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}")
                .exchange();
        assertThat(result).hasStatus(HttpStatus.OK);

        String newAccessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        assertThat(mvc.get().uri("/api/examples").header(HttpHeaders.AUTHORIZATION, bearer(newAccessToken)))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void refreshTokenCannotBeUsedToCallTheApi() {
        var refreshToken = loginAndGet("user", "password", "$.refreshToken");

        assertThat(mvc.get().uri("/api/examples").header(HttpHeaders.AUTHORIZATION, bearer(refreshToken)))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accessTokenCannotBeUsedToRefresh() {
        var result = mvc.post().uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + userToken + "\"}");

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tamperedTokenIs401() {
        assertThat(mvc.get().uri("/api/examples").header(HttpHeaders.AUTHORIZATION, bearer(userToken + "x")))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MockMvcTester.MockMvcRequestBuilder login(String username, String password) {
        return mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password));
    }

    private String loginAndGet(String username, String password, String jsonPath) {
        try {
            var result = login(username, password).exchange();
            assertThat(result).hasStatus(HttpStatus.OK);
            return JsonPath.read(result.getResponse().getContentAsString(), jsonPath);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
