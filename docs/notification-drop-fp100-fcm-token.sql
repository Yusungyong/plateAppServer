-- fp_24 적재 경로 배포 후 실행
-- 전제:
-- 1. 로그인/소셜 로그인 요청에서 fcmToken, deviceId를 보내고 있어야 함
-- 2. 서버가 fp_24에 토큰을 upsert 하도록 배포되어 있어야 함
-- 3. PushNotificationService 가 fp_24만 사용하도록 반영되어 있어야 함

alter table fp_100
    drop column if exists fcm_token;
