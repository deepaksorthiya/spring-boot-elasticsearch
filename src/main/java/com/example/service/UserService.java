package com.example.service;

import com.example.document.exception.ResourceAlreadyExistsException;
import com.example.document.exception.ResourceNotFoundException;
import com.example.document.exception.UnauthorizedException;
import com.example.document.user.*;
import com.example.repository.UserRepository;
import com.example.utils.UserIdPair;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.time.Instant;
import java.util.*;

import static com.example.utils.Utility.isNullOrBlank;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Value("${jwt.signing.key}")
    private String jwtSigningKey;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User newUser(RegisterDTO user) throws IOException {

        List<User> existingUsers = userRepository.findByUsernameOrEmail(user.username(), user.email());

        existingUsers.stream()
                .filter(x -> x.username().equals(user.username()))
                .findFirst()
                .ifPresent(x -> {
                    throw new ResourceAlreadyExistsException("Username already exists");
                });

        existingUsers.stream()
                .filter(x -> x.email().equals(user.email()))
                .findFirst()
                .ifPresent(x -> {
                    throw new ResourceAlreadyExistsException("Email already used");
                });

        byte[] keyBytes = Decoders.BASE64.decode(jwtSigningKey);
        SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);
        String jws = Jwts.builder()
                .issuer("rw-backend")
                .subject(user.email())
                .claim("name", user.username())
                .claim("scope", "user")
                .issuedAt(Date.from(Instant.now()))
                .expiration(new Date((new Date()).getTime() + 1000 * 60 * 60))
                .signWith(secretKey)
                .compact();

        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        String hashedPw = hashUserPw(user.password(), salt);

        User newUser = new User(user.username(), user.email(),
                hashedPw, jws, "", "", salt, new ArrayList<>());

        userRepository.save(newUser);

        return newUser;
    }

    public User authenticateUser(LoginDTO login) throws IOException {

        UserIdPair getUser = userRepository.findUserByEmail(login.email());

        if (getUser == null) {
            throw new ResourceNotFoundException("Email not found");
        }

        User user = getUser.user();
        String hashedPw = hashUserPw(login.password(), user.salt());

        if (!hashedPw.equals(user.password())) {
            throw new UnauthorizedException("Wrong password");
        }
        return user;
    }

    public UserIdPair findUserByToken(String auth) throws IOException {
        String token;
        try {
            token = auth.split(" ")[1];
            byte[] keyBytes = Decoders.BASE64.decode(jwtSigningKey);
            SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parse(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Token missing or not recognised");
        }

        UserIdPair getUser = userRepository.findUserByToken(token);

        if (getUser == null) {
            throw new ResourceNotFoundException("Token not assigned to any user");
        }
        return getUser;
    }

    public User updateUser(UserDTO userDTO, String auth) throws IOException {

        UserIdPair userPair = findUserByToken(auth);
        User user = userPair.user();

        if (!isNullOrBlank(userDTO.username()) && !userDTO.username().equals(user.username())) {
            UserIdPair newUsernameSearch = userRepository.findUserByUsername(userDTO.username());
            if (Objects.nonNull(newUsernameSearch)) {
                throw new ResourceAlreadyExistsException("Username already exists");
            }
        }

        if (!isNullOrBlank(userDTO.email()) && !userDTO.email().equals(user.email())) {
            UserIdPair newEmailSearch = userRepository.findUserByEmail(userDTO.email());
            if (Objects.nonNull(newEmailSearch)) {
                throw new ResourceAlreadyExistsException("Email already in use");
            }
        }

        User updatedUser = new User(isNullOrBlank(userDTO.username()) ? user.username() :
                userDTO.username(),
                isNullOrBlank(userDTO.email()) ? user.email() : userDTO.email(),
                user.password(), user.token(),
                isNullOrBlank(userDTO.bio()) ? user.bio() : userDTO.bio(),
                isNullOrBlank(userDTO.image()) ? user.image() : userDTO.image(),
                user.salt(), user.following());

        userRepository.updateUser(userPair.id(), updatedUser);
        return updatedUser;
    }

    public Profile findUserProfile(String username, String auth) throws IOException {

        UserIdPair targetUserPair = Optional.ofNullable(userRepository.findUserByUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User targetUser = targetUserPair.user();

        UserIdPair askingUserPair = findUserByToken(auth);
        boolean following = askingUserPair.user().following().contains(targetUser.username());

        return new Profile(targetUser, following);
    }

    public Profile followUser(String username, String auth) throws IOException {

        UserIdPair targetUserPair = Optional.ofNullable(userRepository.findUserByUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User targetUser = targetUserPair.user();

        UserIdPair askingUserPair = findUserByToken(auth);
        User askingUser = askingUserPair.user();

        if (askingUser.username().equals(targetUser.username())) {
            throw new RuntimeException("Cannot follow yourself!");
        }

        if (!askingUser.following().contains(targetUser.username())) {
            askingUser.following().add(targetUser.username());

            userRepository.updateUser(askingUserPair.id(), askingUser);
        }

        return new Profile(targetUser, true);
    }

    public Profile unfollowUser(String username, String auth) throws IOException {
        UserIdPair targetUserPair = Optional.ofNullable(userRepository.findUserByUsername(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User targetUser = targetUserPair.user();

        UserIdPair askingUserPair = findUserByToken(auth);
        User askingUser = askingUserPair.user();

        if (askingUser.following().contains(targetUser.username())) {
            askingUser.following().remove(targetUser.username());

            userRepository.updateUser(askingUserPair.id(), askingUser);
        }

        return new Profile(targetUser, false);
    }

    public UserIdPair findUserByUsername(String username) throws IOException {
        return userRepository.findUserByUsername(username);
    }

    private String hashUserPw(String password, byte[] salt) {

        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        String hashedPw = null;
        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = secretKeyFactory.generateSecret(keySpec).getEncoded();
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            hashedPw = encoder.encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        return hashedPw;
    }
}
