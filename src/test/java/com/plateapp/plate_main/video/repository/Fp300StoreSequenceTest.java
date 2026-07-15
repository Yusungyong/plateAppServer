package com.plateapp.plate_main.video.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateapp.plate_main.video.entity.Fp300Store;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class Fp300StoreSequenceTest {

    @Autowired private Fp300StoreRepository storeRepository;

    @Test
    void assignsDistinctIdsWithoutMaxIdLookup() {
        Fp300Store first = storeRepository.saveAndFlush(store("owner-1"));
        Fp300Store second = storeRepository.saveAndFlush(store("owner-2"));

        assertThat(first.getStoreId()).isPositive();
        assertThat(second.getStoreId()).isEqualTo(first.getStoreId() + 1);
    }

    private Fp300Store store(String username) {
        Fp300Store store = new Fp300Store();
        store.setUsername(username);
        store.setUseYn("Y");
        return store;
    }
}
