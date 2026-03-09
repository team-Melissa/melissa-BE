# STAB-004 Manual Migration Guide

## Purpose
Introduce explicit diary image generation state at DB level and guard allowed values.

## Scripts
- `STAB-004-precheck.sql`
- `STAB-004-apply.sql`
- `STAB-004-rollback.sql`

## Run Order
1. Run `STAB-004-precheck.sql`
2. Review precheck results
3. Run `STAB-004-apply.sql`
4. Run verification queries (below)
5. If needed, run `STAB-004-rollback.sql`

## Backfill Rule
- `image_url IS NOT NULL` -> `image_status = 'READY'`
- `image_url IS NULL` -> `image_status = 'NONE'`

This is a conservative rule for legacy rows because old schema did not distinguish `PENDING` vs `NONE`.

## Apply Verification Queries
```sql
SELECT COUNT(*) AS image_status_null_count
FROM diary
WHERE image_status IS NULL;

SELECT COUNT(*) AS image_status_invalid_count
FROM diary
WHERE image_status NOT IN ('NONE', 'PENDING', 'READY', 'FAILED');

SELECT image_status, COUNT(*) AS cnt
FROM diary
GROUP BY image_status
ORDER BY image_status;

SELECT table_name, constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_schema = DATABASE()
  AND table_name = 'diary'
  AND constraint_name = 'chk_diary_image_status';

SELECT column_name, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'diary'
  AND column_name = 'image_status';
```

## Caution
- MySQL DDL is auto-commit. If apply fails midway, partial changes may remain.
- Rollback drops `image_status` column and all values in it.

