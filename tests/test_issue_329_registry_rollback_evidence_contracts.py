import os
import shutil
import stat
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class Issue329RegistryRollbackEvidenceContractsTest(unittest.TestCase):
    def _bash(self):
        git = shutil.which("git")
        if git is not None:
            candidate = Path(git).resolve().parents[1] / "bin" / "bash.exe"
            if candidate.exists():
                return str(candidate)
        bash = shutil.which("bash")
        if bash is not None and "Windows\\system32\\bash.exe" not in bash and "WindowsApps\\bash.exe" not in bash:
            return bash
        self.skipTest("Git Bash is required for release evidence script execution on Windows")

    def _run_script(self, bash, env):
        return subprocess.run(
            [bash, "script/docker/verify-release-evidence.sh", "preflight"],
            cwd=ROOT,
            env=env,
            text=True,
            encoding="utf-8",
            errors="replace",
            capture_output=True,
        )

    def test_preflight_records_packages_link_and_api_cli_fallbacks(self):
        gate = read("script/docker/verify-release-evidence.sh")
        runbook = read("script/deployment-runbook.md")

        for required in [
            "package_link_for()",
            "GITEA_PACKAGES_BASE_URL",
            "packages_api_command_for()",
            "GITEA_PACKAGES_API_URL",
            'prefix="${prefix%/}/"',
            "packages link: $(package_link_for \"$item\" \"$previous_tag\")",
            "packages api fallback command: $(packages_api_command_for \"$item\" \"$previous_tag\")",
            "cli fallback command: docker image inspect ${previous_ref}",
            "command: docker image inspect ${previous_ref}",
            "pull_previous_image \"$previous_ref\" \"$item\"",
        ]:
            self.assertIn(required, gate)

        self.assertNotIn("if command -v docker >/dev/null 2>&1; then\n      echo \"- current image inspect:\"", gate)

        for required in [
            "Packages link",
            "GITEA_PACKAGES_BASE_URL",
            "GITEA_PACKAGES_API_URL",
            "curl -fsS",
            "docker image inspect <registry>/root/manman/<service>:<previous_stable_image_tag>",
        ]:
            self.assertIn(required, runbook)

    def test_preflight_classifies_pull_failures_and_records_log_path(self):
        gate = read("script/docker/verify-release-evidence.sh")
        runbook = read("script/deployment-runbook.md")

        for required in [
            "classify_docker_pull_failure()",
            "Docker daemon",
            "Registry network",
            "Registry authentication",
            "image path or tag",
            "failure categories checked: Docker daemon, Registry network, Registry authentication, image path or tag",
            "failure logs path: ${stderr_path}",
            "RELEASE_GATE_LOG_DIR",
            "root/${service} image",
            "could not be found in this WSL",
            "failed to run command .docker.",
            "image inspect pending until build completes or docker is available",
            "run_docker_with_timeout pull \"$previous_ref\" >\"$stderr_path\" 2>&1",
        ]:
            self.assertIn(required, gate)

        for required in [
            "Docker daemon",
            "Registry 网络",
            "认证",
            "路径或 tag",
            "日志路径",
            "重新 tag/push 到 `root/manman`",
        ]:
            self.assertIn(required, runbook)

    def test_preflight_rejects_legacy_user_registry_path_before_rollback_pull(self):
        bash = self._bash()

        with tempfile.TemporaryDirectory(dir=ROOT / "tmp") as tmp:
            tmp_path = Path(tmp)
            evidence = tmp_path / "evidence.md"
            evidence_rel = evidence.relative_to(ROOT).as_posix()
            env = {
                **os.environ,
                "DEPLOY_ENV": "prod",
                "BUILD_SERVICE": "aigc-model",
                "MICRO_IMAGE_TAG": "123456789abc",
                "PREVIOUS_STABLE_IMAGE_TAG": "abcdef123456",
                "RELEASE_EVIDENCE_FILE": evidence_rel,
                "MICRO_IMAGE_REGISTRY_PREFIX": "111.228.39.103:3000/root/",
            }

            result = self._run_script(bash, env)

        self.assertNotEqual(0, result.returncode)
        self.assertIn(
            "registry prefix must use project-scoped Gitea path root/manman before rollback pull for aigc-model",
            result.stderr,
        )
        self.assertIn("111.228.39.103:3000/root/", result.stderr)
        self.assertNotIn("docker pull", result.stderr)

    def test_preflight_records_each_pull_failure_category_with_logs(self):
        bash = self._bash()

        cases = {
            "daemon": ("Cannot connect to the Docker daemon", "Docker daemon"),
            "network": ("TLS handshake timeout", "Registry network"),
            "auth": ("unauthorized: authentication required", "Registry authentication"),
            "missing": ("manifest unknown", "image path or tag"),
        }

        with tempfile.TemporaryDirectory(dir=ROOT / "tmp") as tmp:
            tmp_path = Path(tmp)
            fake_docker = tmp_path / "docker"
            fake_docker.write_text(
                """#!/usr/bin/env bash
set -euo pipefail
if [ "${1:-}" = "image" ] && [ "${2:-}" = "inspect" ]; then
  echo "  image=[fake] id=sha256:fake created=2026-06-18T00:00:00Z"
  exit 0
fi
if [ "${1:-}" = "pull" ]; then
  case "${FAKE_DOCKER_PULL_MODE:-}" in
    daemon) echo "Cannot connect to the Docker daemon" >&2 ;;
    network) echo "TLS handshake timeout" >&2 ;;
    auth) echo "unauthorized: authentication required" >&2 ;;
    missing) echo "manifest unknown" >&2 ;;
    *) echo "unexpected fake docker mode" >&2 ;;
  esac
  exit 1
fi
echo "unexpected fake docker invocation: $*" >&2
exit 2
""",
                encoding="utf-8",
            )
            fake_docker.chmod(fake_docker.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)

            for mode, (stderr_marker, category) in cases.items():
                with self.subTest(mode=mode):
                    evidence = tmp_path / f"{mode}-evidence.md"
                    log_dir = tmp_path / f"{mode}-logs"
                    evidence_rel = evidence.relative_to(ROOT).as_posix()
                    log_dir_rel = log_dir.relative_to(ROOT).as_posix()
                    env = {
                        **os.environ,
                        "PATH": f"{tmp_path.as_posix()}{os.pathsep}{os.environ.get('PATH', '')}",
                        "FAKE_DOCKER_PULL_MODE": mode,
                        "DEPLOY_ENV": "prod",
                        "BUILD_SERVICE": "aigc-model",
                        "MICRO_IMAGE_TAG": "123456789abc",
                        "PREVIOUS_STABLE_IMAGE_TAG": "abcdef123456",
                        "RELEASE_EVIDENCE_FILE": evidence_rel,
                        "RELEASE_GATE_LOG_DIR": log_dir_rel,
                        "MICRO_IMAGE_REGISTRY_PREFIX": "111.228.39.103:3000/root/manman/",
                        "GITEA_PACKAGES_BASE_URL": "http://111.228.39.103:3000/root/-/packages/container",
                        "GITEA_PACKAGES_API_URL": "http://111.228.39.103:3000/api/v1/packages/root?type=container",
                    }

                    result = self._run_script(bash, env)

                    output = evidence.read_text(encoding="utf-8")
                    stderr_logs = list(log_dir.glob("docker-pull-*.stderr.log"))

                    self.assertNotEqual(0, result.returncode)
                    self.assertEqual(1, len(stderr_logs))
                    self.assertIn(stderr_marker, stderr_logs[0].read_text(encoding="utf-8"))
                    self.assertIn("packages link: http://111.228.39.103:3000/root/-/packages/container/manman%2Faigc-model/abcdef123456", output)
                    self.assertIn("packages api fallback command: curl -fsS", output)
                    self.assertIn("cli fallback command: docker image inspect 111.228.39.103:3000/root/manman/aigc-model:abcdef123456", output)
                    self.assertIn("command: docker pull 111.228.39.103:3000/root/manman/aigc-model:abcdef123456", output)
                    self.assertIn(f"failure classification: {category}", output)
                    self.assertIn("failure categories checked: Docker daemon, Registry network, Registry authentication, image path or tag", output)
                    self.assertIn(f"failure logs path: {stderr_logs[0].relative_to(ROOT).as_posix()}", output)
                    self.assertIn("see failure logs path", result.stderr)


if __name__ == "__main__":
    unittest.main()
