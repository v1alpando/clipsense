package io.videointel.user.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void registerCreatesUserWithGivenEmail() {
        User user = User.register("arman@example.com", "hashed-password");

        assertThat(user.id()).isNotNull();
        assertThat(user.email()).isEqualTo("arman@example.com");
        assertThat(user.passwordHash()).isEqualTo("hashed-password");
        assertThat(user.createdAt()).isNotNull();
    }

    @Test
    void registerRejectsBlankEmail() {
        assertThatThrownBy(() -> User.register(" ", "hashed-password"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registerRejectsNullEmail() {
        assertThatThrownBy(() -> User.register(null, "hashed-password"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registerRejectsBlankPasswordHash() {
        assertThatThrownBy(() -> User.register("arman@example.com", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
