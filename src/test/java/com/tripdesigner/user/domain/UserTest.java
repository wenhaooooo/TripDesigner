package com.tripdesigner.user.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void can_register_sets_password_hash_and_active_status() {
        User u = User.register("a@b.com", "hashed");
        assertNull(u.getId());
        assertEquals("a@b.com", u.getEmail());
        assertEquals("hashed", u.getPasswordHash());
        assertEquals(UserStatus.ACTIVE, u.getStatus());
        assertEquals(0, u.getVersion());
    }

    @Test
    void update_nickname_returns_new_instance() {
        User u = User.register("a@b.com", "h").withNickname("wen");
        assertEquals("wen", u.getNickname());
    }
}