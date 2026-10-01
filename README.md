# Melissa

<br>

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

<br>

## 1. 아키텍처

<img width="8192" height="1574" alt="Mermaid Chart - Create complex, visual diagrams with text -2026-04-02-205143" src="https://github.com/user-attachments/assets/27b37f65-ee5b-469c-9e30-d9f35ff4f4de" />

OpenAI, OAuth, S3, Expo Push처럼 응답시간을 직접 통제할 수 없는 외부 시스템이 많습니다. 외부 호출을 기다리는 동안 DB 트랜잭션까지 계속 열어두지 않도록, 필요한 상태를 먼저 저장한 뒤 외부 API를 호출하고 결과가 돌아오면 다시 짧게 반영하는 방식으로 구성했습니다.

결제 기능도 스토어의 구매 기록과 사용자가 실제로 갖는 광고 제거 권한을 분리했습니다. 구매 검증, 권한 부여, 복원과 환불이 각각 다른 시점에 일어나더라도 현재 상태와 처리 이력을 확인할 수 있도록 했습니다.

<br>

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

<br>

## 3. 주요 문제 해결

### 3-1. 외부 API 호출과 DB 트랜잭션 분리

채팅과 이미지 생성, 소셜 로그인 검증, 푸시 발송은 모두 외부 API를 사용합니다. 처음에는 이 호출들이 DB 작업과 같은 트랜잭션 안에 섞여 있어, 외부 응답이 늦어지면 커넥션도 그만큼 오래 점유했습니다. 외부 서비스의 장애가 내부 저장 과정까지 끌고 갈 수 있는 구조였습니다.

**주요 변경 사항**

- `prepare → external call → finalize` 단계 분리
- 검증과 최소 상태 저장 후 트랜잭션 종료
- 외부 API 응답 수신 후 짧은 트랜잭션으로 최종 상태 반영
- `TransactionTemplate`을 통한 트랜잭션 경계 명시

소셜 로그인, 채팅, 일기 생성·수정, 사용자 메모리 갱신과 알림 발송에 같은 기준을 적용했습니다.

[관련 PR #272](https://github.com/team-Melissa/melissa-BE/pull/272)

<br>

### 3-2. 채팅 요청의 멱등성 보장

모바일 네트워크가 불안정하거나 사용자가 연속으로 요청하면 같은 채팅이 다시 들어올 수 있습니다. 채팅은 메시지만 하나 더 저장되는 것으로 끝나지 않고 사용량 차감과 OpenAI 호출까지 다시 일어나기 때문에, 데이터와 비용이 함께 중복되는 문제였습니다.

**주요 변경 사항**

- `Idempotency-Key`와 사용자·API·키 조합의 unique constraint로 중복 요청 구분
- 동일한 키·본문 재요청 시 기존 응답 반환, 다른 본문 요청 시 충돌 처리
- 처리 중·성공·재시도 가능한 실패 상태 구분
- `INSERT IGNORE`와 `SELECT FOR UPDATE`를 통한 동시 요청 처리
- 키 보관 기간 24시간 적용 및 스케줄러를 통한 만료 기록 정리

동시에 같은 키가 들어왔을 때 Hibernate 세션이 깨지는 문제를 해결하고, 요청 재전송으로 채팅 처리와 사용량 차감이 반복되지 않도록 했습니다.

[관련 PR #271](https://github.com/team-Melissa/melissa-BE/pull/271)

<br>

### 3-3. 비동기 이미지 생성 상태 관리

일기 이미지는 비동기로 생성됩니다. 그런데 `imageUrl`이 비어 있다는 사실만으로는 아직 시작하지 않은 것인지, 생성 중인지, 실패한 것인지 알 수 없었습니다. 이전 요청의 완료 이벤트가 늦게 도착해 최신 결과를 덮을 가능성도 있었습니다.

**주요 변경 사항**

- 이미지 상태를 `NONE`, `PENDING`, `READY`, `FAILED`로 구분
- 도메인 메서드에서 허용된 상태 전이만 처리
- DB 기본값과 check constraint 추가
- 현재 상태가 `PENDING`인 경우에만 완료 이벤트 반영

기존 데이터는 이미지 URL이 있으면 `READY`, 없으면 `NONE`으로 옮겼습니다. URL 유무만으로 구분하던 생성 과정을 명시적인 상태로 관리하도록 바꿨습니다.

[관련 PR #262](https://github.com/team-Melissa/melissa-BE/pull/262)

<br>

### 3-4. 외부 API 오류별 재시도 정책

네트워크 timeout이나 429·5xx 응답은 잠시 뒤 성공할 수 있지만, 잘못된 요청이나 만료된 푸시 토큰은 반복해도 달라지지 않습니다. 서비스마다 달랐던 기준을 공통 재시도 정책으로 정리했습니다.

**주요 변경 사항**

- 일시적인 네트워크 오류·429·5xx에 한해 제한적 재시도
- 일반 4xx와 잘못된 입력의 즉시 실패 처리
- Expo에서 유효하지 않다고 확인된 푸시 토큰 비활성화
- S3 업로드 최종 실패 시 key 반환 대신 예외 전달

재시도로 복구할 수 있는 오류만 다시 처리하고, 최종 실패가 호출부에 전달되도록 정리했습니다.

[관련 PR #278](https://github.com/team-Melissa/melissa-BE/pull/278)

<br>

### 3-5. 구매 내역과 광고 제거 권한 분리

스토어가 구매를 확인했다는 사실과 서비스에서 광고 제거 기능을 사용할 수 있다는 사실은 같아 보이지만, 복원이나 환불이 들어오면 서로 다른 시점에 바뀔 수 있습니다. 그래서 구매 내역은 `Payment`, 실제 사용 권한은 `Entitlement`, 처리 이력은 `PaymentEvent`로 나눴습니다.

**주요 변경 사항**

- Google Play·Apple App Store 구매 검증 및 복원 API 구현
- 구매 검증 완료 후 광고 제거 권한 부여
- 동일 구매 재요청 시 기존 결과 반환 및 다른 사용자에게 연결된 구매 차단
- Google acknowledge 실패 시 재시도
- 관리자 환불 반영 시 결제 상태 변경과 권한 회수의 동시 처리
- 이미 처리된 환불 요청에 대한 동일 결과 반환

구매 검증부터 복원·환불까지 현재 권한과 처리 이력을 함께 확인할 수 있도록 구성했습니다.

[결제 기반 모델 #288](https://github.com/team-Melissa/melissa-BE/pull/288) · [구매 검증과 복원 #290](https://github.com/team-Melissa/melissa-BE/pull/290) · [관리자 환불 #292](https://github.com/team-Melissa/melissa-BE/pull/292)

<br>

### 3-6. S3 파일 경로와 공개 URL 분리

이미지의 전체 URL을 DB에 저장하면 버킷이나 공개 도메인이 바뀔 때 기존 데이터까지 수정해야 합니다. 새로 생성하는 이미지는 S3 object key만 저장하고, 응답을 만들 때 현재 공개 주소와 조합하도록 바꿨습니다.

**주요 변경 사항**

- 신규 이미지의 S3 object key 저장
- API 응답 생성 시 현재 공개 주소와 key 조합
- `S3AssetUrlResolver`에서 전체 URL·`s3://`·object key 형식 지원

기존 DB에 여러 형식이 섞여 있어 일괄 마이그레이션 대신 읽기 호환성을 유지했습니다. 과거 데이터도 그대로 사용하면서 신규 데이터부터 저장 방식을 바꿨습니다.

[관련 PR #274](https://github.com/team-Melissa/melissa-BE/pull/274)

<br>

## 4. 그 밖의 개선

| 문제 | 개선 내용 |
| --- | --- |
| 저장 직후 이미지가 보이지 않음 | 이미지 생성 이벤트를 `AFTER_COMMIT` 이후 실행하도록 변경 |
| 실제 스케줄러에서만 푸시 실패 | Expo 응답 형태, payload, 성공·실패 상태와 시간대 처리 수정 |
| 약관이 바뀌어도 재동의 여부를 알기 어려움 | 약관을 버전별로 관리하고 사용자 동의 이력 저장 |
| Refresh Token 탈취 대응 | 토큰 hash 저장, type 검증과 기존 토큰 무력화 적용 |
| 인스턴스 교체 후 로그 추적이 어려움 | 애플리케이션·NGINX·EB 로그를 CloudWatch에 수집 |

<br>

## 5. 검증

- 같은 멱등 키로 요청을 반복해 실제 채팅 로직이 다시 실행되지 않는지 확인
- 같은 키에 다른 내용을 보내 충돌로 처리되는지 확인
- 이미지 생성이 `PENDING -> READY/FAILED` 순서로 바뀌고 오래된 이벤트가 무시되는지 확인
- 구매 중복 검증, 복원, 권한 부여와 환불 후 권한 회수 테스트
- 재시도할 오류와 바로 실패시킬 오류를 나눠 S3·Expo Push의 최종 상태 확인
- DB 변경에는 적용 전 검사와 rollback 스크립트를 함께 작성
