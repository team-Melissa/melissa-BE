# STAB-002 Manual Migration Guide

## 목적
핵심 도메인에 DB 제약(`NOT NULL`, `UNIQUE`, `FK`, `CHECK`)을 우선 적용하고,
조회 성능 및 운영 안전성을 위한 인덱스를 추가한다.

## 대상 스크립트
- `STAB-002-precheck.sql`
- `STAB-002-apply.sql`
- `STAB-002-rollback.sql`

## 실행 순서
1. `STAB-002-precheck.sql` 실행
2. pre-check 결과가 모두 정상인지 확인
3. `STAB-002-apply.sql` 실행
4. 애플리케이션 주요 시나리오 스모크 테스트
5. 장애 발생 시 `STAB-002-rollback.sql` 실행

## Pre-check 통과 기준
- `user_setting_null_user_id = 0`
- `user_memory_null_user_id = 0`
- `user_setting`/`user_memory` duplicate 결과 없음
- orphan count = 0
- `thread_invalid_month_or_day = 0`
- `diary_invalid_month_or_day = 0`
- `user_setting.user_id` / `user_memory.user_id`의 FK action rule 확인 가능
- `user_id` 컬럼 nullable 여부 확인 가능

## 적용 후 검증 쿼리 예시
```sql
SELECT table_name, constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_schema = DATABASE()
  AND table_name IN ('user_setting', 'user_memory', 'thread', 'diary')
ORDER BY table_name, constraint_type, constraint_name;

SELECT table_name, index_name, non_unique
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name IN ('user_setting', 'diary', 'daily_chat_log')
ORDER BY table_name, index_name;

SELECT
  rc.constraint_name,
  rc.table_name,
  rc.update_rule,
  rc.delete_rule
FROM information_schema.referential_constraints rc
WHERE rc.constraint_schema = DATABASE()
  AND rc.table_name IN ('user_setting', 'user_memory');

SELECT table_name, column_name, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('user_setting', 'user_memory')
  AND column_name = 'user_id';
```

## 주의사항
- MySQL DDL은 대부분 auto-commit이므로, apply 도중 오류가 나면 일부 변경이 남을 수 있다.
- 운영 적용 전에 스테이징에서 동일 데이터 조건으로 리허설을 권장한다.
- 롤백 스크립트의 nullable 복원은 기본 비활성화되어 있으며, 필요 시 주석 해제 후 사용한다.
