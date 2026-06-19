# 입점 신청 계정 중복 확인 프론트엔드 연동 요청

작성일: 2026-06-19  
대상: 접시 프론트엔드 입점 신청 담당자

## 1. 목적

입점 신청의 계정 입력 단계에서 회원 ID, 이메일, 닉네임의 형식과 중복 여부를 입력 필드의 `blur` 시점에 확인한다.

이 API는 타이핑할 때마다 호출하지 않는다. 사용자가 필드 입력을 마치고 다른 필드로 이동했을 때만 호출한다.

## 2. 계정 필드 검증 API

```http
POST /api/owner/signup-account-validations
Content-Type: application/json
```

- 비로그인 공개 API이므로 `Authorization` 헤더가 필요하지 않다.
- 서버 rate limit은 IP 기준 1분에 60회다.
- 한 요청에서는 필드 하나만 검사한다.

### 요청

```json
{
  "field": "username",
  "value": "owner01"
}
```

`field` 허용값:

| field | 서버 정규화 및 검증 |
|---|---|
| `username` | 앞뒤 공백 제거, 영문/숫자 4~30자 |
| `email` | 앞뒤 공백 제거, 이메일 형식 확인, 소문자 변환, 최대 320자 |
| `nickname` | 앞뒤 공백 제거, 1~100자 |

### 사용 가능 응답

```json
{
  "success": true,
  "data": {
    "field": "username",
    "value": "owner01",
    "available": true,
    "message": "사용 가능한 회원 ID입니다."
  },
  "requestId": "...",
  "timestamp": "2026-06-19T00:00:00Z"
}
```

### 중복 응답

중복은 요청 오류가 아니므로 HTTP 200과 `success=true`로 반환된다.

```json
{
  "success": true,
  "data": {
    "field": "email",
    "value": "owner@example.com",
    "available": false,
    "message": "이미 가입된 이메일입니다."
  },
  "requestId": "...",
  "timestamp": "2026-06-19T00:00:00Z"
}
```

서버가 반환하는 `data.value`는 정규화된 값이다. 특히 이메일은 소문자로 반환된다.

## 3. 프론트 처리 요구사항

각 필드는 다음 상태를 독립적으로 관리한다.

```text
idle → checking → available
                ↘ duplicate
                ↘ error
```

필수 처리 사항:

1. 기본 형식 검사를 통과한 값만 blur API로 전송한다.
2. 입력값이 변경되면 해당 필드의 기존 `available` 상태를 즉시 `idle`로 초기화한다.
3. `checking` 중에는 동일한 값으로 중복 요청하지 않는다.
4. 요청이 겹치면 이전 요청을 취소하거나, 응답의 `data.value`가 현재 정규화 값과 같은 경우에만 결과를 반영한다.
5. `available=false`이면 `data.message`를 해당 입력 필드 아래에 표시한다.
6. 세 필드가 모두 `available`인 경우에만 프론트의 다음 단계 또는 최종 제출을 허용한다.
7. 서버 검증 완료 후 값을 다시 수정했다면 반드시 재검증한다.

권장 호출 예시:

```ts
type AccountField = 'username' | 'email' | 'nickname';

async function validateAccountField(field: AccountField, value: string) {
  const response = await fetch('/api/owner/signup-account-validations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ field, value }),
  });

  if (!response.ok) {
    throw await response.json();
  }

  return response.json();
}
```

## 4. 오류 처리

### 형식 오류 또는 지원하지 않는 field: HTTP 400

```json
{
  "success": false,
  "message": "회원 ID는 영문과 숫자로 구성된 4~30자여야 합니다.",
  "errorCode": "COMMON_400"
}
```

프론트 기본 검사에서 걸러지는 것이 정상 흐름이며, 400이 반환되면 해당 필드를 `error` 상태로 처리한다.

### rate limit 초과: HTTP 429

```json
{
  "success": false,
  "message": "Too many requests. Please try again later.",
  "errorCode": "COMMON_429"
}
```

429 발생 시 자동 반복 호출하지 말고 잠시 후 다시 시도하도록 안내한다.

### 네트워크 또는 서버 오류

사용 가능으로 간주하지 않는다. 필드를 `error` 상태로 두고 사용자가 재시도할 수 있게 한다.

## 5. 최종 입점 신청의 중복 응답

blur 검증 통과는 계정 생성을 예약하지 않는다. 검증 후 최종 제출 전까지 다른 사용자가 동일 값을 먼저 사용할 수 있으므로 서버가 최종 제출 시 다시 검사한다.

```http
POST /api/owner/signup-applications
```

최종 재검사에서 중복이 발견되면 HTTP 409가 반환된다.

```json
{
  "success": false,
  "data": {
    "fieldErrors": {
      "username": "이미 사용 중인 회원 ID입니다.",
      "email": "이미 가입된 이메일입니다.",
      "nickname": "이미 사용 중인 닉네임입니다."
    }
  },
  "message": "이미 사용 중인 계정 정보가 있습니다.",
  "errorCode": "ACCOUNT_CONFLICT"
}
```

- `fieldErrors`에는 실제로 중복된 필드만 포함된다.
- 키는 `username`, `email`, `nickname`이다.
- 409를 받으면 사용자를 계정 입력 단계로 이동시키고 각 메시지를 해당 필드에 표시한다.
- 해당 필드의 이전 `available` 상태는 `duplicate`로 변경한다.

## 6. 완료 조건

- username, email, nickname 각각 blur 시 한 번씩 검증 API를 호출한다.
- 값 변경 후 과거의 사용 가능 결과가 유지되지 않는다.
- 늦게 도착한 과거 요청의 응답이 현재 입력 상태를 덮어쓰지 않는다.
- `available=false`, 400, 429, 네트워크 오류를 구분해 처리한다.
- 최종 신청의 `ACCOUNT_CONFLICT` 응답을 `data.fieldErrors` 기준으로 필드에 표시한다.
