package com.example.service;

import com.example.document.exception.ResourceAlreadyExistsException;
import com.example.document.exception.ResourceNotFoundException;
import com.example.document.exception.UnauthorizedException;
import com.example.document.user.LoginDTO;
import com.example.document.user.Profile;
import com.example.document.user.RegisterDTO;
import com.example.document.user.User;
import com.example.repository.UserRepository;
import com.example.utils.UserIdPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService service;

    private final String dummySigningKey = "c29tZS1zZWNyZXQta2V5LXRoYXQtaXMtYXQtbGVhc3QtMjU2LWJpdHMtbG9uZw==";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "jwtSigningKey", dummySigningKey);
    }

    @Test
    void shouldRegisterNewUser() throws IOException {
        RegisterDTO register = new RegisterDTO("user1", "mail1@test.com", "pw");
        when(userRepository.findByUsernameOrEmail("user1", "mail1@test.com")).thenReturn(new ArrayList<>());

        User result = service.newUser(register);

        assertThat(result.username()).isEqualTo(register.username());
        assertThat(result.email()).isEqualTo(register.email());
        assertThat(result.token()).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenRegisteringWithDuplicateUsername() throws IOException {
        RegisterDTO register = new RegisterDTO("user_dup", "another@test.com", "pw");
        User existingUser = new User("user_dup", "mail@test.com", "hashed", "token", "bio", "img", new byte[0], new ArrayList<>());
        when(userRepository.findByUsernameOrEmail("user_dup", "another@test.com")).thenReturn(List.of(existingUser));

        assertThrows(ResourceAlreadyExistsException.class, () -> service.newUser(register));
    }

    @Test
    void shouldThrowExceptionWhenAuthenticatingWithInvalidCredentials() throws IOException {
        // Email not found
        LoginDTO wrongEmail = new LoginDTO("notfound@test.com", "correct_pw");
        when(userRepository.findUserByEmail("notfound@test.com")).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> service.authenticateUser(wrongEmail));

        // Wrong password handled in unit test requires recreating the exact hash flow which is internal to authenticateUser.
        // But we can test that an exception is thrown for invalid password.
        User existingUser = new User("auth_user", "auth@test.com", "incorrect_hash", "token", "bio", "img", new byte[16], new ArrayList<>());
        when(userRepository.findUserByEmail("auth@test.com")).thenReturn(new UserIdPair(existingUser, "id"));
        LoginDTO wrongPw = new LoginDTO("auth@test.com", "wrong_pw");
        assertThrows(UnauthorizedException.class, () -> service.authenticateUser(wrongPw));
    }

    @Test
    void shouldThrowExceptionOnInvalidToken() {
        assertThrows(UnauthorizedException.class, () -> service.findUserByToken("Bearer invalid_token_format"));
    }

    @Test
    void shouldFollowUser() throws IOException {
        // We bypass findUserByToken by mocking it indirectly or just testing the flow.
        // The service decodes the JWT inside findUserByToken, so we can't easily mock it without a valid JWT.
        // Instead of writing a complex valid JWT, we can test that it handles user lookup.

        // Due to JWT generation inside `findUserByToken`, unit testing `followUser` requires a valid JWT 
        // generated with `dummySigningKey` or mocking `findUserByToken`. Since it's private/internal state, 
        // a pure unit test without a real token might just check the repo interactions if we can bypass it.
        // For simplicity in this rewrite, we will just ensure the repository methods are called if we create a valid token.

        RegisterDTO register = new RegisterDTO("follower", "follower@test.com", "pw");
        when(userRepository.findByUsernameOrEmail("follower", "follower@test.com")).thenReturn(new ArrayList<>());
        User createdA = service.newUser(register);
        String tokenA = "Bearer " + createdA.token();

        User followedUser = new User("followed", "followed@test.com", "hashed", "token2", "bio", "img", new byte[0], new ArrayList<>());
        when(userRepository.findUserByUsername("followed")).thenReturn(new UserIdPair(followedUser, "id2"));
        when(userRepository.findUserByToken(createdA.token())).thenReturn(new UserIdPair(createdA, "id1"));

        Profile profile = service.followUser("followed", tokenA);

        assertThat(profile.username()).isEqualTo("followed");
        assertThat(profile.following()).isTrue();
        verify(userRepository).updateUser(eq("id1"), any(User.class));
    }
}
