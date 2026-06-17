#!/usr/bin/env bash
set -euo pipefail

SERVER=""
REMOTE_DIR="/opt/code"
PLATFORM="linux/amd64"
DEPLOY_ENV="auto"
ADMIN_BUILD_MODE=""
CLIENT_API_BASE_URL=""
CLIENT_APP_API_PREFIX="/app-api"
CLIENT_WS_BASE_URL=""
CLIENT_GATEWAY_HOST="host.docker.internal"
CLIENT_GATEWAY_PORT="48080"
CLIENT_TENANT_ID="1"
CLIENT_TERMINAL="20"
ADMIN_GATEWAY_HOST="host.docker.internal"
ADMIN_GATEWAY_PORT="48080"
TARGET="all"
IMAGE_TAG=""
ARCHIVE_NAME=""
COMPOSE_FILE="docker-compose.frontend.yml"
USE_REGISTRY=0
REGISTRY="111.228.39.103:3000/root/manman"
REMOTE_REGISTRY=""
SSH_KEY="${SSH_KEY:-}"
SKIP_BUILD=0
SKIP_SAVE=0
SKIP_UPLOAD=0
NO_PROXY=0

usage() {
  cat <<'EOF'
Usage:
  script/deploy-frontend-images.sh --server manman [options]

Options:
  --server HOST                 SSH target, required
  --remote-dir DIR             Remote deploy dir, default /opt/code
  --platform PLATFORM          Docker build platform, default linux/amd64
  --deploy-env auto|test|prod  Environment, default auto from server
  --admin-build-mode MODE      Vite admin build mode, default deploy env
  --client-api-base-url URL    NEXT_PUBLIC_API_BASE_URL, default empty
  --client-app-api-prefix PATH NEXT_PUBLIC_APP_API_PREFIX, default /app-api
  --client-ws-base-url URL     NEXT_PUBLIC_WS_BASE_URL
  --client-gateway-host HOST   Runtime gateway host for client server proxy
  --client-gateway-port PORT   Runtime gateway port for client server proxy
  --client-tenant-id ID        NEXT_PUBLIC_TENANT_ID, default 1
  --client-terminal ID         NEXT_PUBLIC_TERMINAL, default 20
  --admin-gateway-host HOST    Runtime gateway host for admin nginx proxy
  --admin-gateway-port PORT    Runtime gateway port for admin nginx proxy
  --target all|admin|client|guide
                                Build/deploy target, default all
  --image-tag TAG              Docker image tag, default test-image-version for test and prod git SHA for prod
  --archive-name NAME          Image archive name
  --compose-file NAME          Compose file name, default docker-compose.frontend.yml
  --use-registry               Push images to Gitea registry and deploy by docker pull
  --registry REGISTRY          Registry prefix, default 111.228.39.103:3000/root/manman
  --remote-registry REGISTRY   Registry prefix used by remote docker pull
  --ssh-key PATH               Optional SSH private key, default uses ssh config/agent
  --skip-build                 Skip docker buildx build
  --skip-save                  Skip docker save
  --skip-upload                Skip scp and remote restart
  --no-proxy                   Run ssh/scp without proxy env or ssh config
  -h, --help                   Show help

Examples:
  script/deploy-frontend-images.sh --server manman
  script/deploy-frontend-images.sh --server manman --target admin
  script/deploy-frontend-images.sh --server manman --target client
  script/deploy-frontend-images.sh --server manman --target guide
  script/deploy-frontend-images.sh --server manman2 --deploy-env prod --use-registry
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --server)
      SERVER="${2:-}"
      shift 2
      ;;
    --remote-dir)
      REMOTE_DIR="${2:-}"
      shift 2
      ;;
    --platform)
      PLATFORM="${2:-}"
      shift 2
      ;;
    --deploy-env)
      DEPLOY_ENV="${2:-}"
      shift 2
      ;;
    --admin-build-mode)
      ADMIN_BUILD_MODE="${2:-}"
      shift 2
      ;;
    --client-api-base-url)
      CLIENT_API_BASE_URL="${2:-}"
      shift 2
      ;;
    --client-app-api-prefix)
      CLIENT_APP_API_PREFIX="${2:-}"
      shift 2
      ;;
    --client-ws-base-url)
      CLIENT_WS_BASE_URL="${2:-}"
      shift 2
      ;;
    --client-gateway-host)
      CLIENT_GATEWAY_HOST="${2:-}"
      shift 2
      ;;
    --client-gateway-port)
      CLIENT_GATEWAY_PORT="${2:-}"
      shift 2
      ;;
    --client-tenant-id)
      CLIENT_TENANT_ID="${2:-}"
      shift 2
      ;;
    --client-terminal)
      CLIENT_TERMINAL="${2:-}"
      shift 2
      ;;
    --admin-gateway-host)
      ADMIN_GATEWAY_HOST="${2:-}"
      shift 2
      ;;
    --admin-gateway-port)
      ADMIN_GATEWAY_PORT="${2:-}"
      shift 2
      ;;
    --target)
      TARGET="${2:-}"
      shift 2
      ;;
    --image-tag)
      IMAGE_TAG="${2:-}"
      shift 2
      ;;
    --archive-name)
      ARCHIVE_NAME="${2:-}"
      shift 2
      ;;
    --compose-file)
      COMPOSE_FILE="${2:-}"
      shift 2
      ;;
    --use-registry)
      USE_REGISTRY=1
      shift
      ;;
    --registry)
      REGISTRY="${2:-}"
      shift 2
      ;;
    --remote-registry)
      REMOTE_REGISTRY="${2:-}"
      shift 2
      ;;
    --ssh-key)
      SSH_KEY="${2:-}"
      shift 2
      ;;
    --skip-build)
      SKIP_BUILD=1
      shift
      ;;
    --skip-save)
      SKIP_SAVE=1
      shift
      ;;
    --skip-upload)
      SKIP_UPLOAD=1
      shift
      ;;
    --no-proxy)
      NO_PROXY=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$SERVER" ]]; then
  echo "Missing required --server" >&2
  usage
  exit 1
fi

case "$TARGET" in
  all|admin|client|guide) ;;
  *)
    echo "--target must be one of: all, admin, client, guide" >&2
    exit 1
    ;;
esac

case "$DEPLOY_ENV" in
  auto|test|prod) ;;
  *)
    echo "--deploy-env must be one of: auto, test, prod" >&2
    exit 1
    ;;
esac

is_manman() {
  [[ "$1" == "manman" || "$1" == "root@111.228.39.103" || "$1" == "111.228.39.103" ]]
}

is_manman2() {
  [[ "$1" == "manman2" || "$1" == "root@117.72.215.47" || "$1" == "117.72.215.47" ]]
}

resolve_direct_target() {
  case "$1" in
    manman)
      printf 'root@111.228.39.103\n'
      ;;
    manman2)
      printf 'root@117.72.215.47\n'
      ;;
    111.228.39.103|117.72.215.47)
      printf 'root@%s\n' "$1"
      ;;
    *)
      printf '%s\n' "$1"
      ;;
  esac
}

SSH_TARGET="$SERVER"
if [[ "$NO_PROXY" -eq 1 ]]; then
  SSH_TARGET="$(resolve_direct_target "$SERVER")"
fi

if [[ "$DEPLOY_ENV" == "auto" ]]; then
  if is_manman2 "$SERVER"; then
    DEPLOY_ENV="prod"
  else
    DEPLOY_ENV="test"
  fi
fi

if [[ -z "$ADMIN_BUILD_MODE" ]]; then
  ADMIN_BUILD_MODE="$DEPLOY_ENV"
fi

if [[ -z "$CLIENT_WS_BASE_URL" && "$DEPLOY_ENV" == "prod" ]]; then
  CLIENT_WS_BASE_URL="wss://beta.copse.top"
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADMIN_DIR="$ROOT_DIR/yudao-ui/draw2video-admin"
CLIENT_DIR="$ROOT_DIR/yudao-ui/draw2video-client"
GUIDE_DIR="$ROOT_DIR/yudao-ui/draw2video-guide"
COMPOSE_SOURCE_PATH="$ROOT_DIR/script/docker/$COMPOSE_FILE"
TEST_IMAGE_VERSION_FILE="$ROOT_DIR/script/docker/test-image-version"

read_test_image_version() {
  if [[ ! -f "$TEST_IMAGE_VERSION_FILE" ]]; then
    echo "Test image version file not found: $TEST_IMAGE_VERSION_FILE" >&2
    exit 1
  fi

  local version
  version="$(tr -d '[:space:]' < "$TEST_IMAGE_VERSION_FILE")"
  if [[ ! "$version" =~ ^v[0-9]+\.[0-9]+\.[0-9]+([-.][0-9A-Za-z.-]+)?$ ]]; then
    echo "Invalid test image version in $TEST_IMAGE_VERSION_FILE: $version" >&2
    exit 1
  fi
  printf '%s\n' "$version"
}

if [[ -z "$IMAGE_TAG" ]]; then
  if [[ "$DEPLOY_ENV" == "test" ]]; then
    IMAGE_TAG="$(read_test_image_version)"
  else
    GIT_TAG="$(git -C "$ROOT_DIR" rev-parse --short=12 HEAD 2>/dev/null || true)"
    GIT_TAG="${GIT_TAG:-latest}"
    IMAGE_TAG="${DEPLOY_ENV}-${GIT_TAG}"
  fi
fi

if [[ -z "$ARCHIVE_NAME" ]]; then
  if [[ "$TARGET" == "all" ]]; then
    ARCHIVE_NAME="draw2video-frontend.tar"
  else
    ARCHIVE_NAME="draw2video-${TARGET}.tar"
  fi
fi

if [[ -z "$REMOTE_REGISTRY" ]]; then
  if is_manman "$SERVER"; then
    REMOTE_REGISTRY="127.0.0.1:3000/root/manman"
  else
    REMOTE_REGISTRY="$REGISTRY"
  fi
fi

ARCHIVE_PATH="$ROOT_DIR/$ARCHIVE_NAME"
IMAGES=()
REGISTRY_IMAGES=()
SERVICES=()

if [[ "$TARGET" == "all" || "$TARGET" == "admin" ]]; then
  IMAGES+=("draw2video-admin:$IMAGE_TAG")
  REGISTRY_IMAGES+=("$REGISTRY/draw2video-admin:$IMAGE_TAG")
  SERVICES+=("draw2video-admin")
fi

if [[ "$TARGET" == "all" || "$TARGET" == "client" ]]; then
  IMAGES+=("draw2video-client:$IMAGE_TAG")
  REGISTRY_IMAGES+=("$REGISTRY/draw2video-client:$IMAGE_TAG")
  SERVICES+=("draw2video-client")
fi
if [[ "$TARGET" == "all" || "$TARGET" == "guide" ]]; then
  IMAGES+=("draw2video-guide:$IMAGE_TAG")
  REGISTRY_IMAGES+=("$REGISTRY/draw2video-guide:$IMAGE_TAG")
  SERVICES+=("draw2video-guide")
fi

step() {
  printf '\n==> %s\n' "$1"
}

run() {
  printf '> '
  printf '%q ' "$@"
  printf '\n'
  "$@"
}

ssh_run() {
  local ssh_args=()
  if [[ -n "$SSH_KEY" ]]; then
    ssh_args+=("-i" "$SSH_KEY")
  fi

  if [[ "$NO_PROXY" -eq 1 ]]; then
    run env -u ALL_PROXY -u all_proxy -u HTTPS_PROXY -u https_proxy -u HTTP_PROXY -u http_proxy -u SOCKS_PROXY -u socks_proxy \
      ssh -F /dev/null "${ssh_args[@]}" -o ProxyCommand=none -o ProxyJump=none "$@"
  else
    run ssh "${ssh_args[@]}" "$@"
  fi
}

scp_run() {
  local scp_args=()
  if [[ -n "$SSH_KEY" ]]; then
    scp_args+=("-i" "$SSH_KEY")
  fi

  if [[ "$NO_PROXY" -eq 1 ]]; then
    run env -u ALL_PROXY -u all_proxy -u HTTPS_PROXY -u https_proxy -u HTTP_PROXY -u http_proxy -u SOCKS_PROXY -u socks_proxy \
      scp -F /dev/null "${scp_args[@]}" -o ProxyCommand=none -o ProxyJump=none "$@"
  else
    run scp "${scp_args[@]}" "$@"
  fi
}

remove_with_retry() {
  local path="$1"
  local attempts="${2:-3}"
  local attempt

  for ((attempt = 1; attempt <= attempts; attempt++)); do
    [[ -e "$path" ]] || return 0
    if rm -f -- "$path"; then
      return 0
    fi
    if [[ "$attempt" -eq "$attempts" ]]; then
      return 1
    fi
    sleep "$attempt"
  done
}

save_docker_images() {
  local output_path="$1"
  shift
  local output_dir output_name temp_path attempt

  output_dir="$(dirname "$output_path")"
  output_name="$(basename "$output_path")"

  for attempt in 1 2 3; do
    temp_path="$output_dir/.${output_name}.save-$$-${attempt}.tmp"
    if remove_with_retry "$temp_path" \
      && run docker save -o "$temp_path" "$@" \
      && remove_with_retry "$output_path" \
      && mv -f "$temp_path" "$output_path"; then
      return 0
    fi
    remove_with_retry "$temp_path" || true
    if [[ "$attempt" -eq 3 ]]; then
      echo "Save archive failed after 3 attempts" >&2
      return 1
    fi
    echo "Save archive failed, retrying (${attempt}/3)..." >&2
    sleep $((attempt * 2 > 5 ? 5 : attempt * 2))
  done
}

write_frontend_env_file() {
  local path="$1"
  {
    printf 'FRONTEND_DEPLOY_ENV=%s\n' "$DEPLOY_ENV"
    printf 'ADMIN_GATEWAY_HOST=%s\n' "$ADMIN_GATEWAY_HOST"
    printf 'ADMIN_GATEWAY_PORT=%s\n' "$ADMIN_GATEWAY_PORT"
    printf 'CLIENT_GATEWAY_HOST=%s\n' "$CLIENT_GATEWAY_HOST"
    printf 'CLIENT_GATEWAY_PORT=%s\n' "$CLIENT_GATEWAY_PORT"
    printf 'CLIENT_API_BASE_URL=%s\n' "$CLIENT_API_BASE_URL"
    printf 'CLIENT_APP_API_PREFIX=%s\n' "$CLIENT_APP_API_PREFIX"
    printf 'CLIENT_WS_BASE_URL=%s\n' "$CLIENT_WS_BASE_URL"
    printf 'CLIENT_TENANT_ID=%s\n' "$CLIENT_TENANT_ID"
    printf 'CLIENT_TERMINAL=%s\n' "$CLIENT_TERMINAL"
  } > "$path"
}

step "Check directories"
if [[ "$TARGET" == "all" || "$TARGET" == "admin" ]]; then
  [[ -d "$ADMIN_DIR" ]] || { echo "Admin directory not found: $ADMIN_DIR" >&2; exit 1; }
fi
if [[ "$TARGET" == "all" || "$TARGET" == "client" ]]; then
  [[ -d "$CLIENT_DIR" ]] || { echo "Client directory not found: $CLIENT_DIR" >&2; exit 1; }
fi
if [[ "$TARGET" == "all" || "$TARGET" == "guide" ]]; then
  [[ -d "$GUIDE_DIR" ]] || { echo "Guide directory not found: $GUIDE_DIR" >&2; exit 1; }
fi

if [[ "$SKIP_BUILD" -eq 0 ]]; then
  if [[ "$TARGET" == "all" || "$TARGET" == "admin" ]]; then
    step "Build draw2video-admin image"
    run docker buildx build \
      --platform "$PLATFORM" \
      --build-arg "ADMIN_BUILD_MODE=$ADMIN_BUILD_MODE" \
      -t "draw2video-admin:$IMAGE_TAG" \
      --load \
      "$ADMIN_DIR"
  fi

  if [[ "$TARGET" == "all" || "$TARGET" == "client" ]]; then
    step "Build draw2video-client image"
    run docker buildx build \
      --platform "$PLATFORM" \
      --build-arg "NEXT_PUBLIC_API_BASE_URL=$CLIENT_API_BASE_URL" \
      --build-arg "NEXT_PUBLIC_APP_API_PREFIX=$CLIENT_APP_API_PREFIX" \
      --build-arg "NEXT_PUBLIC_WS_BASE_URL=$CLIENT_WS_BASE_URL" \
      --build-arg "NEXT_PUBLIC_TENANT_ID=$CLIENT_TENANT_ID" \
      --build-arg "NEXT_PUBLIC_TERMINAL=$CLIENT_TERMINAL" \
      -t "draw2video-client:$IMAGE_TAG" \
      --load \
      "$CLIENT_DIR"
  fi

  if [[ "$TARGET" == "all" || "$TARGET" == "guide" ]]; then
    step "Build draw2video-guide image"
    run docker buildx build \
      --platform "$PLATFORM" \
      -t "draw2video-guide:$IMAGE_TAG" \
      --load \
      "$GUIDE_DIR"
  fi
fi

if [[ "$USE_REGISTRY" -eq 1 && "$SKIP_UPLOAD" -eq 0 ]]; then
  step "Push frontend images to registry"
  for i in "${!IMAGES[@]}"; do
    run docker tag "${IMAGES[$i]}" "${REGISTRY_IMAGES[$i]}"
    run docker push "${REGISTRY_IMAGES[$i]}"
  done
elif [[ "$SKIP_SAVE" -eq 0 ]]; then
  step "Save frontend images"
  save_docker_images "$ARCHIVE_PATH" "${IMAGES[@]}"
elif [[ "$USE_REGISTRY" -eq 1 && "$SKIP_UPLOAD" -eq 1 ]]; then
  step "Registry preflight only"
  local_env_file="$(mktemp)"
  write_frontend_env_file "$local_env_file"
  echo "SkipUpload is set; registry push, remote pull, and container restart are skipped."
  echo "target services: ${SERVICES[*]}"
  echo "image tag: $IMAGE_TAG"
  echo "registry images: ${REGISTRY_IMAGES[*]}"
  echo "remote env path: ${REMOTE_DIR}/.frontend-${DEPLOY_ENV}.env"
  echo "remote compose path: ${REMOTE_DIR}/${COMPOSE_FILE}"
  echo "generated env preview:"
  cat "$local_env_file"
  rm -f "$local_env_file"
fi

if [[ "$SKIP_UPLOAD" -eq 0 ]]; then
  step "Prepare remote compose file"
  ssh_run "$SSH_TARGET" "mkdir -p '$REMOTE_DIR'"
  if [[ -f "$COMPOSE_SOURCE_PATH" ]]; then
    scp_run "$COMPOSE_SOURCE_PATH" "${SSH_TARGET}:${REMOTE_DIR}/${COMPOSE_FILE}"
  else
    echo "Warning: compose file not found locally: $COMPOSE_SOURCE_PATH. Remote compose file will be reused." >&2
  fi
  local_env_file="$(mktemp)"
  write_frontend_env_file "$local_env_file"
  scp_run "$local_env_file" "${SSH_TARGET}:${REMOTE_DIR}/.frontend-${DEPLOY_ENV}.env"
  rm -f "$local_env_file"

  if [[ "$USE_REGISTRY" -eq 1 ]]; then
    step "Pull images and restart containers"
    remote_command="cd '$REMOTE_DIR'; FRONTEND_IMAGE_TAG='$IMAGE_TAG' FRONTEND_IMAGE_REGISTRY_PREFIX='$REMOTE_REGISTRY/' docker compose --env-file '.frontend-${DEPLOY_ENV}.env' -f '$COMPOSE_FILE' pull ${SERVICES[*]}; FRONTEND_IMAGE_TAG='$IMAGE_TAG' FRONTEND_IMAGE_REGISTRY_PREFIX='$REMOTE_REGISTRY/' docker compose --env-file '.frontend-${DEPLOY_ENV}.env' -f '$COMPOSE_FILE' up -d --no-build --force-recreate ${SERVICES[*]}"
    ssh_run "$SSH_TARGET" "$remote_command"
  else
    step "Upload image archive"
    scp_run "$ARCHIVE_PATH" "${SSH_TARGET}:${REMOTE_DIR}/${ARCHIVE_NAME}"

    step "Load images and restart containers"
    remote_command="cd '$REMOTE_DIR'; docker load -i '$ARCHIVE_NAME'; FRONTEND_IMAGE_TAG='$IMAGE_TAG' docker compose --env-file '.frontend-${DEPLOY_ENV}.env' -f '$COMPOSE_FILE' up -d --no-build --force-recreate ${SERVICES[*]}"
    ssh_run "$SSH_TARGET" "$remote_command"
  fi
fi

printf '\nDone\n'
