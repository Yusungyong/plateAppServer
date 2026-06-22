package com.plateapp.plate_main.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.plateapp.plate_main.admin.contentverification.dto.ContentVerificationDtos.ActionRequest;
import com.plateapp.plate_main.admin.contentverification.dto.ContentVerificationDtos.AssigneeRequest;
import com.plateapp.plate_main.admin.contentverification.entity.ContentVerification;
import com.plateapp.plate_main.admin.contentverification.repository.ContentVerificationRepository;
import com.plateapp.plate_main.admin.contentverification.service.ContentVerificationService;
import com.plateapp.plate_main.admin.feedback.dto.FeedbackDtos;
import com.plateapp.plate_main.admin.feedback.service.FeedbackService;
import com.plateapp.plate_main.admin.feedmoderation.dto.AdminFeedDtos;
import com.plateapp.plate_main.admin.feedmoderation.service.AdminFeedService;
import com.plateapp.plate_main.admin.seasonal.dto.SeasonalCurationDtos.PublishRequest;
import com.plateapp.plate_main.admin.seasonal.dto.SeasonalCurationDtos.UpsertRequest;
import com.plateapp.plate_main.admin.seasonal.service.SeasonalCurationService;
import com.plateapp.plate_main.admin.security.AdminActor;
import com.plateapp.plate_main.admin.storeoperation.dto.AdminStoreDtos;
import com.plateapp.plate_main.admin.storeoperation.service.AdminStoreService;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.restaurant.entity.Restaurant;
import com.plateapp.plate_main.restaurant.repository.RestaurantRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminOperationsServiceIntegrationTest {
    private static final AdminActor ACTOR = new AdminActor(9001, "admin-test", "SUPER_ADMIN");
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Autowired FeedbackService feedback;
    @Autowired ContentVerificationService verifications;
    @Autowired ContentVerificationRepository verificationRepository;
    @Autowired AdminStoreService stores;
    @Autowired RestaurantRepository restaurantRepository;
    @Autowired AdminFeedService feeds;
    @Autowired SeasonalCurationService curations;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;

    @Test
    void feedbackSupportsCreatePageUpdateAndOptimisticConflict() {
        FeedbackDtos.Response created = feedback.create(
                new FeedbackDtos.CreateRequest("BUG", "Case-sensitive feedback", null), null);
        assertThat(created.content()).isEqualTo("Case-sensitive feedback");
        assertThat(feedback.list(0, 20, null, "bug", "received", null, null).totalElements()).isEqualTo(1);

        FeedbackDtos.Response updated = feedback.update(created.id(),
                new FeedbackDtos.UpdateRequest("in_progress", 9001, "triaged", created.version()), ACTOR, request);
        assertThat(updated.status()).isEqualTo("in_progress");
        assertThatThrownBy(() -> feedback.update(created.id(),
                new FeedbackDtos.UpdateRequest("resolved", 9001, null, created.version()), ACTOR, request))
                .isInstanceOf(AppException.class);
    }

    @Test
    void contentVerificationPreservesWorkflowHistory() {
        ContentVerification entity = verificationRepository.saveAndFlush(
                ContentVerification.create("FEED", "100", 1000));
        var assigned = verifications.assign(
                entity.getId(), new AssigneeRequest(9001, entity.getVersion()), ACTOR, request);
        var approved = verifications.decide(
                entity.getId(), new ActionRequest(assigned.version(), "checked"), "APPROVED", ACTOR, request);
        assertThat(approved.status()).isEqualTo("approved");
        assertThat(verifications.history(entity.getId())).hasSize(2);
    }

    @Test
    void storeAdminStateIsIsolatedWhilePublicVisibilityIsSynchronized() {
        Restaurant restaurant = restaurantRepository.saveAndFlush(
                Restaurant.create("테스트 매장", "서울", null, null, null, "draft"));
        AdminStoreDtos.Response closed = stores.operation(restaurant.getId(),
                new AdminStoreDtos.StatusRequest("temporarily_closed", "명절 휴무", 0L), ACTOR, request);
        AdminStoreDtos.Response visible = stores.visibility(restaurant.getId(),
                new AdminStoreDtos.StatusRequest("visible", "검수 완료", closed.version()), ACTOR, request);

        assertThat(visible.visibilityStatus()).isEqualTo("visible");
        assertThat(stores.history(restaurant.getId())).hasSize(2);
        assertThat(jdbc.queryForObject(
                "select operation_status from admin_store_operations where store_id = ?",
                String.class, restaurant.getId())).isEqualTo("temporarily_closed");
        assertThat(restaurantRepository.findById(restaurant.getId()).orElseThrow().getExposureStatus())
                .isEqualTo("published");
    }

    @Test
    void feedAdminStateIsIsolatedWhilePublicVisibilityIsSynchronized() {
        users.saveAndFlush(User.builder().username("member").role("USER").build());
        jdbc.update("insert into fp_400(feed_no, username, content, use_yn) values (?,?,?,?)",
                -93001, "member", "reported feed", "Y");
        AdminFeedDtos.Response hidden = feeds.visibility(-93001,
                new AdminFeedDtos.ActionRequest("신고 검수", 0L), false, ACTOR, request);

        assertThat(hidden.visibilityStatus()).isEqualTo("hidden");
        assertThat(jdbc.queryForObject(
                "select visibility_status from admin_feed_moderation where feed_id = ?",
                String.class, -93001)).isEqualTo("hidden");
        assertThat(jdbc.queryForObject(
                "select use_yn from fp_400 where feed_no = ?", String.class, -93001)).isEqualTo("N");

        OffsetDateTime tomorrow = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        var created = curations.create(
                new UpsertRequest("여름 특집", null, 1, tomorrow, tomorrow.plusDays(7),
                        List.of(), List.of(), null), ACTOR, request);
        var scheduled = curations.publish(created.id(), new PublishRequest(created.version()), ACTOR, request);
        assertThat(scheduled.status()).isEqualTo("SCHEDULED");
    }
}
