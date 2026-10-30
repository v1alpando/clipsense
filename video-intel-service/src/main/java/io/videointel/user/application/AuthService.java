package io.videointel.user.application;

import io.videointel.user.domain.InvalidCredentialsException;
import io.videointel.user.domain.TokenIssuerPort;
import io.videointel.user.domain.User;
import io.videointel.user.domain.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepositoryPort userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuerPort tokenIssuer;

    @Transactional
    public User register(String email, String rawPassword) {
        String hash = passwordEncoder.encode(rawPassword);
        return userService.register(email, hash);
    }

    @Transactional(readOnly = true)
    public String login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokenIssuer.issueToken(user.id(), user.email());
    }
}
