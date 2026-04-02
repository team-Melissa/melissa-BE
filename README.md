# Melissa

## 0. 프로젝트 설명
<img width="1834" height="1291" alt="image" src="https://github.com/user-attachments/assets/fae38087-6399-4d93-9c3f-6e34b510bcf1" />

Melissa는 사용자가 AI 캐릭터와 대화하듯 하루를 정리하고, 일기 작성을 더 쉽게 지속할 수 있도록 돕는 AI 기반 일기 서비스의 백엔드 프로젝트입니다. 프로젝트는 "일기를 꾸준히 쓰고 싶어도 빈 화면에서 시작하기 어렵고, 기록 습관이 쉽게 끊긴다"는 사용자 문제에서 시작했습니다. 단순 메모 앱이 아니라, 대화형 상호작용과 개인화된 기억, 리마인드를 통해 기록의 진입장벽을 낮추는 것이 목표였습니다.

- AI 대화를 통해 사용자가 부담 없이 하루를 기록할 수 있게 지원
- 개인화 메모리를 바탕으로 더 이어지는 대화 경험 제공
- 이미지 생성과 알림 기능으로 기록 경험의 몰입도와 지속성 강화

### 해결하려는 사용자 문제

- 일기를 쓰고 싶어도 무엇부터 써야 할지 몰라 기록을 시작하기 어려운 문제
- 기록이 단발성으로 끝나고 꾸준한 습관으로 이어지지 않는 문제
- 기존 일기 앱이 사용자와 상호작용하지 않아 감정 정리나 회고가 어렵다는 문제

## 1. 아키텍처
<img width="8192" height="1574" alt="Mermaid Chart - Create complex, visual diagrams with text -2026-04-02-205143" src="https://github.com/user-attachments/assets/27b37f65-ee5b-469c-9e30-d9f35ff4f4de" />



## 2. 사용 기술

| 구분 | 기술 |
| --- | --- |
| Backend | Java 17, Spring Boot 3.3.1, Spring Data JPA, Spring Security |
| Database | MySQL, HikariCP |
| AI / Realtime | Spring AI, OpenAI, DALL-E, SSE, WebFlux |
| Auth / Security | JWT, AES-256-CBC |
| Infra | AWS Elastic Beanstalk, EC2, RDS(MySQL), S3, Route 53, CloudWatch |
| Operation | Grafana, Swagger/OpenAPI, Scheduler, Async Executor |

## 3. 문제 해결 내용

### 3-1. 외부 API가 많은 서비스에서 DB 트랜잭션 경계가 쉽게 무너지는 문제

Melissa는 채팅 OpenAI 호출, 이미지 생성, 소셜 로그인 검증, 푸시 발송처럼 외부 API 비중이 높은 서비스였습니다. 이런 호출이 트랜잭션 안에 섞이면 DB 커넥션이 오래 점유되고, 외부 장애가 내부 저장 흐름까지 함께 흔들릴 수 있습니다. 이를 줄이기 위해 처리 흐름을 `prepare -> external call -> finalize` 단계로 재구성하고, `TransactionTemplate`을 사용해 외부 I/O와 DB 반영의 경계를 명시적으로 분리했습니다.

### 3-2. OpenAI 채팅과 SSE 스트리밍 경로에서 블로킹 작업이 서버 자원을 묶는 문제

실시간 채팅 경로에서는 quota 차감, 채팅 저장, OpenAI 스트리밍 응답이 함께 엮이기 쉽습니다. 이를 완화하기 위해 블로킹 작업은 `boundedElastic`으로 분리하고, SSE 응답 중 DB 저장은 별도 경로로 처리해 스트리밍 동안 커넥션과 스레드가 불필요하게 묶이지 않도록 조정했습니다.

### 3-3. 중복 요청이 OpenAI 비용과 데이터 정합성을 동시에 깨뜨리는 문제

모바일 환경에서는 동일 요청이 재전송되기 쉽고, 채팅 API에서는 이 문제가 곧 비용과 정합성 문제로 이어질 수 있었습니다. 그래서 `Idempotency-Key` 기반 선택적 멱등성을 도입하고, `user_id + endpoint + idempotency_key`를 unique하게 관리했으며, 요청 본문 hash 비교와 응답 재사용 구조를 넣어 중복 저장과 중복 과금을 줄였습니다.

### 3-4. 푸시 알림이 수동 테스트와 실제 스케줄러 경로에서 다르게 실패하는 문제

Expo Push는 수동 호출에서는 성공처럼 보이지만 실제 운영 스케줄러 경로에서는 파싱, payload, 상태 처리, 타임존 문제 때문에 실패가 반복될 수 있었습니다. 이를 해결하기 위해 Expo 응답 `data`가 객체와 배열 두 형태로 오는 경우를 모두 처리하도록 DTO를 보완하고, 성공한 경우에만 완료 처리되도록 상태 모델을 바꿨으며, `Asia/Seoul` 기준으로 날짜 계산을 고정했습니다.

### 3-5. OpenAI 연동 문제를 서비스 코드가 아니라 라이브러리 회귀까지 포함해 추적한 경험

Spring AI 안정화 버전 업그레이드 이후 OpenAI 요청이 `extra_body` 때문에 400으로 실패하는 문제가 발생했습니다. 처음에는 애플리케이션 설정 문제처럼 보였지만, 동일한 코드에서 버전만 바꿔 비교한 끝에 라이브러리 회귀 가능성을 특정했고, `spring-ai-bom 1.1.0`으로 내려 서비스 동작을 복구했습니다.

### 3-6. S3 절대 URL 저장 구조가 인프라 변경 비용을 키우는 문제

이미지 자산을 DB에 절대 URL로 저장하면 버킷 구조나 계정이 바뀔 때 마이그레이션 비용이 커집니다. 이를 해결하기 위해 object key 중심 저장 구조로 전환하고, `S3AssetUrlResolver`를 두어 기존 absolute URL, `s3://` 형태, key 저장값을 모두 읽을 수 있게 했습니다. 읽기 하위호환은 유지하되 쓰기는 새 방식으로 유도하는 점진 마이그레이션 구조였습니다.

### 3-7. 검증 가능한 결과

- HikariCP `maximum-pool-size: 20`
- 멱등성 TTL `24시간`
- 알림 배치 크기 `100`
- notification executor `core 5 / max 10 / queue 100`
- 일반 async thread pool `4`
- JWT Access Token `2시간`, Refresh Token `15일`
- multipart 최대 크기 `20MB`
