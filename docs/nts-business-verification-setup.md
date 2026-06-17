# 국세청 사업자등록정보 검증 설정 안내

## 서버에서 구현된 것

- 공개 API: `POST /api/owner/business-verifications`
- 신청 생성/수정 시 `business` 객체의 국세청 검증 결과 저장
- 제출 조건 변경: `business_verification_status = verified` 또는 `business_registration` 문서 존재 시 제출 가능
- 사업자등록번호는 숫자 10자리로 검증하며, 원문은 로그에 남기지 않는다.

## 운영자가 준비해야 하는 것

1. 공공데이터포털에서 국세청 사업자등록정보 진위확인 API 활용 신청
   - API: 국세청 사업자등록정보 진위확인
   - 서버 호출 엔드포인트: `https://api.odcloud.kr/api/nts-businessman/v1/validate`

2. 서비스키를 서버 secret으로 주입

```bash
NTS_BUSINESS_SERVICE_KEY=공공데이터포털_서비스키
```

3. 선택 환경변수

```bash
NTS_BUSINESS_BASE_URL=https://api.odcloud.kr/api/nts-businessman/v1
NTS_BUSINESS_TIMEOUT_MS=5000
NTS_BUSINESS_VERIFICATION_ENABLED=true
```

4. 로컬 개발 설정

`src/main/resources/application-local.example.yaml`에는 키를 비워 둔다.

```yaml
external:
  nts-business:
    service-key:
```

실제 키는 `application-local.yaml`, OS 환경변수, 배포 secret manager 중 하나로만 주입한다.

## 주의사항

- 서비스키를 Git, 프론트 번들, 로그에 노출하지 않는다.
- 국세청 원문 응답 전체를 로그에 남기지 않는다.
- API 장애 또는 키 미설정 시 서버는 `BUSINESS_VERIFICATION_UNAVAILABLE`을 반환한다.
- 공개 API이므로 현재 서버에서 IP와 사업자번호 기준 rate limit을 적용한다.
