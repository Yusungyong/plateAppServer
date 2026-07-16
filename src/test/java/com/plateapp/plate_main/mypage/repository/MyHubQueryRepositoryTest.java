package com.plateapp.plate_main.mypage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@ActiveProfiles("test")
class MyHubQueryRepositoryTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbc;

    private MyHubQueryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MyHubQueryRepository(namedJdbc);
        jdbc.execute("DROP ALL OBJECTS");
        createSchema();
        insertFixture();
    }

    @Test
    void countsAndMergedPreviewsFollowAcceptedSemantics() {
        MyHubQueryRepository.CountsRow counts = repository.findCounts("me");

        assertThat(counts.videoCount()).isEqualTo(2);
        assertThat(counts.imageCount()).isEqualTo(1);
        assertThat(counts.likedContentCount()).isEqualTo(2);
        assertThat(counts.receivedLikeCount()).isEqualTo(2);
        assertThat(counts.friendCount()).isEqualTo(1);
        assertThat(counts.pendingFriendRequestCount()).isEqualTo(1);

        List<MyHubQueryRepository.ContentRow> content = repository.findRecentContent("me", 2);
        assertThat(content).extracting(MyHubQueryRepository.ContentRow::sourceId)
                .containsExactly(1, 2);
        assertThat(content.get(0).createdOn()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(content.get(0).latitude()).isEqualTo(37.5);
        assertThat(content.get(0).longitude()).isEqualTo(127.0);

        List<MyHubQueryRepository.ContentRow> likes = repository.findRecentLikes("me", 2);
        assertThat(likes).extracting(MyHubQueryRepository.ContentRow::sourceId)
                .containsExactly(3, 4);
        assertThat(likes).extracting(MyHubQueryRepository.ContentRow::likedOn)
                .containsExactly(LocalDate.of(2026, 7, 16), LocalDate.of(2026, 7, 15));
    }

    @Test
    void activeUserReportsStoredByTargetUserIdAreExcluded() {
        jdbc.update("""
                INSERT INTO fp_40(
                    reporter_username, target_type, target_user_id, target_flag
                ) VALUES (?, 'user', ?, 'Y')
                """, "me", 2);
        jdbc.update("""
                INSERT INTO fp_40(
                    reporter_username, target_type, target_user_id, target_flag
                ) VALUES (?, 'user', ?, 'Y')
                """, "me", 3);

        MyHubQueryRepository.CountsRow counts = repository.findCounts("me");

        assertThat(counts.likedContentCount()).isZero();
        assertThat(counts.receivedLikeCount()).isZero();
        assertThat(repository.findRecentLikes("me", 6)).isEmpty();
    }

    private void createSchema() {
        jdbc.execute("""
                CREATE TABLE fp_100 (
                    username VARCHAR(50) PRIMARY KEY,
                    user_id INTEGER,
                    nick_name VARCHAR(100),
                    profile_image_url VARCHAR(500),
                    active_region VARCHAR(100),
                    is_private BOOLEAN
                )
                """);
        jdbc.execute("""
                CREATE TABLE fp_300 (
                    store_id INTEGER PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    title VARCHAR(255),
                    thumbnail VARCHAR(500),
                    store_name VARCHAR(255),
                    address VARCHAR(500),
                    place_id VARCHAR(255),
                    file_name VARCHAR(500),
                    open_yn CHAR(1),
                    use_yn CHAR(1),
                    deleted_at DATE,
                    created_at DATE
                )
                """);
        jdbc.execute("""
                CREATE TABLE fp_400 (
                    feed_no INTEGER PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    feed_title VARCHAR(255),
                    thumbnail VARCHAR(500),
                    images CLOB,
                    store_name VARCHAR(255),
                    location VARCHAR(500),
                    place_id VARCHAR(255),
                    use_yn CHAR(1),
                    open_yn CHAR(1),
                    created_at TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE fp_50 (
                    username VARCHAR(50),
                    store_id INTEGER,
                    use_yn CHAR(1),
                    deleted_at DATE,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    PRIMARY KEY (username, store_id)
                )
                """);
        jdbc.execute("""
                CREATE TABLE fp_60 (
                    username VARCHAR(50),
                    feed_id INTEGER,
                    use_yn CHAR(1),
                    deleted_at TIMESTAMP,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    PRIMARY KEY (username, feed_id)
                )
                """);
        jdbc.execute("""
                CREATE TABLE fp_150 (
                    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    username VARCHAR(50),
                    friend_name VARCHAR(50),
                    status VARCHAR(20),
                    initiator_username VARCHAR(50)
                )
                """);
        jdbc.execute("""
                CREATE TABLE fp_160 (
                    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    blocker_username VARCHAR(50),
                    blocked_username VARCHAR(50)
                )
                """);
        jdbc.execute("""
                CREATE TABLE fp_40 (
                    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    reporter_username VARCHAR(50),
                    target_username VARCHAR(50),
                    target_user_id INTEGER,
                    target_type VARCHAR(20),
                    target_id INTEGER,
                    target_flag CHAR(1),
                    unflagged_at TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE fp_310 (
                    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    place_id VARCHAR(255),
                    formatted_address VARCHAR(500),
                    latitude DOUBLE PRECISION,
                    longitude DOUBLE PRECISION,
                    use_yn CHAR(1),
                    deleted_at DATE
                )
                """);
    }

    private void insertFixture() {
        insertUser("me", 1, "Me", false);
        insertUser("author", 2, "Author", false);
        insertUser("liker", 3, "Liker", false);
        insertUser("friend", 4, "Friend", false);
        insertUser("requester", 5, "Requester", false);

        insertVideo(1, "me", "N", LocalDate.of(2026, 7, 15), "p1");
        insertImage(2, "me", "Y", LocalDateTime.of(2026, 7, 14, 10, 0), "p2");
        insertVideo(3, "author", "Y", LocalDate.of(2026, 7, 13), "p1");
        insertImage(4, "author", "Y", LocalDateTime.of(2026, 7, 12, 10, 0), "p2");
        insertVideo(5, "me", "Y", LocalDate.of(2026, 7, 10), "p1");
        insertImage(6, "author", null, LocalDateTime.of(2026, 7, 11, 10, 0), "p2");

        insertVideoLike("me", 3, LocalDateTime.of(2026, 7, 15, 9, 0), LocalDateTime.of(2026, 7, 16, 9, 0));
        insertImageLike("me", 4, LocalDateTime.of(2026, 7, 14, 9, 0), LocalDateTime.of(2026, 7, 15, 9, 0));
        insertImageLike("me", 6, LocalDateTime.of(2026, 7, 11, 9, 0), LocalDateTime.of(2026, 7, 11, 9, 0));
        insertVideoLike("liker", 1, LocalDateTime.of(2026, 7, 10, 9, 0), LocalDateTime.of(2026, 7, 10, 9, 0));
        insertVideoLike("liker", 5, LocalDateTime.of(2026, 7, 11, 9, 0), LocalDateTime.of(2026, 7, 11, 9, 0));
        insertImageLike("liker", 2, LocalDateTime.of(2026, 7, 12, 9, 0), LocalDateTime.of(2026, 7, 12, 9, 0));

        jdbc.update("INSERT INTO fp_150(username, friend_name, status, initiator_username) VALUES (?, ?, ?, ?)",
                "me", "friend", "accepted", "me");
        jdbc.update("INSERT INTO fp_150(username, friend_name, status, initiator_username) VALUES (?, ?, ?, ?)",
                "requester", "me", "pending", "requester");
        jdbc.update("INSERT INTO fp_310(place_id, formatted_address, latitude, longitude, use_yn) VALUES (?, ?, ?, ?, ?)",
                "p1", "Seoul", 37.5, 127.0, "Y");
        jdbc.update("INSERT INTO fp_310(place_id, formatted_address, latitude, longitude, use_yn) VALUES (?, ?, ?, ?, ?)",
                "p2", "Seoul", 37.6, 127.1, "Y");
    }

    private void insertUser(String username, int userId, String nickname, boolean isPrivate) {
        jdbc.update("INSERT INTO fp_100(username, user_id, nick_name, is_private) VALUES (?, ?, ?, ?)",
                username, userId, nickname, isPrivate);
    }

    private void insertVideo(int id, String username, String openYn, LocalDate createdAt, String placeId) {
        jdbc.update("""
                        INSERT INTO fp_300(
                            store_id, username, title, store_name, address, place_id,
                            file_name, open_yn, use_yn, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Y', ?)
                        """,
                id, username, "video-" + id, "store-" + id, "address-" + id, placeId,
                "video-" + id + ".mp4", openYn, Date.valueOf(createdAt));
    }

    private void insertImage(int id, String username, String openYn, LocalDateTime createdAt, String placeId) {
        jdbc.update("""
                        INSERT INTO fp_400(
                            feed_no, username, feed_title, images, store_name, location,
                            place_id, use_yn, open_yn, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, 'Y', ?, ?)
                        """,
                id, username, "image-" + id, "image-" + id + ".jpg", "store-" + id,
                "address-" + id, placeId, openYn, Timestamp.valueOf(createdAt));
    }

    private void insertVideoLike(String username, int storeId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        jdbc.update("""
                        INSERT INTO fp_50(username, store_id, use_yn, created_at, updated_at)
                        VALUES (?, ?, 'Y', ?, ?)
                        """,
                username, storeId, Timestamp.valueOf(createdAt), Timestamp.valueOf(updatedAt));
    }

    private void insertImageLike(String username, int feedId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        jdbc.update("""
                        INSERT INTO fp_60(username, feed_id, use_yn, created_at, updated_at)
                        VALUES (?, ?, 'Y', ?, ?)
                        """,
                username, feedId, Timestamp.valueOf(createdAt), Timestamp.valueOf(updatedAt));
    }
}
