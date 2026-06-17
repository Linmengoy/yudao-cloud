import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class ReleaseScopeAndEvidenceContractTest(unittest.TestCase):

    def test_issue_312_release_scope_audit_blocks_wrong_frontend_target_and_processing_scope(self):
        script = read("script/release-scope-audit.ps1")
        runbook = read("script/deployment-runbook.md")

        for required in [
            "AllowedIssues",
            "IncludedIssues",
            "ProcessingIssues",
            "AllowedFrontendTargets",
            "FrontendTarget",
            "BackendServices",
            "processing issue entered release candidate",
            "frontend target '$FrontendTarget' is outside release scope",
            "included issues",
            "excluded processing issues",
            "exclusion rationale",
        ]:
            self.assertIn(required, script)

        self.assertIn("./script/release-scope-audit.ps1", runbook)
        self.assertIn("-AllowedIssues '#146','#173','#174'", runbook)
        self.assertIn("-FrontendTarget admin", runbook)
        self.assertIn("-BackendServices aigc-model,aigc-gen", runbook)
        self.assertIn("不要把 `draw2video-client` 放入同一候选发布", runbook)
        self.assertIn("未发布 `draw2video-client` 的原因", runbook)

    def test_issue_313_windows_wrapper_separates_shell_startup_from_evidence_failures(self):
        wrapper = read("script/docker/verify-release-evidence.ps1")
        runbook = read("script/deployment-runbook.md")

        for required in [
            "[ValidateSet(\"preflight\", \"db-evidence\", \"verify-http\", \"verify-service-health\")]",
            "GIT_BASH_PATH",
            "C:\\Program Files\\Git\\bin\\bash.exe",
            "execution environment failure: bash executable was not found",
            "exit 126",
            "exit 124",
            "execution environment failure: bash process did not return an exit code",
            "release evidence gate failed: verify-release-evidence.sh returned $exitCode",
            "START $(Get-Date -Format o)",
            "END $(Get-Date -Format o) exit=$exitCode",
        ]:
            self.assertIn(required, wrapper)

        self.assertIn("./script/docker/verify-release-evidence.ps1 -Command preflight", runbook)
        self.assertIn("退出码 `126`", runbook)
        self.assertIn("退出码 `124`", runbook)
        self.assertIn("证据不满足", runbook)

    def test_issue_314_frontend_admin_test_env_and_preflight_are_auditable(self):
        deploy = read("script/deploy-frontend-images.ps1")
        compose = read("script/docker/docker-compose.frontend.yml")
        runbook = read("script/deployment-runbook.md")

        for required in [
            "FRONTEND_IMAGE_TAG=$(ConvertTo-EnvValue $ImageTag)",
            "FRONTEND_IMAGE_REGISTRY_PREFIX=$(ConvertTo-EnvValue",
            "DRAW2VIDEO_ADMIN_PORT=$(ConvertTo-EnvValue $AdminPort)",
            "DRAW2VIDEO_CLIENT_PORT=$(ConvertTo-EnvValue $ClientPort)",
            "DRAW2VIDEO_GUIDE_PORT=$(ConvertTo-EnvValue $GuidePort)",
            "ADMIN_GATEWAY_HOST=$(ConvertTo-EnvValue $AdminGatewayHost)",
            "ADMIN_GATEWAY_PORT=$(ConvertTo-EnvValue $AdminGatewayPort)",
        ]:
            self.assertIn(required, deploy)

        self.assertIn("${DRAW2VIDEO_ADMIN_PORT:-8081}:80", compose)
        self.assertIn("${DRAW2VIDEO_CLIENT_PORT:-13000}:3000", compose)
        self.assertIn("${DRAW2VIDEO_GUIDE_PORT:-8082}:80", compose)

        self.assertIn("-Target admin -UseRegistry -SkipBuild -SkipSave -SkipUpload", runbook)
        self.assertIn("docker compose --env-file .frontend-test.env -f docker-compose.frontend.yml ps draw2video-admin", runbook)
        self.assertIn("curl -fsS -I http://127.0.0.1:8081/", runbook)
        self.assertIn("HTTP 状态、响应摘要和失败日志路径", runbook)

    def test_issue_315_prod_backend_rollback_evidence_mentions_model_and_gen(self):
        gate = read("script/docker/verify-release-evidence.sh")
        runbook = read("script/deployment-runbook.md")
        workflow = read(".gitea/workflows/yudao-micro-cicd-prod.yml")

        for service in ["aigc-model", "aigc-gen"]:
            self.assertIn(service, gate)
            self.assertIn(service, runbook)
            self.assertIn(service, workflow)

        for required in [
            "current tag、previous stable tag",
            "`docker pull`/`docker image inspect`",
            "current 或 previous stable 任一值为 `latest`",
            "prod 发布保持阻塞",
            "previous_stable_image_tag is required for rollback evidence",
            "previous_stable_image_tag must be a Git SHA tag",
            "MICRO_IMAGE_TAG must be an immutable Git SHA tag",
        ]:
            self.assertIn(required, runbook + gate)

    def test_issue_316_release_notes_require_included_and_excluded_processing_scope(self):
        script = read("script/release-scope-audit.ps1")
        runbook = read("script/deployment-runbook.md")

        self.assertRegex(script, re.escape("manifest line $lineNumber maps a candidate file to processing work"))
        self.assertIn("file|issue|status", script)
        self.assertIn("completed-dependency", script)
        self.assertIn("included issues:", runbook)
        self.assertIn("excluded processing issues:", runbook)
        self.assertIn("exclusion rationale:", runbook)
        self.assertIn("review:processing changes are excluded until their review labels advance", runbook)


if __name__ == "__main__":
    unittest.main()
