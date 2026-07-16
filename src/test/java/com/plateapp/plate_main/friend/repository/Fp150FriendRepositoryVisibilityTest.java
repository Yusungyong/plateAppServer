package com.plateapp.plate_main.friend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateapp.plate_main.friend.entity.Fp150Friend;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class Fp150FriendRepositoryVisibilityTest {

    @Autowired
    private Fp150FriendRepository friendRepository;

    @Test
    void recognizesBothAcceptedStatusFamiliesInEitherDirection() {
        friendRepository.saveAndFlush(friend("viewer", "accepted-user", "accepted"));
        friendRepository.saveAndFlush(friend("code-user", "viewer", "cd_002"));
        friendRepository.saveAndFlush(friend("viewer", "pending-user", "pending"));

        assertThat(friendRepository.existsAcceptedRelationship("viewer", "accepted-user")).isTrue();
        assertThat(friendRepository.existsAcceptedRelationship("viewer", "code-user")).isTrue();
        assertThat(friendRepository.existsAcceptedRelationship("viewer", "pending-user")).isFalse();
    }

    private Fp150Friend friend(String username, String friendName, String status) {
        Fp150Friend friend = new Fp150Friend();
        friend.setUsername(username);
        friend.setFriendName(friendName);
        friend.setStatus(status);
        return friend;
    }
}
