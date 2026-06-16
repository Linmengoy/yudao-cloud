#!/usr/bin/env bash
set -euo pipefail

command="${1:-}"

fail() {
  echo "release gate failed: $*" >&2
  exit 1
}

evidence_path() {
  local path="$1"
  if [ -n "${GITHUB_WORKSPACE:-}" ] && [ "${path#/}" = "$path" ]; then
    printf '%s/%s' "$GITHUB_WORKSPACE" "$path"
  else
    printf '%s' "$path"
  fi
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

is_sha_tag() {
  printf '%s' "$1" | grep -Eq '^[0-9a-fA-F]{7,40}$'
}

run_curl() {
  local url="$1"
  curl -i -sS --fail-with-body --max-time "${CURL_TIMEOUT_SECONDS:-15}" "$url"
}

preflight() {
  local previous_tag="${PREVIOUS_STABLE_IMAGE_TAG:-}"
  local service="${BUILD_SERVICE:-unknown}"
  local evidence="${RELEASE_EVIDENCE_FILE:-}"

  [ -n "$evidence" ] || fail "RELEASE_EVIDENCE_FILE is required"
  evidence="$(evidence_path "$evidence")"
  mkdir -p "$(dirname "$evidence")"
  [ -n "${MICRO_IMAGE_TAG:-}" ] || fail "MICRO_IMAGE_TAG is required"
  is_sha_tag "$MICRO_IMAGE_TAG" || fail "MICRO_IMAGE_TAG must be an immutable Git SHA tag: ${MICRO_IMAGE_TAG}"
  [ -n "$previous_tag" ] || fail "previous_stable_image_tag is required for rollback evidence"
  is_sha_tag "$previous_tag" || fail "previous_stable_image_tag must be a Git SHA tag: ${previous_tag}"

  {
    echo
    echo "pre-release gate"
    echo "- service: ${service}"
    echo "- current image tag: ${MICRO_IMAGE_TAG}"
    echo "- previous stable image tag: ${previous_tag}"
    echo "- rollback command: MICRO_IMAGE_TAG=${previous_tag} FRONTEND_IMAGE_TAG=${previous_tag} docker compose -f docker-compose-micro.yml up -d --no-build --no-deps --force-recreate ${service}"
  } >> "$evidence"
}

db_evidence() {
  local record="${COMMUNITY_DB_RELEASE_RECORD:-}"

  if [ -z "$record" ]; then
    fail "COMMUNITY_DB_RELEASE_RECORD must point to the completed community_db migration record"
  fi
  [ -f "$record" ] || fail "community_db migration record not found: $record"

  local required=(
    "backup file:"
    "backup sha256:"
    "sql commit sha:"
    "executor:"
    "verifier:"
    "execution window:"
    "verification SQL output summary:"
    "rollback drill:"
    "service health result:"
  )
  local item
  for item in "${required[@]}"; do
    grep -F "$item" "$record" >/dev/null || fail "community_db record missing field: $item"
    grep -E "^[- ]*${item}[[:space:]]*$" "$record" >/dev/null && fail "community_db record has empty field: $item"
  done

  local table
  for table in \
    aigc_community_post \
    aigc_community_post_like \
    aigc_community_comment \
    aigc_community_share_log \
    aigc_community_follow \
    aigc_community_author_stats \
    aigc_community_audit_log \
    aigc_guide_content; do
    grep -F "$table" "$record" >/dev/null || fail "community_db record missing verified table: $table"
  done

  grep -F "utf8mb4" "$record" >/dev/null || fail "community_db record must confirm utf8mb4 collation"
}

verify_http() {
  local service="${BUILD_SERVICE:-unknown}"
  local compose_file="${COMPOSE_FILE_PATH:-script/docker/docker-compose-micro.yml}"
  local evidence="${RELEASE_EVIDENCE_FILE:-tmp/release-evidence/${service}-${MICRO_IMAGE_TAG:-unknown}.md}"
  local gateway_base="${GATEWAY_BASE_URL:-http://127.0.0.1:48080}"
  local service_health_url="${COMMUNITY_HEALTH_URL:-http://127.0.0.1:48097/actuator/health}"
  local admin_url="${COMMUNITY_GATEWAY_ADMIN_SMOKE_URL:-${gateway_base}/admin-api/aigc/community/admin/post/page?pageNo=1&pageSize=1}"
  local app_url="${COMMUNITY_GATEWAY_APP_SMOKE_URL:-${gateway_base}/app-api/aigc/community/post/page?pageNo=1&pageSize=1}"

  evidence="$(evidence_path "$evidence")"
  mkdir -p "$(dirname "$evidence")"
  require_command curl

  {
    echo
    echo "strict deployment verification"
    echo "- verified at: $(date -Is)"
    echo "- compose file: ${compose_file}"
    echo "- service health command: curl -i -sS --fail-with-body ${service_health_url}"
  } >> "$evidence"

  if command -v docker >/dev/null 2>&1 && [ -f "$compose_file" ]; then
    {
      echo "- compose ps summary:"
      docker compose -f "$compose_file" ps
    } >> "$evidence"
  fi

  {
    echo "- service health result:"
    run_curl "$service_health_url"
    echo
    echo "- gateway admin smoke command: curl -i -sS --fail-with-body ${admin_url}"
    echo "- gateway admin smoke result:"
    run_curl "$admin_url"
    echo
    echo "- gateway app smoke command: curl -i -sS --fail-with-body ${app_url}"
    echo "- gateway app smoke result:"
    run_curl "$app_url"
    echo
  } >> "$evidence"
}

case "$command" in
  preflight)
    preflight
    ;;
  db-evidence)
    db_evidence
    ;;
  verify-http)
    verify_http
    ;;
  *)
    fail "usage: $0 {preflight|db-evidence|verify-http}"
    ;;
esac
