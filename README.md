# Melissa

## 0. 프로젝트 설명

<p align="center">
  <img src="https://github.com/user-attachments/assets/a5e84a2c-6f37-4f04-8b7e-c7e2b39bffda" width="24%" />
  <img src="https://github.com/user-attachments/assets/d752e88d-8b5d-41fb-8113-3b12278d19f9" width="24%" />
  <img src="https://github.com/user-attachments/assets/3b250cd6-64eb-4240-b9f7-6319cd017818" width="24%" />
  <img src="https://github.com/user-attachments/assets/db3310c7-81eb-4b67-800b-c20b8419cd9a" width="24%" />
</p>

Melissa는 사용자가 AI 캐릭터와 대화하며 하루를 정리하고, 대화 내용을 바탕으로 그림일기를 만드는 서비스입니다. 빈 화면에서 일기를 시작하기 어려운 문제를 대화형 기록, 개인화된 기억, 이미지 생성과 리마인드로 해결하는 것을 목표로 했습니다.

백엔드에서는 OpenAI, Google, Apple, Expo, S3처럼 서로 다른 외부 시스템을 연결하면서 발생하는 **요청 중복, 장기 트랜잭션, 비동기 상태 전이와 결제 권한 정합성**을 다뤘습니다. 기능이 한 번 성공하는 데 그치지 않고, 재시도와 부분 실패 이후에도 상태를 설명하고 복구할 수 있는 구조를 만드는 데 집중했습니다.

### 핵심 백엔드 과제

- AI 대화와 일기·이미지 생성을 하나의 사용자 기록 흐름으로 연결
- 외부 API 지연이 DB 트랜잭션과 커넥션 풀에 전파되지 않도록 경계 분리
- 동일 요청 재전송 시 메시지·사용량·외부 호출이 중복되지 않도록 멱등 처리
- 비동기 결과를 명시적 상태와 전이 규칙으로 관리
- 스토어 구매 검증부터 권한 부여·복원·환불까지 결제 생명주기 구성

## 1. 아키텍처

<img width="8192" height="1574" alt="Mermaid Chart - Create complex, visual diagrams with text -2026-04-02-205143" src="https://github.com/user-attachments/assets/27b37f65-ee5b-469c-9e30-d9f35ff4f4de" />

주요 외부 연동은 DB 작업과 한 트랜잭션으로 묶지 않고 아래 흐름으로 처리합니다.

```text
요청 검증·상태 준비     외부 시스템 호출            결과 확정
prepare transaction  -> OpenAI / S3 / OAuth / Push -> finalize transaction
```

결제 영역은 스토어의 구매 사실과 서비스가 제공하는 권한을 분리해 관리합니다.

```text
Google Play / App Store 검증
            -> Payment 기록
            -> Entitlement 부여·복원·회수
            -> PaymentEvent 감사 이력
```

## 2. 사용 기술

| 구분 | 기술 |
| --- | --- |
| Backend | Java 17, Spring Boot 3.3.1, Spring Data JPA, Spring Security |
| Database | MySQL, HikariCP |
| AI | Spring AI 1.1.0, OpenAI, DALL-E |
| External API | Google Play, Apple App Store, Expo Push, Google·Kakao·Apple OAuth |
| Auth / Security | JWT, Refresh Token Rotation, AES-256-CBC |
| Infra | AWS Elastic Beanstalk, EC2, RDS, S3, Route 53, CloudWatch |
| Operation | Swagger/OpenAPI, Scheduler, Async Executor, WebClient |

## 3. 핵심 문제 해결

### 3-1. 외부 API 지연이 DB 트랜잭션을 장시간 점유하는 문제

채팅, 이미지 생성, 소셜 로그인 검증, 푸시 발송은 모두 외부 시스템의 응답시간과 장애에 영향을 받습니다. 외부 호출을 `@Transactional` 메서드 안에서 기다리면 DB 커넥션 점유 시간이 길어지고 외부 장애가 내부 저장 흐름까지 함께 흔들릴 수 있었습니다.

처리 흐름을 `prepare -> external call -> finalize`로 재구성하고, 다단계 흐름은 `TransactionTemplate`로 경계를 명시했습니다. 검증과 최소 상태 저장, 외부 호출, 최종 반영을 각각 분리해 DB 트랜잭션은 짧게 유지했습니다. 소셜 로그인, 테스트 채팅, 일기 생성·수정, 메모리 갱신과 알림 상태 저장에도 같은 기준을 적용했습니다.

- 관련 PR: [#272 긴 트랜잭션 및 외부 API 구간 분리](https://github.com/team-Melissa/melissa-BE/pull/272)

### 3-2. 중복 요청이 데이터와 외부 API 비용을 함께 증가시키는 문제

모바일 네트워크 재시도나 연속 요청으로 같은 채팅이 다시 처리되면 메시지 저장, 사용량 차감과 OpenAI 호출이 모두 중복될 수 있습니다. 이를 요청 내용의 중복이 아닌 **같은 요청 시도의 재전송** 문제로 정의했습니다.

- `Idempotency-Key`가 있는 요청에 선택적 멱등 처리 적용
- `(user_id, endpoint, idempotency_key)` unique constraint로 동시 요청 경쟁 차단
- 동일 키·동일 본문은 저장된 응답을 재사용하고, 동일 키·다른 본문은 충돌로 처리
- `PENDING`, `SUCCEEDED`, `FAILED_RETRYABLE`, `FAILED_FINAL` 상태로 처리 결과 구분
- 24시간 TTL과 만료 레코드 정리 스케줄러 적용
- 중복 INSERT 이후 Hibernate 세션이 깨지는 문제를 `INSERT IGNORE + SELECT FOR UPDATE`로 해결

- 관련 PR: [#271 채팅·푸시토큰 HTTP 멱등 처리](https://github.com/team-Melissa/melissa-BE/pull/271)

### 3-3. 비동기 이미지 생성 결과를 URL의 null 여부로 판단하던 문제

이미지 생성이 요청과 다른 시점에 끝나기 때문에 `imageUrl`만으로는 생성 전, 처리 중, 실패를 구분할 수 없었습니다. 오래된 이벤트가 뒤늦게 도착하면 최신 상태를 덮어쓸 가능성도 있었습니다.

`DiaryImageStatus`를 도입해 상태를 `NONE -> PENDING -> READY/FAILED`로 명시하고, 도메인 전이 메서드와 DB `NOT NULL`, 기본값, check constraint를 함께 적용했습니다. 비동기 완료 이벤트는 현재 상태가 `PENDING`일 때만 반영해 stale 이벤트를 차단했습니다. 기존 데이터는 URL 존재 여부에 따라 `READY`와 `NONE`으로 안전하게 백필했습니다.

- 관련 PR: [#262 Diary 이미지 상태와 전이 가드 적용](https://github.com/team-Melissa/melissa-BE/pull/262)

### 3-4. 외부 실패를 모두 같은 방식으로 처리하던 문제

timeout이나 rate limit 같은 일시 장애와 잘못된 요청, 만료된 푸시 토큰은 복구 방법이 다릅니다. 서비스마다 달랐던 실패 처리를 `RetryPolicy`, `RetryClassifier`, `RetryExecutor`로 표준화했습니다.

- timeout, connection error, 408·409·425·429·5xx, AWS throttling은 제한적으로 재시도
- 일반 4xx와 잘못된 입력은 즉시 실패 처리
- Expo invalid token은 비활성화하고 재시도 대상에서 제외
- S3 업로드 최종 실패가 성공처럼 처리되지 않도록 예외 전파
- 외부 OAuth 검증에 connect/read timeout 적용

- 관련 PR: [#278 실패 재시도 정책 표준화](https://github.com/team-Melissa/melissa-BE/pull/278)

### 3-5. 스토어 결제와 서비스 권한을 하나의 상태로 다룰 수 없는 문제

구매 검증 결과, 사용자에게 제공되는 광고 제거 권한, 환불 이력은 서로 다른 생명주기를 갖습니다. 이를 `Payment`, `Entitlement`, `PaymentEvent`로 분리하고 플랫폼 이벤트가 서비스 권한에 반영되는 흐름을 구성했습니다.

- Google Play·Apple App Store 비소모성 상품 구매 검증과 복원 API
- 동일 구매 재요청 멱등 처리와 다른 사용자에게 연결된 구매 차단
- 검증 성공 시 결제 저장과 `REMOVE_ADS` 권한 부여
- Google acknowledge 실패 재시도
- 관리자 환불 fallback에서 결제 상태 변경과 권한 회수
- 이미 처리된 환불은 동일 결과로 수렴하고 비정상적으로 남은 권한도 회수
- 상태 변경을 `PaymentEvent`에 기록해 처리 이력 추적

- 관련 PR: [#288 결제·권한 기반 모델](https://github.com/team-Melissa/melissa-BE/pull/288), [#290 Google·Apple 구매 검증과 복원](https://github.com/team-Melissa/melissa-BE/pull/290), [#292 관리자 환불 fallback](https://github.com/team-Melissa/melissa-BE/pull/292)

### 3-6. S3 절대 URL 저장이 인프라 변경 비용을 키우는 문제

DB에 버킷 절대 URL을 저장하면 계정이나 공개 도메인이 바뀔 때 운영 데이터를 함께 마이그레이션해야 합니다. 신규 데이터는 object key만 저장하고, 응답 시점에 현재 public base URL을 조합하도록 전환했습니다.

`S3AssetUrlResolver`는 기존 absolute URL, `s3://` URI와 신규 key를 모두 읽을 수 있습니다. 기존 데이터의 읽기 호환성을 유지하면서 새로운 쓰기부터 점진적으로 key 중심 구조로 옮겼습니다.

- 관련 PR: [#274 S3 URL 비의존화 및 asset key 전환](https://github.com/team-Melissa/melissa-BE/pull/274)

## 4. 추가 운영 개선

| 영역 | 개선 내용 |
| --- | --- |
| Commit 시점 | 이미지 생성 이벤트를 `AFTER_COMMIT` 이후 실행해 저장 직후 조회에서 발생하던 데이터 가시성 문제 해결 |
| Push | Expo 응답의 객체·배열 형태를 모두 처리하고, 성공 마킹·실패 재시도·invalid token 비활성화 상태 분리 |
| Timezone | 알림 스케줄과 streak 계산을 `Asia/Seoul` 기준으로 통일 |
| Terms | 약관과 버전을 분리하고 버전별 사용자 동의 이력 및 재동의 필요 여부 관리 |
| Security | JWT type claim 검증, refresh token hash 저장과 기존 토큰 무력화 적용 |
| Observability | 애플리케이션·NGINX·EB 로그를 CloudWatch에 수집하고 보존 주기를 서비스 기준으로 구성 |

## 5. 검증 포인트

- 같은 멱등 키·같은 본문 재요청 시 비즈니스 로직을 다시 실행하지 않고 저장 응답 반환
- 같은 멱등 키·다른 본문 요청 충돌 처리
- 이미지 생성 `PENDING -> READY/FAILED` 전이와 stale 이벤트 차단 검증
- 결제 중복 검증, 복원, 권한 부여와 환불 후 권한 회수 테스트
- retryable·non-retryable 예외 분류와 S3·Expo Push 최종 상태 검증
- 마이그레이션마다 precheck·apply·post-check·rollback 절차 제공
