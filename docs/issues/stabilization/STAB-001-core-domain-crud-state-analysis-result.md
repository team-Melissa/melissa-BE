# STAB-001 결과: 핵심 도메인 CRUD/상태전이/불변식 분석

작성일: 2026-03-09  
브랜치: `252-feat-핵심-도메인-crud상태전이불변식-분석`

## 1. 분석 목적
- STAB-002(DB 제약), STAB-003(멱등), STAB-004(상태전이), STAB-005(재시도), STAB-006(트랜잭션 분리)의 입력값을 확정한다.
- “어디에서 무엇이 생성/수정/삭제되는지”, “상태가 어떤 경로로 변하는지”, “절대 깨지면 안 되는 규칙이 무엇인지”를 코드 기준으로 정리한다.

## 2. 분석 범위
- 핵심 도메인: `diary`, `thread`, `daily_chat_log`, `user_setting`, `user_memory`, `expo_push_token`
- 참고 코드:
  - `src/main/java/com/melissa/diary/domain/*`
  - `src/main/java/com/melissa/diary/service/*`
  - `src/main/java/com/melissa/diary/web/controller/*`
  - `src/main/java/com/melissa/diary/repository/*`
  - `src/main/java/com/melissa/diary/scheduler/*`

## 3. 도메인별 CRUD 매트릭스 (As-Is)
| 도메인 | Create | Read | Update | Delete | 비고 |
|---|---|---|---|---|---|
| `diary` | `POST /api/v1/diaries/manual`, `POST /api/v1/diaries/from-chat` | 캘린더/피드 조회에서 사용 | `PUT /api/v1/diaries/{diaryId}` | `DELETE /api/v1/diaries/{diaryId}`(soft delete) | 일자별 최대 3개 제한은 코드 카운트 기반 |
| `thread` | `POST /api/v1/chats` | `GET /api/v1/chats` | (명시적 update 없음) | `DELETE /api/v1/chats` | `(user, aiProfile, y,m,d)` 유니크 |
| `daily_chat_log` | 채팅 전송 시 user/ai 메시지 저장 | thread 조회 시 함께 조회 | `PATCH /api/v1/chats/{chatLogId}`(USER만) | `DELETE /api/v1/chats/{chatLogId}`(owner만) | AI 메시지 수정 금지 |
| `user_setting` | `POST /api/v1/user-settings/register` (기본값 생성) | `GET /api/v1/user-settings` | `PUT /api/v1/user-settings` | 회원탈퇴 시 `deleteByUserId` | user당 1개 유니크 |
| `user_memory` | 최초 조회/업데이트 시 lazy create | `GET /api/v1/memory` | 다이어리 기반 스케줄 업데이트, reset | API는 reset(내용 비움), 회원탈퇴 시 현재 명시 삭제 없음 | user당 1개 유니크 |
| `expo_push_token` | `POST /api/v1/expo-push-tokens` | `GET /api/v1/expo-push-tokens` | 같은 토큰 재등록 시 재활성화/소유자 갱신 | `DELETE /api/v1/expo-push-tokens/{token}` | 토큰 문자열 유니크 |

## 4. 상태 전이 규칙 (As-Is)
### 4.1 `Diary`
- 상태 필드: `isActive`, `version`
- 전이:
  - `생성` -> `ACTIVE(isActive=true, version=1)`
  - `수정` -> `ACTIVE(version++)`
  - `삭제` -> `DELETED(isActive=false)` (soft delete)
- 금지:
  - `isActive=false` 인 레코드 재수정/재삭제

### 4.2 `Thread`
- 사실상 “존재/삭제” 모델
- 전이:
  - `NOT_EXISTS` -> `EXISTS` (create or get)
  - `EXISTS` -> `NOT_EXISTS` (delete)
- 제약:
  - `(user_id, ai_profile_id, year, month, day)` 유니크

### 4.3 `DailyChatLog`
- 상태 enum은 없지만 `role(USER/AI)`로 수정 권한이 갈린다.
- 전이:
  - `CREATED(USER or AI)` -> `UPDATED(only USER)`
  - `CREATED/UPDATED` -> `DELETED`
- 금지:
  - `AI role` 메시지 수정
  - 본인 thread가 아닌 메시지 수정/삭제

### 4.4 `UserSetting`
- 상태 enum은 없고 설정값 스냅샷 모델
- 전이:
  - `NOT_EXISTS` -> `DEFAULT_CREATED`
  - `DEFAULT/UPDATED` -> `UPDATED`
  - `ANY` -> `DELETED`(회원탈퇴)
- 알림 관련 필드:
  - `notificationEnabled`, `notificationTime`, `lastSentDate`

### 4.5 `UserMemory`
- 상태 enum은 없고 내용 스냅샷 모델
- 전이:
  - `NOT_EXISTS` -> `EMPTY_CREATED`
  - `EMPTY/UPDATED` -> `UPDATED` (LLM 융합)
  - `ANY` -> `RESET_EMPTY`
- 특징:
  - 읽기/업데이트 시 없으면 자동 생성(lazy create)

### 4.6 `ExpoPushToken`
- 상태 필드: `invalid`
- 전이:
  - `NEW` -> `VALID(invalid=false)`
  - `VALID` -> `INVALID(invalid=true)` (전송 중 invalid 판정 시)
  - `INVALID` -> `VALID` (재등록 시 false로 복구)
  - `ANY` -> `DELETED`

## 5. 도메인 불변식(Invariant) 정리
| ID | 불변식 | 현재 강제 위치 | 상태 |
|---|---|---|---|
| INV-001 | 동일 유저/프로필/일자의 `thread`는 1개 | DB Unique + 코드 예외 처리 | 강제됨 |
| INV-002 | 유저당 `user_setting` 1개 | DB Unique + `existsByUserId` 가드 | 부분 강제 (`user_id` nullable) |
| INV-003 | 유저당 `user_memory` 1개 | `JoinColumn(unique=true)` + 조회시 생성 | 부분 강제 (탈퇴 정리 누락 가능) |
| INV-004 | 일자별 active diary 최대 3개 | 코드 카운트 후 생성 | 경합 취약 |
| INV-005 | 삭제된 diary는 수정/재삭제 불가 | 서비스 가드 | 강제됨 |
| INV-006 | AI chat log는 수정 불가 | 서비스 가드 | 강제됨 |
| INV-007 | push token 문자열은 유일 | DB Unique + 중복 등록시 업데이트 | 강제됨 |
| INV-008 | 권한 없는 타인 리소스 수정/삭제 금지 | 서비스 소유권 검증 | 강제됨 |

## 6. 주요 갭 및 우선순위
### P0
1. `Diary` 일자 3개 제한이 애플리케이션 카운트 기반이라 동시 요청 경합 시 초과 저장 가능
2. `ThreadService` SSE 경로에서 quota 선차감 후 실패 시 보상 규칙이 명확하지 않음
3. 회원탈퇴(`UserService.deleteUser`)에서 `user_memory` 정리 경로가 없음

### P1
1. `user_setting.user_id`가 nullable이라 “유저당 1개” 모델이 DB 레벨에서 완전하지 않음
2. `NotificationService`가 `lastSentDate`를 전송 전에 갱신해 실패 시 당일 재시도 기회가 줄어듦
3. 긴 배치/외부 I/O 구간(`NotificationService`, `MemoryUpdateScheduler`)의 트랜잭션 경계 개선 필요

## 7. 후속 이슈 입력값
### STAB-002 (DB 제약)
- `user_setting.user_id` -> `NOT NULL + FK + UNIQUE` 확정
- `diary/thread year/month/day` 범위 `CHECK` 도입 검토
- 탈퇴 경로 기준 FK cascade 정책 재정의

### STAB-003 (멱등)
- 대상 우선순위: `diary create`, `chat send`, `token register/delete`
- 키 스코프: `user_id + endpoint + idempotency_key`

### STAB-004 (상태전이)
- `Diary`, `ExpoPushToken`, 알림 전송 상태(신규 상태테이블 또는 outbox 상태)를 명시 상태 머신으로 고정

### STAB-005 (재시도)
- 재시도 가능 오류: `timeout`, `429`, `5xx`, 일시적 네트워크 장애
- 재시도 불가 오류: 검증 실패, 권한 오류, 비즈니스 룰 위반

### STAB-006 (트랜잭션 경계)
- 원칙: 외부 API 호출은 트랜잭션 밖, DB 반영만 짧은 트랜잭션

## 8. 결론
- STAB-001 범위에서 “핵심 도메인 규칙의 현재 강제 수준”과 “실제 운영 리스크”를 식별했다.
- 다음 단계는 STAB-002(제약 강화)와 STAB-003(멱등)부터 병행 착수하는 것이 효과가 가장 크다.
