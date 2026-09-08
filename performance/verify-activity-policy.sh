#!/usr/bin/env bash
#
# 활동 기록이 정책서(docs/activity-log-spec.md 2절)대로 동작하는지 배포 환경에서 확인한다.
#
# 확인 항목
#   1. 일정 생성        → 공유 피드에 뜬다
#   2. 일정 시간 변경    → 공유 피드에 뜬다
#   3. 일정 제목만 변경  → 공유 피드에 뜨지 않는다 (내 활동에만)
#   4. 댓글 작성        → 공유 피드에 뜬다
#   5. 댓글 삭제        → 공유 피드에 뜨지 않는다 (내 활동에만)
#   6. 일정 삭제        → 공유 피드에 뜬다
#
# 사용법
#   BASE_URL=https://호스트 PLAN_ID=14 ACCESS_TOKEN=eyJ... ./performance/verify-activity-policy.sh
#
# 주의
#   실제로 일정과 댓글을 만들고 지운다. 반드시 테스트용 계획에서 실행한다.

set -uo pipefail

require() {
  if [[ -z "${!1:-}" ]]; then
    echo "환경 변수 $1이(가) 필요합니다." >&2
    exit 1
  fi
}
require BASE_URL
require PLAN_ID
require ACCESS_TOKEN

BASE_URL="${BASE_URL%/}"
API="${BASE_URL}/api/v1/plans/${PLAN_ID}"
AUTH="Authorization: Bearer ${ACCESS_TOKEN}"
JSON="Content-Type: application/json"

pass=0
fail=0

# 공유 피드 최신 30건의 summary 목록을 가져온다.
shared_feed() {
  curl -sS -H "${AUTH}" "${API}/activities?size=30" | jq -r '.data.activities[].summary'
}

# 내 활동 최신 30건.
my_feed() {
  curl -sS -H "${AUTH}" "${BASE_URL}/api/v1/users/me/activities?size=30" | jq -r '.data.activities[].summary'
}

# $1 설명 / $2 기대(yes|no) / $3 찾을 문구 / $4 피드종류(shared|my)
check() {
  local label=$1 expect=$2 needle=$3 feed=$4 found
  if [[ "${feed}" == "shared" ]]; then
    shared_feed | grep -qF "${needle}" && found=yes || found=no
  else
    my_feed | grep -qF "${needle}" && found=yes || found=no
  fi

  if [[ "${found}" == "${expect}" ]]; then
    echo "  PASS  ${label}"
    pass=$((pass + 1))
  else
    echo "  FAIL  ${label} (기대 ${expect}, 실제 ${found} · 찾은 문구: '${needle}')"
    fail=$((fail + 1))
  fi
}

echo "대상: ${API}"
echo

# ---------------------------------------------------------------------------
echo "[1] 일정 생성"
SCHEDULE_ID=$(curl -sS -X POST "${API}/schedules" -H "${AUTH}" -H "${JSON}" -d '{
  "title": "[VERIFY] 정책 확인용 일정",
  "startAt": "2026-01-01T10:00:00",
  "endAt": "2026-01-01T11:00:00",
  "reservationStatus": "NOT_REQUIRED"
}' | jq -r '.data.scheduleId // empty')

if [[ -z "${SCHEDULE_ID}" ]]; then
  echo "  일정 생성 실패. 토큰과 PLAN_ID를 확인하세요." >&2
  exit 1
fi
echo "      scheduleId=${SCHEDULE_ID}"
check "일정 생성이 공유 피드에 뜬다" yes "일정을 추가했어요." shared

# ---------------------------------------------------------------------------
echo "[2] 일정 시간 변경 (공유 대상)"
curl -sS -X PATCH "${API}/schedules/${SCHEDULE_ID}" -H "${AUTH}" -H "${JSON}" \
  -d '{"startAt": "2026-01-01T14:00:00"}' > /dev/null
check "시간 변경이 공유 피드에 뜬다" yes "일정의 시간이나 장소를 변경했어요." shared

# ---------------------------------------------------------------------------
echo "[3] 일정 제목만 변경 (공유 제외 대상)"
curl -sS -X PATCH "${API}/schedules/${SCHEDULE_ID}" -H "${AUTH}" -H "${JSON}" \
  -d '{"title": "[VERIFY] 제목만 바꾼 일정"}' > /dev/null
check "제목 변경이 공유 피드에 뜨지 않는다" no  "일정 내용을 수정했어요." shared
check "제목 변경이 내 활동에는 뜬다"        yes "일정 내용을 수정했어요." my

# ---------------------------------------------------------------------------
echo "[4] 댓글 작성"
curl -sS -X POST "${API}/schedules/${SCHEDULE_ID}/comments" -H "${AUTH}" -H "${JSON}" \
  -d '{"content": "[VERIFY] 정책 확인용 댓글"}' > /dev/null
COMMENT_ID=$(curl -sS -H "${AUTH}" "${API}/schedules/${SCHEDULE_ID}/comments?size=20" \
  | jq -r '.data.comments[-1].commentId // empty')
check "댓글 작성이 공유 피드에 뜬다" yes "댓글을 남겼어요." shared

# ---------------------------------------------------------------------------
echo "[5] 댓글 삭제 (공유 제외 대상)"
if [[ -n "${COMMENT_ID}" ]]; then
  curl -sS -X DELETE "${API}/schedules/${SCHEDULE_ID}/comments/${COMMENT_ID}" -H "${AUTH}" > /dev/null
  check "댓글 삭제가 공유 피드에 뜨지 않는다" no  "댓글을 삭제했어요." shared
  check "댓글 삭제가 내 활동에는 뜬다"        yes "댓글을 삭제했어요." my
else
  echo "  SKIP  댓글 ID를 찾지 못해 건너뜁니다."
fi

# ---------------------------------------------------------------------------
echo "[6] 일정 삭제"
curl -sS -X DELETE "${API}/schedules/${SCHEDULE_ID}" -H "${AUTH}" > /dev/null
check "일정 삭제가 공유 피드에 뜬다" yes "일정을 삭제했어요." shared

# ---------------------------------------------------------------------------
echo
echo "─────────────────────────────"
echo "  통과 ${pass} · 실패 ${fail}"
echo "─────────────────────────────"
[[ ${fail} -eq 0 ]] || exit 1
