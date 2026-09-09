#!/usr/bin/env bash
#
# 활동 기록 정책(docs/activity-log-spec.md 2·4절)이 실제 API에서 지켜지는지 검증한다.
#
# 시나리오
#   1. 일정 생성      → 공유 활동 O, 내 활동 O (SCHEDULE_CREATED)
#   2. 댓글 작성      → 공유 활동 O, 내 활동 O (COMMENT_CREATED)
#   3. 댓글 좋아요    → 공유 활동 X, 내 활동 O (COMMENT_LIKED)
#   4. 좋아요 취소    → 기록 없음 (내 활동 COMMENT_LIKED 개수 그대로)
#   5. 댓글 삭제      → 공유 활동 X, 내 활동 O (COMMENT_DELETED)
#   6. 삭제된 댓글의 COMMENT_CREATED 활동 → targetDeleted=true (4절)
#
# 사용법
#   BASE_URL=http://localhost:8080 PLAN_ID=1 ACCESS_TOKEN=eyJ... ./performance/verify-activity-policy.sh
#
# 주의
#   실제로 일정·댓글이 생성된다. 테스트 전용 계획에서 실행한다.

set -euo pipefail

require() {
  if [[ -z "${!1:-}" ]]; then echo "환경 변수 $1 필요" >&2; exit 1; fi
}
require BASE_URL; require PLAN_ID; require ACCESS_TOKEN

BASE_URL="${BASE_URL%/}"
if [[ "${BASE_URL}" != https://* && "${BASE_URL}" != http://localhost* && "${BASE_URL}" != http://127.0.0.1* ]]; then
  echo "BASE_URL은 HTTPS URL이어야 합니다 (로컬 제외)." >&2; exit 1
fi

AUTH="Authorization: Bearer ${ACCESS_TOKEN}"
API="${BASE_URL}/api/v1"
PASS=0; FAIL=0

check() { # check "설명" 실제값 기대값
  if [[ "$2" == "$3" ]]; then echo "  PASS  $1"; PASS=$((PASS+1));
  else echo "  FAIL  $1 (기대: $3, 실제: $2)"; FAIL=$((FAIL+1)); fi
}

shared_count() { # 공유 활동에서 targetId가 일치하는 actionType 개수
  curl -sS -H "${AUTH}" "${API}/plans/${PLAN_ID}/activities?size=100" \
    | jq --arg t "$1" --argjson id "$2" \
      '[.data.activities[] | select(.actionType == $t and .targetId == $id)] | length'
}
my_count() { # 내 활동에서 targetId가 일치하는 actionType 개수
  curl -sS -H "${AUTH}" "${API}/users/me/activities?size=100" \
    | jq --arg t "$1" --argjson id "$2" \
      '[.data.activities[] | select(.actionType == $t and .targetId == $id)] | length'
}

echo "[준비] 검증용 일정·댓글 생성"
SCHEDULE_ID=$(curl -sS -X POST "${API}/plans/${PLAN_ID}/schedules" -H "${AUTH}" \
  -H "Content-Type: application/json" \
  -d '{"title":"[VERIFY] 정책 검증 일정","startAt":"2026-01-02T10:00:00","endAt":"2026-01-02T11:00:00","reservationStatus":"NOT_REQUIRED"}' \
  | jq -r '.data.scheduleId')
[[ -n "${SCHEDULE_ID}" && "${SCHEDULE_ID}" != "null" ]] || { echo "일정 생성 실패" >&2; exit 1; }

COMMENT_ID=$(curl -sS -X POST "${API}/plans/${PLAN_ID}/schedules/${SCHEDULE_ID}/comments" -H "${AUTH}" \
  -H "Content-Type: application/json" -d '{"content":"[VERIFY] 정책 검증 댓글"}' \
  | jq -r '.data.commentId')
[[ -n "${COMMENT_ID}" && "${COMMENT_ID}" != "null" ]] || { echo "댓글 생성 실패" >&2; exit 1; }
echo "  scheduleId=${SCHEDULE_ID}, commentId=${COMMENT_ID}"
echo

echo "[1] 일정 생성 기록"
check "공유 활동에 SCHEDULE_CREATED 1건" "$(shared_count SCHEDULE_CREATED "${SCHEDULE_ID}")" 1
check "내 활동에 SCHEDULE_CREATED 1건"   "$(my_count SCHEDULE_CREATED "${SCHEDULE_ID}")" 1

echo "[2] 댓글 작성 기록"
check "공유 활동에 COMMENT_CREATED 1건" "$(shared_count COMMENT_CREATED "${COMMENT_ID}")" 1
check "내 활동에 COMMENT_CREATED 1건"   "$(my_count COMMENT_CREATED "${COMMENT_ID}")" 1

echo "[3] 댓글 좋아요 기록"
curl -sS -o /dev/null -X POST "${API}/plans/${PLAN_ID}/schedules/${SCHEDULE_ID}/comments/${COMMENT_ID}/likes" -H "${AUTH}"
check "공유 활동에 COMMENT_LIKED 0건 (공유 X)" "$(shared_count COMMENT_LIKED "${COMMENT_ID}")" 0
check "내 활동에 COMMENT_LIKED 1건"            "$(my_count COMMENT_LIKED "${COMMENT_ID}")" 1

echo "[4] 좋아요 취소는 기록 없음"
curl -sS -o /dev/null -X POST "${API}/plans/${PLAN_ID}/schedules/${SCHEDULE_ID}/comments/${COMMENT_ID}/likes" -H "${AUTH}"
check "내 활동 COMMENT_LIKED 여전히 1건" "$(my_count COMMENT_LIKED "${COMMENT_ID}")" 1

echo "[5] 댓글 삭제 기록"
curl -sS -o /dev/null -X DELETE "${API}/plans/${PLAN_ID}/schedules/${SCHEDULE_ID}/comments/${COMMENT_ID}" -H "${AUTH}"
check "공유 활동에 COMMENT_DELETED 0건 (공유 X)" "$(shared_count COMMENT_DELETED "${COMMENT_ID}")" 0
check "내 활동에 COMMENT_DELETED 1건"            "$(my_count COMMENT_DELETED "${COMMENT_ID}")" 1

echo "[6] 삭제된 댓글 활동의 targetDeleted"
DELETED_FLAG=$(curl -sS -H "${AUTH}" "${API}/plans/${PLAN_ID}/activities?size=100" \
  | jq --argjson id "${COMMENT_ID}" \
    '[.data.activities[] | select(.actionType == "COMMENT_CREATED" and .targetId == $id)][0].targetDeleted')
check "COMMENT_CREATED 활동의 targetDeleted=true" "${DELETED_FLAG}" true

echo
echo "결과: PASS ${PASS} / FAIL ${FAIL}"
[[ "${FAIL}" -eq 0 ]]
