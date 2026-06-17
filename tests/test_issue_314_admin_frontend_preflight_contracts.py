import shutil
import subprocess
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class Issue314AdminFrontendPreflightContractsTest(unittest.TestCase):

    def test_admin_registry_preflight_outputs_env_preview_without_side_effects(self):
        powershell = shutil.which("powershell") or shutil.which("pwsh")
        if powershell is None:
            self.skipTest("PowerShell is required for frontend preflight execution")

        result = subprocess.run(
            [
                powershell,
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                str(ROOT / "script/deploy-frontend-images.ps1"),
                "-Server",
                "manman",
                "-DeployEnv",
                "test",
                "-Target",
                "admin",
                "-UseRegistry",
                "-SkipBuild",
                "-SkipSave",
                "-SkipUpload",
            ],
            cwd=ROOT,
            text=True,
            capture_output=True,
        )

        output = result.stdout + result.stderr
        self.assertEqual(0, result.returncode, output)
        self.assertIn("==> Registry preflight only", output)
        self.assertIn("SkipUpload is set; registry push, remote pull, and container restart are skipped.", output)
        self.assertIn("target services: draw2video-admin", output)
        self.assertIn("registry images: 111.228.39.103:3000/root/manman/draw2video-admin:v0.0.1", output)
        self.assertIn("remote env path: /opt/code/.frontend-test.env", output)
        self.assertIn("remote compose path: /opt/code/docker-compose.frontend.yml", output)
        self.assertIn("generated env preview:", output)

        for required in [
            "FRONTEND_DEPLOY_ENV=test",
            "FRONTEND_IMAGE_TAG=v0.0.1",
            "FRONTEND_IMAGE_REGISTRY_PREFIX=127.0.0.1:3000/root/manman/",
            "DRAW2VIDEO_ADMIN_PORT=8081",
            "DRAW2VIDEO_CLIENT_PORT=13000",
            "DRAW2VIDEO_GUIDE_PORT=8082",
            "ADMIN_GATEWAY_HOST=host.docker.internal",
            "ADMIN_GATEWAY_PORT=48080",
            "CLIENT_GATEWAY_HOST=host.docker.internal",
            "CLIENT_GATEWAY_PORT=48080",
        ]:
            self.assertIn(required, output)

        for forbidden in [
            "==> Push frontend images to registry",
            "==> Prepare remote compose file",
            "==> Pull images and restart containers",
            "> docker tag",
            "> docker push",
            "> ssh ",
            "> scp ",
            "draw2video-client:v0.0.1",
            "draw2video-guide:v0.0.1",
        ]:
            self.assertNotIn(forbidden, output)

    def test_admin_test_compose_and_runbook_require_auditable_health_evidence(self):
        compose = read("script/docker/docker-compose.frontend.yml")
        runbook = read("script/deployment-runbook.md")

        self.assertIn("container_name: draw2video-admin", compose)
        self.assertIn('image: ${FRONTEND_IMAGE_REGISTRY_PREFIX:-}draw2video-admin:${FRONTEND_IMAGE_TAG:-latest}', compose)
        self.assertIn('"${DRAW2VIDEO_ADMIN_PORT:-8081}:80"', compose)
        self.assertIn("http://127.0.0.1/", compose)
        self.assertIn("retries: 10", compose)

        for required in [
            "./script/deploy-frontend-images.ps1 -Server manman -DeployEnv test -Target admin -UseRegistry -SkipBuild -SkipSave -SkipUpload",
            "ssh manman \"cd /opt/code && docker compose --env-file .frontend-test.env -f docker-compose.frontend.yml ps draw2video-admin\"",
            "ssh manman \"curl -fsS -I http://127.0.0.1:8081/\"",
            "记录证据时写明命令、开始时间、结束时间、退出码、HTTP 状态、响应摘要和失败日志路径",
            "发布证据必须包含 `docker compose ps` 的 `healthy` 状态、HTTP 探活输出、服务日志路径和失败时的回滚决策",
        ]:
            self.assertIn(required, runbook)


if __name__ == "__main__":
    unittest.main()
