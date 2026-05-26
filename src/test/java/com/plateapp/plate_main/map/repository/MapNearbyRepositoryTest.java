package com.plateapp.plate_main.map.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class MapNearbyRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    @Test
    void findNearbyFiltersInactiveClosedDeletedStoresAndLocations() {
        when(jdbcTemplate.query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        MapNearbyRepository repository = new MapNearbyRepository(jdbcTemplate);

        repository.findNearby(
                37.5,
                127.0,
                1000,
                20,
                List.of(),
                null,
                null,
                null,
                List.of()
        );

        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("loc.use_yn = 'Y'");
        assertThat(sql).contains("loc.deleted_at IS NULL");
        assertThat(sql).contains("s.use_yn = 'Y'");
        assertThat(sql).contains("s.open_yn = 'Y'");
        assertThat(sql).contains("s.deleted_at IS NULL");
    }
}
