#!/usr/bin/env bash
set -euo pipefail

SERVER=""
REMOTE_DIR="/opt/code"
PLATFORM="linux/amd64"
CLIENT_API_BASE_URL=""
CLIENT_APP_API_PREFIX="/app-api"
CLIENT_WS_BASE_URL="wss://beta.copse.top"
TARGET="all"
ARCHIVE_NAME=""
COMPOSE_FILE="docker-compose.frontend.yml"
SSH_KEY="$HOME/.ssh/jd_ssh_0304.pem"
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
  --client-api-base-url URL    NEXT_PUBLIC_API_BASE_URL, default empty
  --client-app-api-prefix PATH NEXT_PUBLIC_APP_API_PREFIX, default /app-api
  --client-ws-base-url URL     NEXT_PUBLIC_WS_BASE_URL, default wss://beta.copse.top
  --target all|admin|client    Build/deploy target, default all
  --archive-name NAME          Image archive name
  --compose-file NAME          Compose file name, default docker-compose.frontend.yml
  --ssh-key PATH               SSH private key, default ~/.ssh/jd_ssh_0304.pem
  --skip-build                 Skip docker buildx build
  --skip-save                  Skip docker save
  --skip-upload                Skip scp and remote restart
  --no-proxy                   Run ssh/scp without proxy env or ssh config
  -h, --help                   Show help

Examples:
  script/deploy-frontend-images.sh --server manman
  script/deploy-frontend-images.sh --server manman --target admin
  script/deploy-frontend-images.sh --server manman --target client
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
    --target)
      TARGET="${2:-}"
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
  all|admin|client) ;;
  *)
    echo "--target must be one of: all, admin, client" >&2
    exit 1
    ;;
esac

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADMIN_DIR="$ROOT_DIR/yudao-ui/draw2video-admin"
CLIENT_DIR="$ROOT_DIR/yudao-ui/draw2video-client"
COMPOSE_SOURCE_PATH="$ROOT_DIR/script/docker/$COMPOSE_FILE"

if [[ -z "$ARCHIVE_NAME" ]]; then
  if [[ "$TARGET" == "all" ]]; then
    ARCHIVE_NAME="draw2video-frontend.tar"
  else
    ARCHIVE_NAME="draw2video-${TARGET}.tar"
  fi
fi

ARCHIVE_PATH="$ROOT_DIR/$ARCHIVE_NAME"
IMAGES=()
SERVICES=()

if [[ "$TARGET" == "all" || "$TARGET" == "admin" ]]; then
  IMAGES+=("draw2video-admin:latest")
  SERVICES+=("draw2video-admin")
fi

if [[ "$TARGET" == "all" || "$TARGET" == "client" ]]; then
  IMAGES+=("draw2video-client:latest")
  SERVICES+=("draw2video-client")
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
  if [[ "$NO_PROXY" -eq 1 ]]; then
    run env -u ALL_PROXY -u all_proxy -u HTTPS_PROXY -u https_proxy -u HTTP_PROXY -u http_proxy -u SOCKS_PROXY -u socks_proxy \
      ssh -F /dev/null -i "$SSH_KEY" -o ProxyCommand=none -o ProxyJump=none "$@"
  else
    run ssh -i "$SSH_KEY" "$@"
  fi
}

scp_run() {
  if [[ "$NO_PROXY" -eq 1 ]]; then
    run env -u ALL_PROXY -u all_proxy -u HTTPS_PROXY -u https_proxy -u HTTP_PROXY -u http_proxy -u SOCKS_PROXY -u socks_proxy \
      scp -F /dev/null -i "$SSH_KEY" -o ProxyCommand=none -o ProxyJump=none "$@"
  else
    run scp -i "$SSH_KEY" "$@"
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

step "Check directories"
if [[ "$TARGET" == "all" || "$TARGET" == "admin" ]]; then
  [[ -d "$ADMIN_DIR" ]] || { echo "Admin directory not found: $ADMIN_DIR" >&2; exit 1; }
fi
if [[ "$TARGET" == "all" || "$TARGET" == "client" ]]; then
  [[ -d "$CLIENT_DIR" ]] || { echo "Client directory not found: $CLIENT_DIR" >&2; exit 1; }
fi

if [[ "$SKIP_BUILD" -eq 0 ]]; then
  if [[ "$TARGET" == "all" || "$TARGET" == "admin" ]]; then
    step "Build draw2video-admin image"
    run docker buildx build \
      --platform "$PLATFORM" \
      -t draw2video-admin:latest \
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
      -t draw2video-client:latest \
      --load \
      "$CLIENT_DIR"
  fi
fi

if [[ "$SKIP_SAVE" -eq 0 ]]; then
  step "Save frontend images"
  save_docker_images "$ARCHIVE_PATH" "${IMAGES[@]}"
fi

if [[ "$SKIP_UPLOAD" -eq 0 ]]; then
  step "Upload image archive"
  ssh_run "$SERVER" "mkdir -p '$REMOTE_DIR'"
  if [[ -f "$COMPOSE_SOURCE_PATH" ]]; then
    scp_run "$COMPOSE_SOURCE_PATH" "${SERVER}:${REMOTE_DIR}/${COMPOSE_FILE}"
  else
    echo "Warning: compose file not found locally: $COMPOSE_SOURCE_PATH. Remote compose file will be reused." >&2
  fi
  scp_run "$ARCHIVE_PATH" "${SERVER}:${REMOTE_DIR}/${ARCHIVE_NAME}"

  step "Load images and restart containers"
  remote_command="cd '$REMOTE_DIR'; docker load -i '$ARCHIVE_NAME'; docker compose -f '$COMPOSE_FILE' up -d --no-build --force-recreate ${SERVICES[*]}"
  ssh_run "$SERVER" "$remote_command"
fi

printf '\nDone\n'
