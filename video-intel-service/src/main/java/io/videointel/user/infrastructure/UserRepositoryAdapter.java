package io.videointel.user.infrastructure;

import io.videointel.user.domain.User;
import io.videointel.user.domain.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserRepository jpaRepository;

    @Override
    public User save(User user) {
        UserEntity entity = new UserEntity(user.id(), user.email(), user.passwordHash(), user.createdAt());
        jpaRepository.save(entity);
        return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private User toDomain(UserEntity entity) {
        return User.reconstruct(entity.getId(), entity.getEmail(), entity.getPasswordHash(), entity.getCreatedAt());
    }
}
