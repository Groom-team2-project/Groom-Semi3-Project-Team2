-- 활동 기록 조회 성능 테스트용 시드 데이터
--
-- 실행 전 조건
-- 1. 운영·공용 DB가 아닌 로컬 MySQL에서만 실행한다.
-- 2. 카카오 로그인한 계정이 멤버로 참여한 전용 벤치마크 계획을 먼저 만든다.
-- 3. 아래 plan_id, user_id를 해당 값으로 바꾼다.
-- 4. mysql 클라이언트의 접속 문자셋이 utf8mb4가 아니면 summary의 한글이 깨진 채로 저장됨
--    (화면에 '?????'로 보이는 원인). 아래에서 문자셋을 utf8mb4로 맞춤.
SET NAMES utf8mb4;

SET @benchmark_plan_id := 1;
SET @benchmark_user_id := 1;
-- 여행 계획 1건에서 충분히 큰 활동 기록 규모를 300건으로 가정한다.
SET @benchmark_log_count := 300;
SET @benchmark_now := NOW(6);

-- 같은 벤치마크 계획에서 이 스크립트가 이전에 만든 데이터만 제거한다.
DELETE FROM activity_logs
WHERE plan_id = @benchmark_plan_id
  AND summary LIKE '[BENCHMARK]%';

-- 0~99,999 숫자 후보 중 필요한 개수만 사용해 1초 간격의 최신순 활동 로그를 넣는다.
INSERT INTO activity_logs (
    plan_id,
    user_id,
    action_type,
    target_type,
    target_id,
    summary,
    created_at
)
SELECT
    @benchmark_plan_id,
    @benchmark_user_id,
    'SCHEDULE_CREATED',
    'SCHEDULE',
    sequence_no + 1,
    CONCAT('[BENCHMARK] 일정 ', sequence_no + 1, '개를 추가했어요'),
    TIMESTAMPADD(SECOND, -sequence_no, @benchmark_now)
FROM (
    SELECT ones.n
        + tens.n * 10
        + hundreds.n * 100
        + thousands.n * 1000
        + tenThousands.n * 10000 AS sequence_no
    FROM (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) ones
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) tens
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) hundreds
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) thousands
    CROSS JOIN (
        SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) tenThousands
) sequence_numbers
WHERE sequence_no < @benchmark_log_count;

SELECT
    COUNT(*) AS inserted_log_count,
    MAX(created_at) AS newest_created_at,
    MIN(created_at) AS oldest_created_at
FROM activity_logs
WHERE plan_id = @benchmark_plan_id
  AND summary LIKE '[BENCHMARK]%';

-- K6의 DEEP_CURSOR_CREATED_AT, DEEP_CURSOR_LOG_ID에 넣을 151번째 활동의 커서 값이다.
-- 아래 OFFSET·EXPLAIN 쿼리와 같은 action_type 조건을 써야 같은 행 집합 기준 151번째가 됨
SELECT
    DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s.%f') AS deep_cursor_created_at,
    log_id AS deep_cursor_log_id
FROM activity_logs
WHERE plan_id = @benchmark_plan_id
  AND action_type IN (
      'SCHEDULE_CREATED', 'SCHEDULE_UPDATED', 'SCHEDULE_DELETED',
      'VOTE_CREATED', 'VOTE_UPDATED', 'VOTE_DELETED', 'VOTE_CLOSED',
      'MEMBER_JOINED', 'MEMBER_LEFT', 'MEMBER_ROLE_CHANGED', 'COMMENT_CREATED'
  )
ORDER BY created_at DESC, log_id DESC
LIMIT 150, 1;

-- 첫·중간·후반 페이지에서 OFFSET 방식과 커서 방식이 읽는 행 수를 비교할 때 사용한다.
EXPLAIN ANALYZE
SELECT log_id, created_at
FROM activity_logs
WHERE plan_id = @benchmark_plan_id
  AND action_type IN (
      'SCHEDULE_CREATED', 'SCHEDULE_UPDATED', 'SCHEDULE_DELETED',
      'VOTE_CREATED', 'VOTE_UPDATED', 'VOTE_DELETED', 'VOTE_CLOSED',
      'MEMBER_JOINED', 'MEMBER_LEFT', 'MEMBER_ROLE_CHANGED', 'COMMENT_CREATED'
  )
ORDER BY created_at DESC, log_id DESC
LIMIT 20 OFFSET 0;

EXPLAIN ANALYZE
SELECT log_id, created_at
FROM activity_logs
WHERE plan_id = @benchmark_plan_id
  AND action_type IN (
      'SCHEDULE_CREATED', 'SCHEDULE_UPDATED', 'SCHEDULE_DELETED',
      'VOTE_CREATED', 'VOTE_UPDATED', 'VOTE_DELETED', 'VOTE_CLOSED',
      'MEMBER_JOINED', 'MEMBER_LEFT', 'MEMBER_ROLE_CHANGED', 'COMMENT_CREATED'
  )
ORDER BY created_at DESC, log_id DESC
LIMIT 20 OFFSET 150;

EXPLAIN ANALYZE
SELECT log_id, created_at
FROM activity_logs
WHERE plan_id = @benchmark_plan_id
  AND action_type IN (
      'SCHEDULE_CREATED', 'SCHEDULE_UPDATED', 'SCHEDULE_DELETED',
      'VOTE_CREATED', 'VOTE_UPDATED', 'VOTE_DELETED', 'VOTE_CLOSED',
      'MEMBER_JOINED', 'MEMBER_LEFT', 'MEMBER_ROLE_CHANGED', 'COMMENT_CREATED'
  )
ORDER BY created_at DESC, log_id DESC
LIMIT 20 OFFSET 250;

-- 아래 마지막 EXPLAIN ANALYZE와 같은 action_type 조건으로 뽑아야 같은 행 집합 기준 커서가 됨
SELECT created_at, log_id
INTO @cursor_created_at, @cursor_log_id
FROM activity_logs
WHERE plan_id = @benchmark_plan_id
  AND action_type IN (
      'SCHEDULE_CREATED', 'SCHEDULE_UPDATED', 'SCHEDULE_DELETED',
      'VOTE_CREATED', 'VOTE_UPDATED', 'VOTE_DELETED', 'VOTE_CLOSED',
      'MEMBER_JOINED', 'MEMBER_LEFT', 'MEMBER_ROLE_CHANGED', 'COMMENT_CREATED'
  )
ORDER BY created_at DESC, log_id DESC
LIMIT 150, 1;

EXPLAIN ANALYZE
SELECT log_id, created_at
FROM activity_logs
WHERE plan_id = @benchmark_plan_id
  AND action_type IN (
      'SCHEDULE_CREATED', 'SCHEDULE_UPDATED', 'SCHEDULE_DELETED',
      'VOTE_CREATED', 'VOTE_UPDATED', 'VOTE_DELETED', 'VOTE_CLOSED',
      'MEMBER_JOINED', 'MEMBER_LEFT', 'MEMBER_ROLE_CHANGED', 'COMMENT_CREATED'
  )
  AND (
      created_at < @cursor_created_at
      OR (created_at = @cursor_created_at AND log_id < @cursor_log_id)
  )
ORDER BY created_at DESC, log_id DESC
LIMIT 20;
