package com.plateapp.plate_main.seasonfood.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
class SeasonStoreMatchRepositoryTest {

  @Mock
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Captor
  private ArgumentCaptor<String> sqlCaptor;

  @Captor
  private ArgumentCaptor<MapSqlParameterSource> paramsCaptor;

  @Test
  void matchTablesReadyChecksAllMatchingTables() {
    when(jdbcTemplate.queryForObject(sqlCaptor.capture(), any(MapSqlParameterSource.class), eq(Boolean.class)))
        .thenReturn(true);

    SeasonStoreMatchRepository repository = new SeasonStoreMatchRepository(jdbcTemplate);

    assertThat(repository.matchTablesReady()).isTrue();
    assertThat(sqlCaptor.getValue()).contains("fp_340_season_match_keyword");
    assertThat(sqlCaptor.getValue()).contains("fp_341_season_store_match");
    assertThat(sqlCaptor.getValue()).contains("fp_342_season_match_evidence");
    assertThat(sqlCaptor.getValue()).contains("fp_343_season_store_match_override");
  }

  @Test
  void findIngredientStoresFiltersRejectedExpiredAndInactiveMatches() {
    when(jdbcTemplate.query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class)))
        .thenReturn(List.of());

    SeasonStoreMatchRepository repository = new SeasonStoreMatchRepository(jdbcTemplate);

    repository.findIngredientStores("ING_GUL", 12, 37.5, 127.0, 3000, "REG_ALL", 21, 0);

    String sql = sqlCaptor.getValue();
    assertThat(sql).contains("m.use_yn = 'Y'");
    assertThat(sql).contains("m.match_status_cd <> 'REJECTED'");
    assertThat(sql).contains("(m.expires_at IS NULL OR m.expires_at > NOW())");
    assertThat(sql).contains("loc.deleted_at IS NULL");
    assertThat(paramsCaptor.getValue().getValue("radius_m")).isEqualTo(3000);
  }
}
