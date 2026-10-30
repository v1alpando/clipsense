package io.videointel.user.application;

import io.videointel.user.domain.EmailAlreadyRegisteredException;
import io.videointel.user.domain.User;
import io.videointel.user.domain.UserNotFoundException;
import io.videointel.user.domain.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepositoryPort userRepository;

    @Transactional
    public User register(String email, String passwordHash) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        User user = User.register(email, passwordHash);
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent requests can both pass the existsByEmail check above;
            // the DB unique constraint is the real guard here, this just turns the
            // resulting SQL exception into a clean domain error instead of a 500.
            throw new EmailAlreadyRegisteredException(email);
        }
    }

    @Transactional(readOnly = true)
    public User get(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }
}
