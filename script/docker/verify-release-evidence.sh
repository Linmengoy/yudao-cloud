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

is_semver_tag() {
  printf '%s' "$1" | grep -Eq '^v[0-9]+\.[0-9]+\.[0-9]+([-.][0-9A-Za-z.-]+)?$'
}

reject_latest_tag() {
  [ -n "${1:-}" ] || fail "$2 is required"
  [ "$1" != "latest" ] || fail "$2 must not be latest"
}

service_list_for() {
  local service="$1"
  local deploy_env="${DEPLOY_ENV:-prod}"

  if [ "$service" = "all" ]; then
    local backend_services=(
      yudao-gateway \
      yudao-system \
      yudao-infra \
      yudao-member \
      yudao-pay \
      aigc-model \
      aigc-billing \
      aigc-task \
      aigc-asset \
      aigc-safety \
      aigc-gen \
      aigc-workflow \
      aigc-community
    )
    printf '%s\n' "${backend_services[@]}"
    if [ "$deploy_env" = "test" ]; then
      printf '%s\n' \
        draw2video-admin \
        draw2video-client \
        draw2video-guide
    fi
  else
    printf '%s\n' "$service"
  fi
}

registry_prefix_for() {
  local service="$1"

  case "$service" in
    draw2video-*) printf '%s' "${FRONTEND_IMAGE_REGISTRY_PREFIX:-}" ;;
    *) printf '%s' "${MICRO_IMAGE_REGISTRY_PREFIX:-}" ;;
  esac
}

image_ref_for() {
  local service="$1"
  local tag="$2"
  local prefix

  prefix="$(registry_prefix_for "$service")"
  printf '%s%s:%s' "$prefix" "$service" "$tag"
}

run_curl() {
  local url="$1"
  curl -i -sS --fail-with-body --max-time "${CURL_TIMEOUT_SECONDS:-15}" "$url"
}

preflight() {
  local previous_tag="${PREVIOUS_STABLE_IMAGE_TAG:-}"
  local service="${BUILD_SERVICE:-unknown}"
  local evidence="${RELEASE_EVIDENCE_FILE:-}"
  local deploy_env="${DEPLOY_ENV:-prod}"

  [ -n "$evidence" ] || fail "RELEASE_EVIDENCE_FILE is required"
  evidence="$(evidence_path "$evidence")"
  mkdir -p "$(dirname "$evidence")"
  [ -n "${MICRO_IMAGE_TAG:-}" ] || fail "MICRO_IMAGE_TAG is required"
  reject_latest_tag "$MICRO_IMAGE_TAG" "MICRO_IMAGE_TAG"
  case "$deploy_env" in
    test)
      is_semver_tag "$MICRO_IMAGE_TAG" || fail "MICRO_IMAGE_TAG must be a semantic test image tag such as v0.0.1: ${MICRO_IMAGE_TAG}"
      if [ -n "$previous_tag" ]; then
        reject_latest_tag "$previous_tag" "previous_stable_image_tag"
        is_semver_tag "$previous_tag" || fail "previous_stable_image_tag must be a semantic test image tag such as v0.0.1: ${previous_tag}"
      elif [ "$MICRO_IMAGE_TAG" != "v0.0.1" ]; then
        fail "previous_stable_image_tag is required for rollback evidence"
      fi
      ;;
    prod)
      is_sha_tag "$MICRO_IMAGE_TAG" || fail "MICRO_IMAGE_TAG must be an immutable Git SHA tag: ${MICRO_IMAGE_TAG}"
      [ -n "$previous_tag" ] || fail "previous_stable_image_tag is required for rollback evidence"
      reject_latest_tag "$previous_tag" "previous_stable_image_tag"
      is_sha_tag "$previous_tag" || fail "previous_stable_image_tag must be a Git SHA tag: ${previous_tag}"
      ;;
    *)
      fail "DEPLOY_ENV must be test or prod: ${deploy_env}"
      ;;
  esac

  {
    echo
    echo "pre-release gate"
    echo "- service: ${service}"
    echo "- deploy env: ${deploy_env}"
    echo "- current image tag: ${MICRO_IMAGE_TAG}"
    echo "- previous stable image tag: ${previous_tag:-not-provided}"
    echo "- micro image registry prefix: ${MICRO_IMAGE_REGISTRY_PREFIX:-not-set}"
    echo "- frontend image registry prefix: ${FRONTEND_IMAGE_REGISTRY_PREFIX:-not-set}"
    if command -v docker >/dev/null 2>&1; then
      echo "- current image inspect:"
      while IFS= read -r item; do
        echo "  command: docker image inspect ${item}:${MICRO_IMAGE_TAG}"
        if ! docker image inspect "${item}:${MICRO_IMAGE_TAG}" --format='  image={{.RepoTags}} id={{.Id}} created={{.Created}}' 2>/dev/null; then
          echo "  image inspect pending until build completes: ${item}:${MICRO_IMAGE_TAG}"
        fi
      done < <(service_list_for "$service")
      if [ -n "$previous_tag" ]; then
        echo "- previous stable registry pull:"
        while IFS= read -r item; do
          previous_ref="$(image_ref_for "$item" "$previous_tag")"
          echo "  command: docker pull ${previous_ref}"
          docker pull "$previous_ref"
        done < <(service_list_for "$service")
      fi
    fi
    if [ -n "$previous_tag" ]; then
      echo "- rollback command: MICRO_IMAGE_TAG=${previous_tag} FRONTEND_IMAGE_TAG=${previous_tag} MICRO_IMAGE_REGISTRY_PREFIX=${MICRO_IMAGE_REGISTRY_PREFIX:-} FRONTEND_IMAGE_REGISTRY_PREFIX=${FRONTEND_IMAGE_REGISTRY_PREFIX:-} docker compose -f docker-compose-micro.yml up -d --no-build --no-deps --force-recreate ${service}"
    else
      echo "- rollback command: not available for initial test image version"
    fi
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

verify_service_health() {
  local service="${BUILD_SERVICE:-unknown}"
  local compose_file="${COMPOSE_FILE_PATH:-script/docker/docker-compose-micro.yml}"
  local evidence="${RELEASE_EVIDENCE_FILE:-tmp/release-evidence/${service}-${MICRO_IMAGE_TAG:-unknown}.md}"
  local service_health_url="${SERVICE_HEALTH_URL:-}"

  evidence="$(evidence_path "$evidence")"
  mkdir -p "$(dirname "$evidence")"

  {
    echo
    echo "service health verification"
    echo "- verified at: $(date -Is)"
    echo "- service: ${service}"
    echo "- compose file: ${compose_file}"
    echo "- current image tag: ${MICRO_IMAGE_TAG:-not-set}"
    echo "- previous stable image tag: ${PREVIOUS_STABLE_IMAGE_TAG:-not-provided}"
    if command -v docker >/dev/null 2>&1 && [ -f "$compose_file" ]; then
      echo "- compose ps summary:"
      if ! docker compose -f "$compose_file" ps "$service"; then
        echo "  compose ps unavailable for ${service}; continuing to collect image/health evidence"
      fi
      echo "- image inspect:"
      if ! docker image inspect "${service}:${MICRO_IMAGE_TAG}" --format='  image={{.RepoTags}} id={{.Id}} created={{.Created}}' 2>/dev/null; then
        echo "  image inspect unavailable for ${service}:${MICRO_IMAGE_TAG:-not-set}; continuing to health gate"
      fi
    fi
    if [ -n "$service_health_url" ]; then
      echo "- service health command: curl -i -sS --fail-with-body ${service_health_url}"
      echo "- service health result:"
      run_curl "$service_health_url"
      echo
    else
      echo "- service health command: docker inspect health status"
    fi
  } >> "$evidence"

  if command -v docker >/dev/null 2>&1 && [ -f "$compose_file" ]; then
    local health_state
    if ! health_state="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$service" 2>/dev/null)"; then
      health_state=""
    fi
    {
      echo "- container health state: ${health_state:-unknown}"
      if [ "$health_state" != "healthy" ] && [ "$health_state" != "running" ]; then
        echo "- failure logs path: docker compose -f ${compose_file} logs --tail=200 ${service}"
        if ! docker compose -f "$compose_file" logs --tail=200 "$service"; then
          echo "  logs unavailable for ${service}; health gate remains failed"
        fi
      fi
      echo "- rollback decision: fail this gate before promotion when health is not healthy/running"
    } >> "$evidence"
    [ "$health_state" = "healthy" ] || [ "$health_state" = "running" ] || fail "${service} health check failed: ${health_state:-unknown}"
  fi
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
  verify-service-health)
    verify_service_health
    ;;
  *)
    fail "usage: $0 {preflight|db-evidence|verify-http|verify-service-health}"
    ;;
esac
