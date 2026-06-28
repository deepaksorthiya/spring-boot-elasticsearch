package com.example.repository;

import com.example.config.TestConfiguration;
import com.example.document.user.User;
import com.example.utils.UserIdPair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ImportTestcontainers(TestConfiguration.class)
@Testcontainers
class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUser() throws IOException {
        User user = new User("testuser1", "test1@test.com", "hashedPw", "token123", "bio", "image", new byte[0], new ArrayList<>());
        userRepository.save(user);

        UserIdPair foundByUsername = userRepository.findUserByUsername("testuser1");
        assertThat(foundByUsername).isNotNull();
        assertThat(foundByUsername.user().email()).isEqualTo("test1@test.com");

        UserIdPair foundByEmail = userRepository.findUserByEmail("test1@test.com");
        assertThat(foundByEmail).isNotNull();
        assertThat(foundByEmail.user().username()).isEqualTo("testuser1");

        UserIdPair foundByToken = userRepository.findUserByToken("token123");
        assertThat(foundByToken).isNotNull();
        assertThat(foundByToken.user().username()).isEqualTo("testuser1");
    }

    @Test
    void shouldFindUserByUsernameOrEmail() throws IOException {
        User user2 = new User("testuser2", "test2@test.com", "hashedPw", "token456", "bio", "image", new byte[0], new ArrayList<>());
        userRepository.save(user2);

        List<User> found = userRepository.findByUsernameOrEmail("testuser2", "test2@test.com");
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).username()).isEqualTo("testuser2");
    }

    @Test
    void shouldUpdateUser() throws IOException {
        User user = new User("updateuser", "update@test.com", "hashedPw", "token_update", "bio", "image", new byte[0], new ArrayList<>());
        userRepository.save(user);

        UserIdPair savedPair = userRepository.findUserByUsername("updateuser");

        User updatedUser = new User("updateuser", "newemail@test.com", "hashedPw", "token_update", "new bio", "image", new byte[0], new ArrayList<>());
        userRepository.updateUser(savedPair.id(), updatedUser);

        UserIdPair newlyFound = userRepository.findUserByUsername("updateuser");
        assertThat(newlyFound.user().email()).isEqualTo("newemail@test.com");
        assertThat(newlyFound.user().bio()).isEqualTo("new bio");
    }
}
