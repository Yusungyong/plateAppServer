package com.plateapp.plate_main.friend.repository;

import com.plateapp.plate_main.friend.entity.Fp150Friend;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Fp150FriendRepository extends JpaRepository<Fp150Friend, Integer> {

    List<Fp150Friend> findByUsername(String username);

    List<Fp150Friend> findByUsernameAndStatus(String username, String status);

    Optional<Fp150Friend> findByUsernameAndFriendName(String username, String friendName);
}
