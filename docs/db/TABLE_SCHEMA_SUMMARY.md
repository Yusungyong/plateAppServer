# Table Schema Summary

This file is a compact reference for the main tables used by the backend.

Purpose
- quick lookup of table purpose
- quick lookup of important columns
- quick lookup of major relations

Source of truth
- [backend_schema.md](C:/workspace/plate-main/backend_schema.md)
- implemented entities in `src/main/java`

## Naming

- `fp_100` series: member/auth
- `fp_300` series: video/store content
- `fp_400` series: image feed content
- `fp_900` series: customer support

---

## 1. Member / Auth

### `fp_100` users

Purpose
- member master table

Main columns
- `user_id`: internal numeric member ID
- `username`: login ID, unique
- `password`: hashed password, nullable for some social cases
- `email`
- `phone`
- `role`
- `nick_name`
- `active_region`
- `profile_image_url`
- `code`: member status or code
- `is_private`
- `created_at`
- `updated_at`

Notes
- backend authentication still uses `username` as the main identity key
- several tables also reference `user_id`

### `fp_103` refresh tokens

Purpose
- refresh token storage

Main columns
- `id`
- `username`
- `refresh_token`
- `expiry_date`
- `device_id`
- `created_at`

### `fp_105` login history

Purpose
- login success/failure history

Main columns
- `login_id`
- `username`
- `login_datetime`
- `ip_address`
- `login_status`
- `fail_reason`
- `device_model`
- `os`
- `os_version`
- `app_version`
- `device_id`

Used by
- member monitoring summary
- login risk detection

### `fp_120` email verification

Purpose
- email verification code issue/verify

Main columns
- `id`
- `email`
- `verification_code`
- `is_verified`
- `created_at`
- `verified_at`
- `expires_at`

### `fp_101` profile change history

Purpose
- member/profile/account change history

Main columns
- `history_id`
- `username`
- `before_ex`
- `after_ex`
- `change_tp`
- `created_dt`

Important change codes
- `CD_001`: email
- `CD_002`: phone
- `CD_003`: password
- `CD_004`: role
- `CD_005`: active region
- `CD_006`: profile image
- `CD_007`: account delete
- `CD_008`: nickname
- `CD_009`: privacy/display mode
- `CD_010`: signup

### `fp_110` social account mapping

Purpose
- map internal member to social provider account

Main columns
- `social_id`
- `user_id`
- `provider`
- `provider_user_id`
- `email`
- `display_name`
- `created_at`

Main relation
- `user_id -> fp_100.user_id`

---

## 2. Friend / Block / Visit / Notification

### `fp_150` friend requests and relations

Main columns
- `id`
- `username`
- `friend_name`
- `status`
- `initiator_username`
- `message`
- `accepted_at`
- `created_at`
- `updated_at`

### `fp_160` block relations

Main columns
- `id`
- `blocker_username`
- `blocked_username`
- `blocked_at`

### `fp_200` visits with friends

Main columns
- `id`
- `username`
- `friend_name`
- `store_id`
- `store_name`
- `memo`
- `visit_date`
- `address`
- `feed_id`
- `created_at`
- `updated_at`

### `fp_20` notifications

Main columns
- `id`
- `receiver_id`
- `sender_id`
- `type`
- `reference_id`
- `message`
- `is_read`
- `created_at`
- `comment_id`
- `reply_id`

---

## 3. Region / Place / Code

### `fp_005` region master

Main columns
- `id`
- `region_name`
- `depth`
- `parent_id`

### `fp_310` places

Main columns
- `id`
- `formatted_address`
- `latitude`
- `longitude`
- `place_id`
- `types`
- `locality`
- `administrative_area_level_1`
- `administrative_area_level_2`
- `country`
- `postal_code`
- `use_yn`
- `deleted_at`

### `fp_code` common code table

Main columns
- `code`
- `group_code`
- `use_yn`
- `code_ex`
- `group_code_ex`
- `created_at`
- `updated_at`

Important groups
- `001`: store/content change type
- `002`: feed sort/filter
- `003`: user profile change type
- `004`: friend relation status
- `005`: user display type
- `006`: home screen display type

---

## 4. Video / Store Domain

### `fp_300` video/store content

Purpose
- uploaded video content metadata

Main columns
- `store_id`
- `title`
- `file_name`
- `thumbnail`
- `username`
- `store_name`
- `address`
- `place_id`
- `open_yn`
- `use_yn`
- `deleted_at`
- `video_duration`
- `video_size`
- `mute_yn`
- `created_at`
- `updated_at`

Notes
- current backend stores video path metadata here
- `file_name` is used as stored video path

### `fp_301` video change history

Main columns
- `change_hist_seq`
- `store_id`
- `change_name`
- `change_tp`
- `before_ex`
- `after_ex`
- `created_at`

### `fp_303` video thumbnail/watch exposure log

Main columns
- `id`
- `username`
- `store_id`
- `watched_at`
- `guest_id`
- `is_guest`

### `fp_305` video watch history

Main columns
- `id`
- `username`
- `store_id`
- `user_id`
- `timestamp`
- `duration_watched`
- `device_info`
- `ip_address`
- `session_id`
- `video_quality`
- `completion_status`
- `comments`
- `use_yn`
- `deleted_at`

Main relation
- `user_id -> fp_100.user_id`

### `fp_50` video likes

Main columns
- `username`
- `store_id`
- `use_yn`
- `deleted_at`
- `created_at`
- `updated_at`

### `fp_440` video comments

Main columns
- `comment_id`
- `store_id`
- `username`
- `user_id`
- `content`
- `use_yn`
- `deleted_at`
- `created_at`
- `updated_at`

Main relation
- `user_id -> fp_100.user_id`

### `fp_450` video replies

Main columns
- `reply_id`
- `comment_id`
- `username`
- `content`
- `use_yn`
- `deleted_at`
- `created_at`
- `updated_at`

### `fp_320` menu items

Main columns
- `item_id`
- `store_id`
- `item_name`
- `price`
- `description`
- `menu_image`
- `feed_id`
- `menu_title`
- `place_id`
- `store_name`
- `use_yn`
- `deleted_at`

---

## 5. Image Feed Domain

### `fp_400` image feeds

Main columns
- `feed_no`
- `username`
- `feed_title`
- `content`
- `images`
- `location`
- `store_name`
- `place_id`
- `thumbnail`
- `use_yn`
- `created_at`
- `updated_at`

### `fp_401` image feed change history

Main columns
- `history_id`
- `feed_id`
- `username`
- `change_code`
- `before_info`
- `after_info`
- `ip_addr`
- `created_at`

### `fp_60` image feed likes

Main columns
- `username`
- `feed_id`
- `use_yn`
- `deleted_at`
- `created_at`
- `updated_at`

### `fp_460` image comments

Main columns
- `comment_id`
- `feed_id`
- `username`
- `content`
- `use_yn`
- `deleted_at`
- `created_at`
- `updated_at`

### `fp_470` image replies

Main columns
- `reply_id`
- `comment_id`
- `username`
- `content`
- `use_yn`
- `deleted_at`
- `created_at`
- `updated_at`

### `fp_350` tags

Main columns
- `id`
- `store_id`
- `feed_id`
- `tags`
- `created_at`
- `updated_at`

### `fp_360` guestbook

Main columns
- `gb_id`
- `store_id`
- `feed_id`
- `username`
- `target_username`
- `content`
- `custom_images`
- `visibility`
- `display_image`
- `store_name`
- `place_id`
- `is_hidden`
- `writer_profile_image`
- `write_ip`
- `user_agent`
- `created_at`
- `updated_at`

---

## 6. Moderation / Reporting / Monitoring

### `fp_40` reports

Main columns
- `id`
- `reporter_username`
- `target_username`
- `target_type`
- `target_id`
- `reason`
- `submitted_at`
- `target_flag`
- `unflagged_at`

### `fp_410` user feed filter state

Main columns
- `username`
- `filter_type`
- `image_yn`
- `time_filter`
- `region_filter`
- `post_sorted`
- `sort_code`
- `post_source`
- `created_at`
- `updated_at`

### `fp_411` user filter history

Main columns
- `history_id`
- `username`
- `filter_type`
- `condition_1`
- `condition_2`
- `condition_3`
- `condition_4`
- `sort_code`
- `created_at`

---

## 7. Seasonal / Category Master

### `fp_340` seasonal foods

Main columns
- `id`
- `seasonal_term`
- `month`
- `category`
- `food_name`

### `fp_341` seasonal term date ranges

Main columns
- `id`
- `seasonal_term`
- `start_date`
- `end_date`

### `fp_99` menu/category master

Main columns
- `menu_id`
- `name`
- `category`

---

## 8. Customer Support

### `fp_900` FAQ

Source
- implemented entity: [Fp900Faq.java](C:/workspace/plate-main/src/main/java/com/plateapp/plate_main/faq/entity/Fp900Faq.java)

Main columns
- `faq_id`
- `username`
- `category`
- `title`
- `answer`
- `is_pinned`
- `view_count`
- `display_order`
- `status_code`
- `created_at`
- `updated_at`

Used by
- FAQ list/detail
- FAQ admin create/update/delete

### `fp_901` QnA

Source
- implemented entity: [Fp901Qna.java](C:/workspace/plate-main/src/main/java/com/plateapp/plate_main/qna/entity/Fp901Qna.java)

Main columns
- `qna_id`
- `username`
- `guest_name`
- `guest_email`
- `category`
- `question`
- `answer`
- `status_code`
- `is_public`
- `created_at`
- `updated_at`
- `answered_at`

Used by
- public QnA create/list/detail
- admin answer/update

---

## 9. Tables With Direct `user_id -> fp_100.user_id` FK

These are especially important for account deletion.

- `fp_110`
- `fp_305`
- `fp_440`

If `fp_100` is hard-deleted, these references must be removed, deleted, or nulled first.

---

## 10. Tables With Heavy `username`-Based Coupling

These are also important for account deletion, anonymization, and content retention policy.

- `fp_105`
- `fp_150`
- `fp_160`
- `fp_200`
- `fp_300`
- `fp_400`
- `fp_440`
- `fp_450`
- `fp_460`
- `fp_470`
- `fp_360`
- `fp_40`

Even if `fp_100` is deleted successfully, these tables may still retain the deleted user's `username`.

---

## 11. Recommended Usage

Use this file for:
- quick schema lookup
- API implementation context
- account deletion impact review
- monitoring/reporting feature planning

Use [backend_schema.md](C:/workspace/plate-main/backend_schema.md) when:
- exact constraints are needed
- exact indexes are needed
- exact column types/defaults are needed
