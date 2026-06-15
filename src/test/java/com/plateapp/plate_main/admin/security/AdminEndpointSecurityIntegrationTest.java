package com.plateapp.plate_main.admin.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.auth.security.JwtProvider;
import com.plateapp.plate_main.auth.security.PlateAuthorities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminEndpointSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.findById("viewer@example.com").ifPresent(userRepository::delete);
    }

    @Test
    void unauthenticatedAdminRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-15"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_401"));
    }

    @Test
    void viewerCannotApproveStoreApplication() throws Exception {
        User viewer = userRepository.save(User.builder()
                .username("viewer@example.com")
                .role(PlateAuthorities.ROLE_VIEWER)
                .tokenVersion(0)
                .build());
        String token = jwtProvider.createAccessToken(
                viewer,
                PlateAuthorities.defaultPermissionsFor(PlateAuthorities.ROLE_VIEWER)
        );

        mockMvc.perform(post("/api/admin/store-approvals/10/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 0,
                                  "comment": "reviewed"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_403"));
    }
}
