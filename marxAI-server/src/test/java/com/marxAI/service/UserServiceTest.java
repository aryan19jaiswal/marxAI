package com.marxAI.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marxAI.exception.EmailAlreadyExistsException;
import com.marxAI.exception.UserNotFoundException;
import com.marxAI.model.dto.AuthResponse;
import com.marxAI.model.dto.LoginRequest;
import com.marxAI.model.dto.RegisterRequest;
import com.marxAI.model.dto.UserResponse;
import com.marxAI.model.entity.User;
import com.marxAI.repository.UserRepository;
import com.marxAI.security.JwtService;
import com.marxAI.security.UserPrincipal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests for {@link UserService}, with all collaborators mocked. */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void register_throwsEmailAlreadyExists_whenEmailTaken() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("Jane", "jane@example.com", "password123");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("jane@example.com");
    }

    @Test
    void register_hashesPasswordAndIssuesToken_whenEmailIsFree() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("signed-jwt");
        when(jwtService.getExpirationMs()).thenReturn(86_400_000L);

        RegisterRequest request = new RegisterRequest("Jane", "jane@example.com", "password123");
        AuthResponse response = userService.register(request);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getPasswordHash()).isEqualTo("hashed-password");
        assertThat(savedUser.getValue().getEmail()).isEqualTo("jane@example.com");

        assertThat(response.token()).isEqualTo("signed-jwt");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("jane@example.com");
    }

    @Test
    void login_returnsAuthResponse_onValidCredentials() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("jane@example.com")
                .name("Jane")
                .passwordHash("hashed-password")
                .build();
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(user));
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("signed-jwt");
        when(jwtService.getExpirationMs()).thenReturn(86_400_000L);

        AuthResponse response = userService.login(new LoginRequest("jane@example.com", "password123"));

        assertThat(response.token()).isEqualTo("signed-jwt");
        assertThat(response.user().id()).isEqualTo(user.getId());
    }

    @Test
    void login_propagatesBadCredentials_whenAuthenticationManagerRejects() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad creds"));

        assertThatThrownBy(() -> userService.login(new LoginRequest("jane@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void getProfile_returnsUserResponse_whenUserExists() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("jane@example.com")
                .name("Jane")
                .passwordHash("hashed-password")
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.getProfile(userId);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("jane@example.com");
    }

    @Test
    void getProfile_throwsUserNotFound_whenUserMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(userId)).isInstanceOf(UserNotFoundException.class);
    }
}
