import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class Issue318To321ReleaseCandidateFollowupContractTest(unittest.TestCase):

    def test_issue_318_308_scope_excludes_unfinished_related_work(self):
        runbook = read("script/deployment-runbook.md")

        for required in [
            "-AllowedIssues '#308'",
            "-IncludedIssues '#308'",
            "-ProcessingIssues '#306','#176','#170','#150'",
            "-FrontendTarget admin",
            "-BackendServices aigc-workflow,aigc-gen",
            "included issues: #308",
            "excluded processing issues: #306, #176, #170, #150",
        ]:
            self.assertIn(required, runbook)

    def test_issue_319_admin_port_contract_stays_env_file_driven(self):
        compose = read("script/docker/docker-compose.frontend.yml")
        tests = read("tests/test_generation_persistence_contracts.py")
        runbook = read("script/deployment-runbook.md")

        self.assertIn("${DRAW2VIDEO_ADMIN_PORT:-8081}:80", compose)
        self.assertIn('self.assertIn(\'"${DRAW2VIDEO_ADMIN_PORT:-8081}:80"\', compose)', tests)
        self.assertIn("DRAW2VIDEO_ADMIN_PORT=8081", runbook)
        self.assertIn("docker compose --env-file .frontend-test.env -f docker-compose.frontend.yml ps draw2video-admin", runbook)

    def test_issue_320_candidate_evidence_records_clean_sha_and_ignores_cache_noise(self):
        script = read("script/release-candidate-evidence.ps1")
        runbook = read("script/deployment-runbook.md")

        for required in [
            "full_commit_sha",
            "short_commit_sha",
            "Build Input Files",
            "clean_result",
            "git status --short --",
            "Release candidate build input paths are not clean",
        ]:
            self.assertIn(required, script)

        self.assertIn("-CandidateIssues '#308'", runbook)
        self.assertIn("tests/__pycache__", runbook)
        self.assertIn("不能作为构建输入变更", runbook)

    def test_issue_321_windows_preflight_log_records_env_path_and_exit_classification(self):
        wrapper = read("script/docker/verify-release-evidence.ps1")
        runbook = read("script/deployment-runbook.md")

        for required in [
            "log_path=$LogPath",
            '"DEPLOY_ENV"',
            '"BUILD_SERVICE"',
            '"MICRO_IMAGE_TAG"',
            '"PREVIOUS_STABLE_IMAGE_TAG"',
            '"RELEASE_EVIDENCE_FILE"',
            '"MICRO_IMAGE_REGISTRY_PREFIX"',
            '"GIT_BASH_PATH"',
            "env:$name=",
            "exit=126",
            "exit=124",
            "release evidence gate failed: verify-release-evidence.sh returned $exitCode",
        ]:
            self.assertIn(required, wrapper)

        self.assertIn("退出码 `126`", runbook)
        self.assertIn("退出码 `124`", runbook)
        self.assertIn("开始时间、结束时间、退出码和日志路径", runbook)


if __name__ == "__main__":
    unittest.main()
