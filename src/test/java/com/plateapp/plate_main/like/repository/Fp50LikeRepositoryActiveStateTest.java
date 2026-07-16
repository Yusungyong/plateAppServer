package com.plateapp.plate_main.like.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateapp.plate_main.like.entity.Fp50Like;
import com.plateapp.plate_main.like.entity.Fp50LikeId;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class Fp50LikeRepositoryActiveStateTest {

    @Autowired
    private Fp50LikeRepository repository;

    @Test
    void useYRowsWithDeletedAtAreNotCurrentLikes() {
        repository.saveAndFlush(like("viewer", 81, null));
        repository.saveAndFlush(like("viewer", 82, LocalDate.of(2026, 7, 15)));

        assertThat(repository.countByIdStoreIdAndUseYnAndDeletedAtIsNull(81, "Y")).isEqualTo(1);
        assertThat(repository.countByIdStoreIdAndUseYnAndDeletedAtIsNull(82, "Y")).isZero();
        assertThat(repository.existsByIdUsernameAndIdStoreIdAndUseYnAndDeletedAtIsNull(
                "viewer", 82, "Y"
        )).isFalse();
        assertThat(repository.findByIdUsernameAndUseYnAndDeletedAtIsNull("viewer", "Y"))
                .extracting(current -> current.getId().getStoreId())
                .containsExactly(81);
        assertThat(repository.findMyActiveLikedStoreIds("viewer", List.of(81, 82)))
                .containsExactly(81);
    }

    private Fp50Like like(String username, int storeId, LocalDate deletedAt) {
        return Fp50Like.builder()
                .id(new Fp50LikeId(username, storeId))
                .useYn("Y")
                .deletedAt(deletedAt)
                .build();
    }
}
