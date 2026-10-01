# Melissa

## 0. 프로젝트 설명

<p align="center">
  <img src="https://github.com/user-attachments/assets/a5e84a2c-6f37-4f04-8b7e-c7e2b39bffda" width="24%" />
  <img src="https://github.com/user-attachments/assets/d752e88d-8b5d-41fb-8113-3b12278d19f9" width="24%" />
  <img src="https://github.com/user-attachments/assets/3b250cd6-64eb-4240-b9f7-6319cd017818" width="24%" />
  <img src="https://github.com/user-attachments/assets/db3310c7-81eb-4b67-800b-c20b8419cd9a" width="24%" />
</p>

Melissa는 사용자가 AI 캐릭터와 대화하듯 하루를 정리하고, 일기 작성을 더 쉽게 지속할 수 있도록 돕는 AI 기반 일기 서비스입니다. 프로젝트는 "일기를 꾸준히 쓰고 싶어도 빈 화면에서 시작하기 어렵고, 기록 습관이 쉽게 끊긴다"는 사용자 문제에서 시작했습니다. 단순 메모 앱이 아니라, 대화형 상호작용과 개인화된 기억, 리마인드를 통해 기록의 진입장벽을 낮추는 것이 목표였습니다.

- AI 대화를 통해 사용자가 부담 없이 하루를 기록할 수 있게 지원
- 개인화 메모리를 바탕으로 이전 대화가 이어지는 경험 제공
- 대화를 그림일기로 만들고, 알림과 streak로 기록 습관 형성 지원

처음에는 채팅과 일기 생성 기능을 구현하는 데 집중했습니다. 서비스를 배포하고 기능이 늘어나면서 같은 요청이 두 번 들어오거나, 외부 API가 늦어지고, 비동기 작업의 순서가 꼬이는 문제를 겪었습니다. 그때부터 기능이 한 번 잘 되는 것보다 **실패하거나 다시 요청됐을 때 어떤 상태가 남는지**를 먼저 생각하며 구조를 개선했습니다.

## 1. 아키텍처

<img width="8192" height="1574" alt="Mermaid Chart - Create complex, visual diagrams with text -2026-04-02-205143" src="https://github.com/user-attachments/assets/27b37f65-ee5b-469c-9e30-d9f35ff4f4de" />

OpenAI, OAuth, S3, Expo Push처럼 응답시간을 직접 통제할 수 없는 외부 시스템이 많습니다. 외부 호출을 기다리는 동안 DB 트랜잭션까지 계속 열어두지 않도록, 필요한 상태를 먼저 저장한 뒤 외부 API를 호출하고 결과가 돌아오면 다시 짧게 반영하는 방식으로 구성했습니다.

결제 기능도 스토어의 구매 기록과 사용자가 실제로 갖는 광고 제거 권한을 분리했습니다. 구매 검증, 권한 부여, 복원과 환불이 각각 다른 시점에 일어나더라도 현재 상태와 처리 이력을 확인할 수 있도록 했습니다.

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

## 3. 주요 문제 해결

### 3-1. 외부 API를 기다리는 동안 DB 커넥션까지 잡고 있던 문제

채팅과 이미지 생성, 소셜 로그인 검증, 푸시 발송은 모두 외부 API를 사용합니다. 처음에는 이 호출들이 DB 작업과 같은 트랜잭션 안에 섞여 있어, 외부 응답이 늦어지면 커넥션도 그만큼 오래 점유했습니다. 외부 서비스의 장애가 내부 저장 과정까지 끌고 갈 수 있는 구조였습니다.

처리 과정을 `prepare -> external call -> finalize`로 나눴습니다. 먼저 검증과 최소한의 상태 저장을 끝내고 트랜잭션을 닫은 뒤 외부 API를 호출하고, 결과가 돌아오면 다시 짧은 트랜잭션에서 최종 상태를 반영했습니다. 경계가 중요한 흐름은 `TransactionTemplate`로 코드에 드러나게 했습니다.

이 기준을 소셜 로그인, 채팅, 일기 생성·수정, 사용자 메모리 갱신과 알림 발송에 공통으로 적용했습니다. [관련 PR #272](https://github.com/team-Melissa/melissa-BE/pull/272)

### 3-2. 같은 채팅 요청이 두 번 처리되는 문제

모바일 네트워크가 불안정하거나 사용자가 연속으로 요청하면 같은 채팅이 다시 들어올 수 있습니다. 채팅은 메시지만 하나 더 저장되는 것으로 끝나지 않고 사용량 차감과 OpenAI 호출까지 다시 일어나기 때문에, 데이터와 비용이 함께 중복되는 문제였습니다.

`Idempotency-Key`로 같은 요청 시도를 구분하고, 사용자·API·키 조합에 unique constraint를 걸었습니다. 같은 키와 같은 내용이 다시 들어오면 처음 저장한 응답을 돌려주고, 같은 키로 다른 내용이 들어오면 충돌로 처리합니다. 처리 중인지, 성공했는지, 다시 시도할 수 있는 실패인지도 상태로 구분했습니다.

동시에 같은 키가 들어왔을 때 Hibernate 세션이 깨지는 문제는 `INSERT IGNORE`와 `SELECT FOR UPDATE`를 사용해 해결했습니다. 키는 24시간 동안 보관하고 만료된 기록은 스케줄러가 정리합니다. [관련 PR #271](https://github.com/team-Melissa/melissa-BE/pull/271)

### 3-3. 이미지 URL만으로는 생성 상태를 알 수 없던 문제

일기 이미지는 비동기로 생성됩니다. 그런데 `imageUrl`이 비어 있다는 사실만으로는 아직 시작하지 않은 것인지, 생성 중인지, 실패한 것인지 알 수 없었습니다. 이전 요청의 완료 이벤트가 늦게 도착해 최신 결과를 덮을 가능성도 있었습니다.

이미지 상태를 `NONE`, `PENDING`, `READY`, `FAILED`로 나누고 허용된 순서로만 바뀌도록 도메인 메서드를 만들었습니다. DB에도 기본값과 check constraint를 추가했으며, 완료 이벤트는 현재 상태가 `PENDING`일 때만 반영했습니다. 기존 데이터는 이미지 URL이 있으면 `READY`, 없으면 `NONE`으로 옮겼습니다. [관련 PR #262](https://github.com/team-Melissa/melissa-BE/pull/262)

### 3-4. 모든 외부 API 실패를 똑같이 재시도하던 문제

네트워크 timeout이나 429·5xx 응답은 잠시 뒤 성공할 수 있지만, 잘못된 요청이나 만료된 푸시 토큰은 반복해도 달라지지 않습니다. 서비스마다 달랐던 기준을 공통 재시도 정책으로 정리했습니다.

일시적인 네트워크 오류와 rate limit, 서버 오류만 제한적으로 다시 시도하고, 일반 4xx와 잘못된 입력은 바로 실패 처리합니다. Expo에서 유효하지 않다고 확인된 토큰은 비활성화하고 다시 보내지 않습니다. S3 업로드가 끝내 실패했는데도 성공한 것처럼 key를 반환하던 경로도 예외를 전달하도록 고쳤습니다. [관련 PR #278](https://github.com/team-Melissa/melissa-BE/pull/278)

### 3-5. 결제 완료와 광고 제거 권한이 어긋날 수 있는 문제

스토어가 구매를 확인했다는 사실과 서비스에서 광고 제거 기능을 사용할 수 있다는 사실은 같아 보이지만, 복원이나 환불이 들어오면 서로 다른 시점에 바뀔 수 있습니다. 그래서 구매 내역은 `Payment`, 실제 사용 권한은 `Entitlement`, 처리 이력은 `PaymentEvent`로 나눴습니다.

Google Play와 Apple App Store의 구매 검증·복원 API를 만들고, 검증이 끝나면 광고 제거 권한을 부여하도록 연결했습니다. 같은 구매가 다시 들어오면 기존 결과로 처리하고, 이미 다른 사용자에게 연결된 구매는 차단합니다. Google acknowledge가 실패하면 다시 시도하며, 관리자가 환불을 반영하면 결제 상태 변경과 권한 회수가 함께 이뤄집니다. 이미 처리된 환불 요청도 같은 결과로 끝나도록 만들었습니다.

[결제 기반 모델 #288](https://github.com/team-Melissa/melissa-BE/pull/288) · [구매 검증과 복원 #290](https://github.com/team-Melissa/melissa-BE/pull/290) · [관리자 환불 #292](https://github.com/team-Melissa/melissa-BE/pull/292)

### 3-6. S3 주소를 DB에 그대로 저장했던 문제

이미지의 전체 URL을 DB에 저장하면 버킷이나 공개 도메인이 바뀔 때 기존 데이터까지 수정해야 합니다. 새로 생성하는 이미지는 S3 object key만 저장하고, 응답을 만들 때 현재 공개 주소와 조합하도록 바꿨습니다.

기존 DB에는 전체 URL과 `s3://` 형식이 이미 섞여 있었기 때문에 한 번에 마이그레이션하지 않았습니다. `S3AssetUrlResolver`가 과거 형식과 새 key를 모두 읽도록 만들어 기존 데이터는 그대로 사용할 수 있게 했습니다. [관련 PR #274](https://github.com/team-Melissa/melissa-BE/pull/274)

## 4. 그 밖의 개선

| 문제 | 개선 내용 |
| --- | --- |
| 저장 직후 이미지가 보이지 않음 | 이미지 생성 이벤트를 `AFTER_COMMIT` 이후 실행하도록 변경 |
| 실제 스케줄러에서만 푸시 실패 | Expo 응답 형태, payload, 성공·실패 상태와 시간대 처리 수정 |
| 약관이 바뀌어도 재동의 여부를 알기 어려움 | 약관을 버전별로 관리하고 사용자 동의 이력 저장 |
| Refresh Token 탈취 대응 | 토큰 hash 저장, type 검증과 기존 토큰 무력화 적용 |
| 인스턴스 교체 후 로그 추적이 어려움 | 애플리케이션·NGINX·EB 로그를 CloudWatch에 수집 |

## 5. 검증

- 같은 멱등 키로 요청을 반복해 실제 채팅 로직이 다시 실행되지 않는지 확인
- 같은 키에 다른 내용을 보내 충돌로 처리되는지 확인
- 이미지 생성이 `PENDING -> READY/FAILED` 순서로 바뀌고 오래된 이벤트가 무시되는지 확인
- 구매 중복 검증, 복원, 권한 부여와 환불 후 권한 회수 테스트
- 재시도할 오류와 바로 실패시킬 오류를 나눠 S3·Expo Push의 최종 상태 확인
- DB 변경에는 적용 전 검사와 rollback 스크립트를 함께 작성
