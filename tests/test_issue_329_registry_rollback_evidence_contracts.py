import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class Issue329RegistryRollbackEvidenceContractsTest(unittest.TestCase):

    def test_preflight_records_packages_link_and_api_cli_fallbacks(self):
        gate = read("script/docker/verify-release-evidence.sh")
        runbook = read("script/deployment-runbook.md")

        for required in [
            "package_link_for()",
            "GITEA_PACKAGES_BASE_URL",
            "packages_api_command_for()",
            "GITEA_PACKAGES_API_URL",
            "packages link: $(package_link_for \"$item\" \"$previous_tag\")",
            "packages api fallback command: $(packages_api_command_for \"$item\" \"$previous_tag\")",
            "cli fallback command: docker image inspect ${previous_ref}",
            "command: docker image inspect ${previous_ref}",
        ]:
            self.assertIn(required, gate)

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


if __name__ == "__main__":
    unittest.main()
