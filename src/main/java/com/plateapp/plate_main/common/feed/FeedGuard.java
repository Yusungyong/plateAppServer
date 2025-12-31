package com.plateapp.plate_main.common.feed;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FeedGuard {
  private final JdbcTemplate jdbcTemplate;

  public FeedGuard(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void assertFeedExists(int feedId) {
    Integer exists = jdbcTemplate.queryForObject(
        "select 1 from fp_400 where feed_no = ? and use_yn = 'Y' limit 1",
        Integer.class,
        feedId
    );
    if (exists == null) throw new java.util.NoSuchElementException("feed not found");
  }
}
