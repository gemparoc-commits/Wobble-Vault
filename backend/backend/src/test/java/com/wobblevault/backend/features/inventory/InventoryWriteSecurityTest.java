package com.wobblevault.backend.features.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wobblevault.backend.entity.Permission;
import com.wobblevault.backend.entity.User;
import com.wobblevault.backend.features.users.PermissionRepository;
import com.wobblevault.backend.features.users.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class InventoryWriteSecurityTest {

    private static final String EMAIL = "csrf-test@wobblevault.local";
    private static final String PASSWORD = "CsrfTest123!";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        seedAdmin();
    }

    void seedAdmin() {
        User admin = new User();
        admin.setUsername("csrf-admin");
        admin.setEmail(EMAIL);
        admin.setPassword(passwordEncoder.encode(PASSWORD));
        admin.setRole(User.Role.ADMIN);
        admin = userRepository.save(admin);

        Permission permission = new Permission();
        permission.setUser(admin);
        permission.setPageName("INVENTORY");
        permissionRepository.save(permission);
    }

    @Test
    void csrfBodyTokenMatchesCookie() throws Exception {
        CsrfPair csrf = fetchCsrf(login());
        assertEquals(csrf.cookieValue(), csrf.bodyToken());
    }

    @Test
    void createInventoryWithCookiePlusBodyTokenReturns201() throws Exception {
        String token = login();
        CsrfPair csrf = fetchCsrf(token);

        mockMvc.perform(post("/api/inventory")
                        .header("Authorization", "Bearer " + token)
                        .cookie(new Cookie("XSRF-TOKEN", csrf.cookieValue()))
                        .header("X-XSRF-TOKEN", csrf.bodyToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writePayload()))
                .andExpect(status().isCreated());
    }

    @Test
    void createInventoryWithTamperedTokenIsForbidden() throws Exception {
        String token = login();
        CsrfPair csrf = fetchCsrf(token);

        mockMvc.perform(post("/api/inventory")
                        .header("Authorization", "Bearer " + token)
                        .cookie(new Cookie("XSRF-TOKEN", csrf.cookieValue()))
                        .header("X-XSRF-TOKEN", "tampered-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writePayload()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void createInventoryWithoutCsrfIsForbidden() throws Exception {
        String token = login();

        mockMvc.perform(post("/api/inventory")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writePayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createInventoryWithoutBearerIsUnauthorized() throws Exception {
        CsrfPair csrf = fetchCsrf(login());

        mockMvc.perform(post("/api/inventory")
                        .cookie(new Cookie("XSRF-TOKEN", csrf.cookieValue()))
                        .header("X-XSRF-TOKEN", csrf.bodyToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writePayload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void readInventoryWithoutCsrfIsOk() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/inventory")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.path("accessToken").asText();
        assertNotNull(token);
        return token;
    }

    private CsrfPair fetchCsrf(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CsrfPair(cookie.getValue(), body.path("token").asText());
    }

    private String writePayload() {
        return "{\"brand\":\"Nike\",\"name\":\"Air Zoom\",\"size\":\"9\",\"quantity\":2,\"price\":2500.50}";
    }

    private record CsrfPair(String cookieValue, String bodyToken) {}
}
