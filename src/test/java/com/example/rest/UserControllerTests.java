package com.example.rest;

import com.example.document.user.LoginDTO;
import com.example.document.user.RegisterDTO;
import com.example.document.user.User;
import com.example.document.user.UserDTO;
import com.example.service.UserService;
import com.example.utils.UserIdPair;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private co.elastic.clients.elasticsearch.ElasticsearchClient elasticsearchClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterDTO req = new RegisterDTO("testuser", "test@test.com", "password");
        User user = new User("testuser", "test@test.com", "pw", "token", "bio", "image", new byte[0], java.util.List.of());
        when(userService.newUser(any(RegisterDTO.class))).thenReturn(user);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@test.com"));
    }

    @Test
    void shouldLoginUserSuccessfully() throws Exception {
        LoginDTO req = new LoginDTO("test@test.com", "password");
        User user = new User("testuser", "test@test.com", "pw", "token", "bio", "image", new byte[0], java.util.List.of());
        when(userService.authenticateUser(any(LoginDTO.class))).thenReturn(user);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@test.com"));
    }

    @Test
    void shouldFindCurrentUserByToken() throws Exception {
        User user = new User("testuser", "test@test.com", "pw", "token", "bio", "image", new byte[0], java.util.List.of());
        when(userService.findUserByToken("Bearer token")).thenReturn(new UserIdPair(user, "id"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@test.com"));
    }

    @Test
    void shouldUpdateUserSuccessfully() throws Exception {
        UserDTO req = new UserDTO("updateduser", "updated@test.com", "pw", "new bio", "new image");
        User user = new User("updateduser", "updated@test.com", "pw", "token", "new bio", "new image", new byte[0], java.util.List.of());
        when(userService.updateUser(any(UserDTO.class), eq("Bearer token"))).thenReturn(user);

        mockMvc.perform(put("/api/users")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("updateduser"))
                .andExpect(jsonPath("$.user.bio").value("new bio"));
    }
}
