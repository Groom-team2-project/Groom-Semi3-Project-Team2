#!/usr/bin/env bash
#
# 활동 기록 정책(docs/activity-log-spec.md 2·4절)이 실제 API에서 지켜지는지 검증한다.
#
# 시나리오 (기대: 공유 활동 / 내 활동)
#   [1] 일정 생성            → O / O  (SCHEDULE_CREATED)
#   [2] 일정 시간 변경        → O / O  (SCHEDULE_UPDATED)
#   [3] 일정 제목만 변경      → X / O  (SCHEDULE_DETAIL_UPDATED)
#   [4] 투표 생성            → O / O  (VOTE_CREATED)
#   [5] 투표 참여            → 기록 없음 (익명성 보호)
#   [6] 투표 마감            → O / O  (VOTE_CLOSED)
#   [7] 댓글 작성            → O / O  (COMMENT_CREATED)
#   [8] 댓글 좋아요          → X / O  (COMMENT_LIKED)
#   [9] 좋아요 취소          → 기록 없음
#  [10] 댓글 삭제            → X / O  (COMMENT_DELETED)
#  [11] 삭제된 댓글의 작성 활동 → targetDeleted=true (4절)
#  [12] 일정 삭제            → O / O  (SCHEDULE_DELETED)
#  [13] 초대 링크 재발급      → 기록 없음
#
# 사용법
#   BASE_URL=http://localhost:8080 PLAN_ID=1 ACCESS_TOKEN=eyJ... ./performance/verify-activity-policy.sh
#
# 검증은 요약 문구가 아니라 (actionType, targetId) 조합으로 한다. 이전 실행이 남긴 기록과 섞이지 않는다.
# 실제로 일정·투표·댓글이 생성된다. 테스트 전용 계획에서 실행한다.
# MEMBER_JOINED는 다른 계정이 초대 코드로 참여해야 발생해 토큰 하나로는 검증할 수 없어 제외한다.

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
JSON="Content-Type: application/json"
API="${BASE_URL}/api/v1"
PASS=0; FAIL=0

check() { # check "설명" 실제값 기대값
  if [[ "$2" == "$3" ]]; then echo "  PASS  $1"; PASS=$((PASS+1));
  else echo "  FAIL  $1 (기대: $3, 실제: $2)"; FAIL=$((FAIL+1)); fi
}

req() { # req "설명" METHOD URL [BODY] — 실패 시 응답을 출력하고 중단
  local label=$1 method=$2 url=$3 body=${4:-}
  local response status
  if [[ -n "${body}" ]]; then
    response=$(curl -sS -w '\n%{http_code}' -X "${method}" "${url}" -H "${AUTH}" -H "${JSON}" -d "${body}")
  else
    response=$(curl -sS -w '\n%{http_code}' -X "${method}" "${url}" -H "${AUTH}")
  fi
  status=$(tail -n1 <<< "${response}")
  if [[ "${status}" != 2* ]]; then
    echo "  ${label} 요청 실패 (HTTP ${status})" >&2
    sed '$d' <<< "${response}" >&2; exit 1
  fi
  sed '$d' <<< "${response}"
}

shared_count() { # 공유 활동에서 (actionType, targetId) 개수
  curl -sS -H "${AUTH}" "${API}/plans/${PLAN_ID}/activities?size=100" \
    | jq --arg t "$1" --argjson id "$2" \
      '[.data.activities[] | select(.actionType == $t and .targetId == $id)] | length'
}
my_count() { # 내 활동에서 (actionType, targetId) 개수
  curl -sS -H "${AUTH}" "${API}/users/me/activities?size=100" \
    | jq --arg t "$1" --argjson id "$2" \
      '[.data.activities[] | select(.actionType == $t and .targetId == $id)] | length'
}
my_total() { # 내 활동 전체 개수 (내 활동은 내 것만 나오므로 동시 사용자 영향 없음)
  curl -sS -H "${AUTH}" "${API}/users/me/activities?size=100" \
    | jq '.data.activities | length'
}

echo "대상: ${API} (planId=${PLAN_ID})"
echo

echo "[1] 일정 생성 → 공유 O / 내 활동 O"
SCHEDULE_ID=$(req "일정 생성" POST "${API}/plans/${PLAN_ID}/schedules" \
  '{"title":"[VERIFY] 정책 검증 일정","startAt":"2026-01-02T10:00:00","endAt":"2026-01-02T11:00:00","reservationStatus":"NOT_REQUIRED"}' \
  | jq -r '.data.scheduleId')
check "공유 SCHEDULE_CREATED 1건" "$(shared_count SCHEDULE_CREATED "${SCHEDULE_ID}")" 1
check "내 활동 SCHEDULE_CREATED 1건" "$(my_count SCHEDULE_CREATED "${SCHEDULE_ID}")" 1

echo "[2] 일정 시간 변경 → 공유 O"
req "시간 변경" PATCH "${API}/plans/${PLAN_ID}/schedules/${SCHEDULE_ID}" \
  '{"startAt":"2026-01-02T09:00:00"}' > /dev/null
check "공유 SCHEDULE_UPDATED 1건" "$(shared_count SCHEDULE_UPDATED "${SCHEDULE_ID}")" 1

echo "[3] 일정 제목만 변경 → 공유 X / 내 활동 O"
req "제목 변경" PATCH "${API}/plans/${PLAN_ID}/schedules/${SCHEDULE_ID}" \
  '{"title":"[VERIFY] 제목만 바꾼 일정"}' > /dev/null
check "공유 SCHEDULE_DETAIL_UPDATED 0건" "$(shared_count SCHEDULE_DETAIL_UPDATED "${SCHEDULE_ID}")" 0
check "내 활동 SCHEDULE_DETAIL_UPDATED 1건" "$(my_count SCHEDULE_DETAIL_UPDATED "${SCHEDULE_ID}")" 1

echo "[4] 투표 생성 → 공유 O / 내 활동 O"
VOTE_ID=$(req "투표 생성" POST "${API}/plans/${PLAN_ID}/votes" \
  '{"title":"[VERIFY] 정책 검증 투표","deadline":"2027-01-01T00:00:00Z","options":[{"placeName":"후보A"},{"placeName":"후보B"}]}' \
  | jq -r '.data.id')
OPTION_ID=$(curl -sS -H "${AUTH}" "${API}/plans/${PLAN_ID}/votes/${VOTE_ID}" | jq -r '.data.options[0].id')
check "공유 VOTE_CREATED 1건" "$(shared_count VOTE_CREATED "${VOTE_ID}")" 1
check "내 활동 VOTE_CREATED 1건" "$(my_count VOTE_CREATED "${VOTE_ID}")" 1

echo "[5] 투표 참여 → 기록 없음 (익명성)"
BEFORE=$(my_total)
req "투표 참여" POST "${API}/plans/${PLAN_ID}/votes/${VOTE_ID}/participations" \
  "{\"optionId\":${OPTION_ID}}" > /dev/null
check "참여 후 내 활동 개수 변화 없음" "$(my_total)" "${BEFORE}"

echo "[6] 투표 마감 → 공유 O / 내 활동 O"
req "투표 마감" POST "${API}/plans/${PLAN_ID}/votes/${VOTE_ID}/close" > /dev/null
check "공유 VOTE_CLOSED 1건" "$(shared_count VOTE_CLOSED "${VOTE_ID}")" 1
check "내 활동 VOTE_CLOSED 1건" "$(my_count VOTE_CLOSED "${VOTE_ID}")" 1

echo "[7] 댓글 작성 → 공유 O / 내 활동 O"
COMMENT_ID=$(req "댓글 작성" POST "${API}/plans/${PLAN_ID}/schedules/${SCHEDULE_ID}/comments" \
  '{"content":"[VERIFY] 정책 검증 댓글"}' | jq -r '.data.commentId')
check "공유 COMMENT_CREATED 1건" "$(shared_count COMMENT_CREATED "${COMMENT_ID}")" 1
check "내 활동 COMMENT_CREATED 1건" "$(my_count COMMENT_CREATED "${COMMENT_ID}")" 1

echo "[8] 댓글 좋아요 → 공유 X / 내 활동 O"
req "좋아요" POST "${API}/plans/${PLAN_ID}/schedules/${SCHEDULE_ID}/comments/${COMMENT_ID}/likes" > /dev/null
check "공유 COMMENT_LIKED 0건" "$(shared_count COMMENT_LIKED "${COMMENT_ID}")" 0
check "내 활동 COMMENT_LIKED 1건" "$(my_count COMMENT_LIKED "${COMMENT_ID}")" 1

echo "[9] 좋아요 취소 → 기록 없음"
req "좋아요 취소" POST "${API}/plans/${PLAN_ID}/schedules/${SCHEDULE_ID}/comments/${COMMENT_ID}/likes" > /dev/null
check "내 활동 COMMENT_LIKED 여전히 1건" "$(my_count COMMENT_LIKED "${COMMENT_ID}")" 1

echo "[10] 댓글 삭제 → 공유 X / 내 활동 O"
req "댓글 삭제" DELETE "${API}/plans/${PLAN_ID}/schedules/${SCHEDULE_ID}/comments/${COMMENT_ID}" > /dev/null
check "공유 COMMENT_DELETED 0건" "$(shared_count COMMENT_DELETED "${COMMENT_ID}")" 0
check "내 활동 COMMENT_DELETED 1건" "$(my_count COMMENT_DELETED "${COMMENT_ID}")" 1

echo "[11] 삭제된 댓글의 작성 활동 → targetDeleted=true"
DELETED_FLAG=$(curl -sS -H "${AUTH}" "${API}/plans/${PLAN_ID}/activities?size=100" \
  | jq --argjson id "${COMMENT_ID}" \
    '[.data.activities[] | select(.actionType == "COMMENT_CREATED" and .targetId == $id)][0].targetDeleted')
check "targetDeleted=true" "${DELETED_FLAG}" true

echo "[12] 일정 삭제 → 공유 O / 내 활동 O"
req "일정 삭제" DELETE "${API}/plans/${PLAN_ID}/schedules/${SCHEDULE_ID}" > /dev/null
check "공유 SCHEDULE_DELETED 1건" "$(shared_count SCHEDULE_DELETED "${SCHEDULE_ID}")" 1
check "내 활동 SCHEDULE_DELETED 1건" "$(my_count SCHEDULE_DELETED "${SCHEDULE_ID}")" 1

echo "[13] 초대 링크 재발급 → 기록 없음"
BEFORE=$(my_total)
req "초대 재발급" POST "${API}/plans/${PLAN_ID}/invitations/reissue" > /dev/null
check "재발급 후 내 활동 개수 변화 없음" "$(my_total)" "${BEFORE}"

echo
echo "결과: PASS ${PASS} / FAIL ${FAIL}"
[[ "${FAIL}" -eq 0 ]]
