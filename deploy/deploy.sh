#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 2 ]]; then
  echo "사용법: $0 <backend-image-uri> <aws-region>" >&2
  exit 2
fi

IMAGE_URI="$1"
DEPLOY_REGION="$2"
DEPLOY_DIR="/opt/moigo"
COMPOSE_FILE="${DEPLOY_DIR}/compose.yml"
ENV_FILE="${DEPLOY_DIR}/.env"
BACKEND_CONTAINER="moigo-backend"
DEPLOYMENT_STARTED=0

stop_failed_backend() {
  if [[ "${DEPLOYMENT_STARTED}" -eq 1 ]] && docker inspect "${BACKEND_CONTAINER}" >/dev/null 2>&1; then
    docker update --restart=no "${BACKEND_CONTAINER}" >/dev/null 2>&1 || true
    docker stop "${BACKEND_CONTAINER}" >/dev/null 2>&1 || true
  fi
}

on_error() {
  local exit_code=$?
  echo "배포 실패: 새 백엔드 컨테이너를 중지합니다." >&2
  stop_failed_backend
  docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" logs --tail=200 backend || true
  exit "${exit_code}"
}

trap on_error ERR
cd "${DEPLOY_DIR}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "배포 실패: ${ENV_FILE} 파일이 없습니다." >&2
  exit 1
fi

export BACKEND_IMAGE="${IMAGE_URI}"
export AWS_REGION="${DEPLOY_REGION}"

echo "[1/6] 배포 설정 검증"
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" config --quiet

echo "[2/6] ECR 로그인"
ECR_REGISTRY="${IMAGE_URI%%/*}"
aws ecr get-login-password --region "${DEPLOY_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

echo "[3/6] 백엔드 이미지 다운로드"
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" pull backend

echo "[4/6] 백엔드와 MySQL 실행"
DEPLOYMENT_STARTED=1
docker compose --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d mysql backend

echo "[5/6] 백엔드 헬스체크 대기"
for attempt in $(seq 1 30); do
  HEALTH_STATUS="$(
    docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
      "${BACKEND_CONTAINER}" 2>/dev/null || true
  )"

  echo "헬스체크 ${attempt}/30: ${HEALTH_STATUS:-unknown}"

  if [[ "${HEALTH_STATUS}" == "healthy" ]]; then
    echo "[6/6] 정상 배포 확정"
    docker update --restart=unless-stopped "${BACKEND_CONTAINER}" >/dev/null
    DEPLOYMENT_STARTED=0
    trap - ERR
    echo "배포 성공"
    exit 0
  fi

  if [[ "${HEALTH_STATUS}" == "exited" || "${HEALTH_STATUS}" == "dead" ]]; then
    echo "백엔드 프로세스가 종료됐습니다." >&2
    false
  fi

  sleep 10
done

echo "백엔드가 제한 시간 안에 정상 상태가 되지 않았습니다." >&2
false
