import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class Issue322ReleasePreflightDockerPullContractsTest(unittest.TestCase):

    def test_preflight_rejects_non_project_scoped_registry_before_rollback_pull(self):
        gate = read("script/docker/verify-release-evidence.sh")
        runbook = read("script/deployment-runbook.md")

        for required in [
            "require_project_registry_prefix()",
            "registry prefix is required before pulling rollback image",
            "registry prefix must use project-scoped Gitea path root/manman before rollback pull",
            "require_project_registry_prefix \"$item\"",
        ]:
            self.assertIn(required, gate)

        self.assertIn("不要把 `127.0.0.1:3000/root/<service>:<tag>` 当作有效回滚证据", runbook)
        self.assertIn("拒绝非项目级 `root/manman` registry 前缀", runbook)

    def test_preflight_reports_docker_pull_timeout_with_actionable_diagnostics(self):
        gate = read("script/docker/verify-release-evidence.sh")
        runbook = read("script/deployment-runbook.md")

        for required in [
            "pull_previous_image()",
            "run_docker_with_timeout pull \"$previous_ref\"",
            "docker CLI timed out after ${timeout_seconds}s while pulling rollback image",
            "Check Docker daemon health, registry reachability, and project-scoped root/manman image path",
            "docker pull failed for rollback image ${previous_ref} with exit code ${docker_exit}",
        ]:
            self.assertIn(required, gate)

        self.assertIn("DOCKER_CLI_TIMEOUT_SECONDS", runbook)
        self.assertIn("Docker Desktop/docker engine 可响应", runbook)
        self.assertIn("docker pull <registry>/root/manman/<service>:<previous_stable_image_tag>", runbook)


if __name__ == "__main__":
    unittest.main()
