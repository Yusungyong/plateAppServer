package com.plateapp.plate_main.seasonfood.repository;

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
class SeasonFoodRepositoryTest {

  @Mock
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Captor
  private ArgumentCaptor<String> sqlCaptor;

  @Captor
  private ArgumentCaptor<MapSqlParameterSource> paramsCaptor;

  @Test
  void findSeasonFoodsAppliesActiveFiltersAndOrdering() {
    when(jdbcTemplate.query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of());

    SeasonFoodRepository repository = new SeasonFoodRepository(jdbcTemplate);

    repository.findSeasonFoods(12, "CAT_SEAFOOD", "REG_ALL", 60, 21, 0);

    String sql = sqlCaptor.getValue();
    assertThat(sql).contains("WITH RECURSIVE category_filter");
    assertThat(sql).contains("ms.month_no = :month");
    assertThat(sql).contains("ms.season_score >= :min_score");
    assertThat(sql).contains("i.use_yn = 'Y'");
    assertThat(sql).contains("w.use_yn = 'Y'");
    assertThat(sql).contains("ORDER BY ms.is_peak_yn DESC");
  }

  @Test
  void findSeasonWindowsIncludesGlobalRegionWhenSpecificRegionIsRequested() {
    when(jdbcTemplate.query(any(String.class), paramsCaptor.capture(), any(RowMapper.class)))
        .thenReturn(List.of());

    SeasonFoodRepository repository = new SeasonFoodRepository(jdbcTemplate);

    repository.findSeasonWindows("ING_GUL", "REG_JEJU");

    Object regionIds = paramsCaptor.getValue().getValue("region_ids");
    assertThat(regionIds).isEqualTo(List.of("REG_JEJU", "REG_ALL"));
  }
}
