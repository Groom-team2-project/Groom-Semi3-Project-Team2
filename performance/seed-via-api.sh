#!/usr/bin/env bash
#
# 활동 기록 조회 성능 테스트용 시드 데이터를 API로 만든다.
# DB에 직접 접속할 수 없는 환경(배포 서버 등)에서 seed_activity_logs.sql 대신 사용한다.
#
# 일정 1건을 만들고 그 일정에 댓글을 COUNT개 작성한다.
# 댓글 작성은 COMMENT_CREATED 활동 기록을 남기고, 이 액션은 SHARED_ACTIONS에 포함되므로
# 계획 활동 피드 조회(GET /activities)의 대상이 된다.
#
# 사용법
#   BASE_URL=http://호스트:8080 PLAN_ID=14 ACCESS_TOKEN=eyJ... ./performance/seed-via-api.sh
#
# 선택 환경 변수
#   COUNT       생성할 댓글 수 (기본 300)
#   CONCURRENCY 동시 요청 수 (기본 4). 공용 서버에서는 낮게 유지한다.
#
# 주의
#   반드시 성능 테스트 전용 계획에서 실행한다. 실제로 댓글이 작성되며,
#   정리하려면 해당 계획을 삭제하는 것이 가장 확실하다.

set -euo pipefail

require() {
  local name=$1
  if [[ -z "${!name:-}" ]]; then
    echo "환경 변수 ${name}이(가) 필요합니다." >&2
    exit 1
  fi
}

require BASE_URL
require PLAN_ID
require ACCESS_TOKEN

BASE_URL="${BASE_URL%/}"

# 모든 요청이 Bearer 토큰을 실어 보내므로, 네트워크를 타는 주소는 HTTPS만 허용
if [[ "${BASE_URL}" != https://* \
      && "${BASE_URL}" != http://localhost* \
      && "${BASE_URL}" != http://127.0.0.1* ]]; then
  echo "BASE_URL은 HTTPS URL이어야 합니다 (로컬 http://localhost 제외)." >&2
  exit 1
fi

COUNT="${COUNT:-300}"
CONCURRENCY="${CONCURRENCY:-4}"

AUTH_HEADER="Authorization: Bearer ${ACCESS_TOKEN}"
API="${BASE_URL}/api/v1/plans/${PLAN_ID}"

echo "대상       ${API}"
echo "생성 개수  ${COUNT}건 (동시 ${CONCURRENCY})"
echo

# ---------------------------------------------------------------------------
# 1. 댓글을 달 일정 1건 생성
# ---------------------------------------------------------------------------

echo "[1/3] 일정 생성 중..."

schedule_body=$(cat <<'JSON'
{
  "title": "[BENCHMARK] 성능 테스트용 일정",
  "memo": "활동 기록 조회 성능 테스트를 위해 생성된 일정입니다.",
  "startAt": "2026-01-01T10:00:00",
  "endAt": "2026-01-01T11:00:00",
  "reservationStatus": "NOT_REQUIRED"
}
JSON
)

schedule_response=$(curl -sS -X POST "${API}/schedules" \
  -H "${AUTH_HEADER}" \
  -H "Content-Type: application/json" \
  -d "${schedule_body}")

SCHEDULE_ID=$(echo "${schedule_response}" | jq -r '.data.scheduleId // empty')

if [[ -z "${SCHEDULE_ID}" ]]; then
  echo "일정 생성에 실패했습니다. 응답:" >&2
  echo "${schedule_response}" | jq . >&2 2>/dev/null || echo "${schedule_response}" >&2
  exit 1
fi

echo "      scheduleId=${SCHEDULE_ID}"
echo

# ---------------------------------------------------------------------------
# 2. 댓글 COUNT개 작성 (각각 COMMENT_CREATED 활동 기록을 남긴다)
# ---------------------------------------------------------------------------

echo "[2/3] 댓글 ${COUNT}건 작성 중..."

post_comment() {
  local index=$1
  local status
  status=$(curl -sS -o /dev/null -w '%{http_code}' -X POST \
    "${API}/schedules/${SCHEDULE_ID}/comments" \
    -H "${AUTH_HEADER}" \
    -H "Content-Type: application/json" \
    -d "{\"content\":\"[BENCHMARK] 성능 테스트용 댓글 ${index}\"}")

  if [[ "${status}" != "200" ]]; then
    echo "  ${index}번 실패 (HTTP ${status})" >&2
    return 1
  fi
}
export -f post_comment
export API AUTH_HEADER SCHEDULE_ID

# 일부만 성공하면 요청한 COUNT보다 적은 데이터로 커서를 계산하게 되므로 실패 시 중단
seq 1 "${COUNT}" \
  | xargs -P "${CONCURRENCY}" -I {} bash -c 'post_comment {}' \
  || {
    echo "  일부 요청이 실패했습니다. 시드 데이터가 불완전하므로 중단합니다." >&2
    exit 1
  }

echo "      완료"
echo

# ---------------------------------------------------------------------------
# 3. 깊은 커서(151번째 지점) 값 조회
#    size 최대가 100이므로 100건 + 50건을 이어 읽어 150번째 항목의 커서를 얻는다.
# ---------------------------------------------------------------------------

echo "[3/3] 깊은 커서 값 계산 중..."

page1=$(curl -sS -H "${AUTH_HEADER}" "${API}/activities?size=100")

total_hint=$(echo "${page1}" | jq -r '.data.activities | length')
if [[ "${total_hint}" == "0" || "${total_hint}" == "null" ]]; then
  echo "활동 기록이 조회되지 않았습니다. 응답:" >&2
  echo "${page1}" | jq . >&2 2>/dev/null || echo "${page1}" >&2
  exit 1
fi

c1_at=$(echo "${page1}" | jq -r '.data.nextCursorCreatedAt // empty')
c1_id=$(echo "${page1}" | jq -r '.data.nextCursorLogId // empty')

if [[ -z "${c1_at}" || -z "${c1_id}" ]]; then
  echo "활동 기록이 100건에 못 미쳐 깊은 커서를 만들 수 없습니다." >&2
  echo "COUNT를 늘려 다시 실행하세요." >&2
  exit 1
fi

page2=$(curl -sS -H "${AUTH_HEADER}" \
  "${API}/activities?size=50&cursorCreatedAt=${c1_at}&cursorLogId=${c1_id}")

DEEP_AT=$(echo "${page2}" | jq -r '.data.nextCursorCreatedAt // empty')
DEEP_ID=$(echo "${page2}" | jq -r '.data.nextCursorLogId // empty')

if [[ -z "${DEEP_AT}" || -z "${DEEP_ID}" ]]; then
  echo "활동 기록이 150건에 못 미쳐 깊은 커서를 만들 수 없습니다." >&2
  echo "COUNT를 늘려 다시 실행하세요." >&2
  exit 1
fi

echo "      완료"
echo
echo "─────────────────────────────────────────────"
echo "아래를 그대로 복사해서 실행하세요."
echo "─────────────────────────────────────────────"
echo
echo "export BASE_URL=${BASE_URL}"
echo "export PLAN_ID=${PLAN_ID}"
# 토큰 값은 터미널 기록·CI 로그에 남지 않도록 출력하지 X
echo "# ACCESS_TOKEN은 이 스크립트를 실행한 셸에 이미 설정되어 있습니다."
echo "export DEEP_CURSOR_CREATED_AT=${DEEP_AT}"
echo "export DEEP_CURSOR_LOG_ID=${DEEP_ID}"
echo
echo "PROFILE=journey k6 run performance/k6/activity-log.js"
echo
