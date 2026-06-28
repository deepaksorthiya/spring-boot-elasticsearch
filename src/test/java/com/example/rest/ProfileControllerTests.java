package com.example.rest;

import com.example.document.user.Profile;
import com.example.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
class ProfileControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private co.elastic.clients.elasticsearch.ElasticsearchClient elasticsearchClient;

    @Test
    void shouldReturnProfileSuccessfully() throws Exception {
        Profile profile = new Profile("testuser", "bio", "image", false);
        when(userService.findUserProfile(eq("testuser"), eq("Bearer token"))).thenReturn(profile);

        mockMvc.perform(get("/api/profiles/testuser")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("testuser"))
                .andExpect(jsonPath("$.profile.following").value(false));
    }

    @Test
    void shouldFollowUserAndReturnProfile() throws Exception {
        Profile profile = new Profile("testuser", "bio", "image", true);
        when(userService.followUser(eq("testuser"), eq("Bearer token"))).thenReturn(profile);

        mockMvc.perform(post("/api/profiles/testuser/follow")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("testuser"))
                .andExpect(jsonPath("$.profile.following").value(true));
    }

    @Test
    void shouldUnfollowUserAndReturnProfile() throws Exception {
        Profile profile = new Profile("testuser", "bio", "image", false);
        when(userService.unfollowUser(eq("testuser"), eq("Bearer token"))).thenReturn(profile);

        mockMvc.perform(delete("/api/profiles/testuser/follow")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.username").value("testuser"))
                .andExpect(jsonPath("$.profile.following").value(false));
    }
}
