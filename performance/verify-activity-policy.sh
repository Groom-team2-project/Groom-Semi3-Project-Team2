#!/usr/bin/env bash
#
# 활동 기록이 정책서(docs/activity-log-spec.md 2절)대로 동작하는지 배포 환경에서 확인한다.
#
# 확인 항목
#   1. 일정 생성        → 공유 피드에 뜬다
#   2. 일정 시간 변경    → 공유 피드에 뜬다
#   3. 일정 제목만 변경  → 공유 피드에 뜨지 않고 내 활동에만 뜬다
#   4. 댓글 작성        → 공유 피드에 뜬다
#   5. 댓글 삭제        → 공유 피드에 뜨지 않고 내 활동에만 뜬다
#   6. 일정 삭제        → 공유 피드에 뜬다
#
# 검증은 요약 문구가 아니라 (actionType, targetId) 조합으로 한다.
# 요약 문구는 이전 실행이 남긴 기록과 구분되지 않아 두 번째 실행부터 거짓 통과가 나기 때문이다.
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

# 모든 요청이 Bearer 토큰을 실어 보낸다. HTTPS가 아니면 토큰이 평문으로 오간다.
# 배포 서버에 아직 인증서가 없어 차단하지는 않되, 매번 경고를 남긴다.
if [[ "${BASE_URL}" != https://* && "${BASE_URL}" != http://localhost* && "${BASE_URL}" != http://127.0.0.1* ]]; then
  echo "경고: ${BASE_URL} 은(는) HTTPS가 아닙니다. 액세스 토큰이 평문으로 전송됩니다." >&2
  echo "      공용 네트워크에서는 실행하지 말고, 끝난 뒤 로그아웃해 토큰을 무효화하세요." >&2
  echo >&2
fi

API="${BASE_URL}/api/v1/plans/${PLAN_ID}"
AUTH="Authorization: Bearer ${ACCESS_TOKEN}"
JSON="Content-Type: application/json"

pass=0
fail=0

# 쓰기 요청을 보내고 HTTP 상태를 확인한다. 성공하면 응답 본문을 그대로 돌려준다.
# $1 설명 / $2 메서드 / $3 URL / $4 본문(선택)
write_request() {
  local label=$1 method=$2 url=$3 body=${4:-} response status
  if [[ -n "${body}" ]]; then
    response=$(curl -sS -w '\n%{http_code}' -X "${method}" "${url}" -H "${AUTH}" -H "${JSON}" -d "${body}")
  else
    response=$(curl -sS -w '\n%{http_code}' -X "${method}" "${url}" -H "${AUTH}")
  fi
  status=$(tail -n1 <<< "${response}")
  if [[ "${status}" != "200" && "${status}" != "201" ]]; then
    echo "  ${label} 요청 실패 (HTTP ${status})" >&2
    sed '$d' <<< "${response}" >&2
    exit 1
  fi
  sed '$d' <<< "${response}"
}

# 피드에 (actionType, targetId)가 일치하는 활동이 있는지 본다.
# $1 shared|my / $2 actionType / $3 targetId
has_activity() {
  local feed=$1 action=$2 target=$3 url
  if [[ "${feed}" == "shared" ]]; then
    url="${API}/activities?size=100"
  else
    url="${BASE_URL}/api/v1/users/me/activities?size=100"
  fi
  curl -sS -H "${AUTH}" "${url}" \
    | jq -e --arg a "${action}" --argjson t "${target}" \
        '[.data.activities[] | select(.actionType == $a and .targetId == $t)] | length > 0' \
    > /dev/null 2>&1
}

# $1 설명 / $2 기대(yes|no) / $3 피드 / $4 actionType / $5 targetId
check() {
  local label=$1 expect=$2 feed=$3 action=$4 target=$5 found
  has_activity "${feed}" "${action}" "${target}" && found=yes || found=no

  if [[ "${found}" == "${expect}" ]]; then
    echo "  PASS  ${label}"
    pass=$((pass + 1))
  else
    echo "  FAIL  ${label}  (기대 ${expect}, 실제 ${found} · ${action} / targetId=${target})"
    fail=$((fail + 1))
  fi
}

echo "대상: ${API}"
echo

# ---------------------------------------------------------------------------
echo "[1] 일정 생성"
SCHEDULE_ID=$(write_request "일정 생성" POST "${API}/schedules" '{
  "title": "[VERIFY] 정책 확인용 일정",
  "startAt": "2026-01-01T10:00:00",
  "endAt": "2026-01-01T11:00:00",
  "reservationStatus": "NOT_REQUIRED"
}' | jq -r '.data.scheduleId // empty')

if [[ -z "${SCHEDULE_ID}" ]]; then
  echo "  응답에서 scheduleId를 찾지 못했습니다." >&2
  exit 1
fi
echo "      scheduleId=${SCHEDULE_ID}"
check "일정 생성이 공유 피드에 뜬다" yes shared SCHEDULE_CREATED "${SCHEDULE_ID}"

# ---------------------------------------------------------------------------
echo "[2] 일정 시간 변경 (공유 대상)"
# 생성 시 10:00~11:00이므로 종료 시간보다 앞선 값으로 옮긴다.
write_request "시간 변경" PATCH "${API}/schedules/${SCHEDULE_ID}" \
  '{"startAt": "2026-01-01T09:00:00"}' > /dev/null
check "시간 변경이 공유 피드에 뜬다" yes shared SCHEDULE_UPDATED "${SCHEDULE_ID}"

# ---------------------------------------------------------------------------
echo "[3] 일정 제목만 변경 (공유 제외 대상)"
write_request "제목 변경" PATCH "${API}/schedules/${SCHEDULE_ID}" \
  '{"title": "[VERIFY] 제목만 바꾼 일정"}' > /dev/null
check "제목 변경이 공유 피드에 뜨지 않는다" no  shared SCHEDULE_DETAIL_UPDATED "${SCHEDULE_ID}"
check "제목 변경이 내 활동에는 뜬다"        yes my     SCHEDULE_DETAIL_UPDATED "${SCHEDULE_ID}"

# ---------------------------------------------------------------------------
echo "[4] 댓글 작성"
COMMENT_ID=$(write_request "댓글 작성" POST "${API}/schedules/${SCHEDULE_ID}/comments" \
  '{"content": "[VERIFY] 정책 확인용 댓글"}' | jq -r '.data.commentId // empty')

if [[ -z "${COMMENT_ID}" ]]; then
  echo "  응답에서 commentId를 찾지 못했습니다." >&2
  exit 1
fi
echo "      commentId=${COMMENT_ID}"
check "댓글 작성이 공유 피드에 뜬다" yes shared COMMENT_CREATED "${COMMENT_ID}"

# ---------------------------------------------------------------------------
echo "[5] 댓글 삭제 (공유 제외 대상)"
write_request "댓글 삭제" DELETE "${API}/schedules/${SCHEDULE_ID}/comments/${COMMENT_ID}" > /dev/null
check "댓글 삭제가 공유 피드에 뜨지 않는다" no  shared COMMENT_DELETED "${COMMENT_ID}"
check "댓글 삭제가 내 활동에는 뜬다"        yes my     COMMENT_DELETED "${COMMENT_ID}"

# ---------------------------------------------------------------------------
echo "[6] 일정 삭제"
write_request "일정 삭제" DELETE "${API}/schedules/${SCHEDULE_ID}" > /dev/null
check "일정 삭제가 공유 피드에 뜬다" yes shared SCHEDULE_DELETED "${SCHEDULE_ID}"

# ---------------------------------------------------------------------------
echo
echo "─────────────────────────────"
echo "  통과 ${pass} · 실패 ${fail}"
echo "─────────────────────────────"
[[ ${fail} -eq 0 ]] || exit 1
