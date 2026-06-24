package com.marxAI.service;

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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Account registration, login, and profile lookup. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .name(request.name())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        User saved;
        try {
            // Belt-and-suspenders: the unique constraint on users.email is the real guard against
            // a race between the existsByEmail check above and this insert.
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException(request.email());
        }

        return issueAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return issueAuthResponse(principal.getUser());
    }

    public UserResponse getProfile(UUID userId) {
        return userRepository
                .findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private AuthResponse issueAuthResponse(User user) {
        String token = jwtService.generateToken(new UserPrincipal(user));
        return AuthResponse.of(token, jwtService.getExpirationMs(), UserResponse.from(user));
    }
}
