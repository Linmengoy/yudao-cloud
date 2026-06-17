import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class Issue319And321ReleasePreflightContractsTest(unittest.TestCase):

    def test_issue_319_admin_frontend_port_is_env_file_controlled(self):
        compose = read("script/docker/docker-compose.frontend.yml")
        runbook = read("script/deployment-runbook.md")

        self.assertIn('image: ${FRONTEND_IMAGE_REGISTRY_PREFIX:-}draw2video-admin:${FRONTEND_IMAGE_TAG:-latest}', compose)
        self.assertIn('"${DRAW2VIDEO_ADMIN_PORT:-8081}:80"', compose)
        self.assertNotIn('"8081:80"', compose)

        self.assertIn("DRAW2VIDEO_ADMIN_PORT=8081", runbook)
        self.assertIn(
            "docker compose --env-file .frontend-test.env -f docker-compose.frontend.yml ps draw2video-admin",
            runbook,
        )
        self.assertIn("ssh manman \"curl -fsS -I http://127.0.0.1:8081/\"", runbook)
        self.assertIn("记录证据时写明命令、开始时间、结束时间、退出码、HTTP 状态、响应摘要和失败日志路径", runbook)

    def test_issue_321_windows_preflight_wrapper_records_reproducible_log_context(self):
        wrapper = read("script/docker/verify-release-evidence.ps1")
        runbook = read("script/deployment-runbook.md")

        for required in [
            'Write-Log "START $(Get-Date -Format o)"',
            'Write-Log "command=$Command"',
            'Write-Log "script=$ScriptPath"',
            'Write-Log "log_path=$LogPath"',
            '"DEPLOY_ENV"',
            '"BUILD_SERVICE"',
            '"MICRO_IMAGE_TAG"',
            '"PREVIOUS_STABLE_IMAGE_TAG"',
            '"RELEASE_EVIDENCE_FILE"',
            '"MICRO_IMAGE_REGISTRY_PREFIX"',
            '"GIT_BASH_PATH"',
            'Write-Log "env:$name=$([Environment]::GetEnvironmentVariable($name))"',
            'Write-Log "bash=$bashPath"',
            'Write-Log "END $(Get-Date -Format o) exit=$exitCode"',
        ]:
            self.assertIn(required, wrapper)

        self.assertIn("Windows 本机不要直接依赖 shell 关联启动", runbook)
        self.assertIn("日志会写入 `tmp/release-gates/windows-verify-*`", runbook)
        self.assertIn("必须随工单写回开始时间、结束时间、退出码和日志路径", runbook)

    def test_issue_321_windows_preflight_distinguishes_environment_and_gate_failures(self):
        wrapper = read("script/docker/verify-release-evidence.ps1")
        runbook = read("script/deployment-runbook.md")

        self.assertIn("execution environment failure: bash executable was not found", wrapper)
        self.assertIn("Install Git Bash, set GIT_BASH_PATH, or run through WSL", wrapper)
        self.assertIn('Write-Log "END $(Get-Date -Format o) exit=126"', wrapper)
        self.assertIn("exit 126", wrapper)

        self.assertIn("WaitForExit($TimeoutSeconds * 1000)", wrapper)
        self.assertIn("execution environment failure: bash startup or script execution exceeded ${TimeoutSeconds}s", wrapper)
        self.assertIn('Write-Log "END $(Get-Date -Format o) exit=124"', wrapper)
        self.assertIn("exit 124", wrapper)

        self.assertIn("release evidence gate failed: verify-release-evidence.sh returned $exitCode", wrapper)
        self.assertIn("release evidence gate passed", wrapper)
        self.assertIn("退出码 `126` 表示 bash/Git Bash/WSL 执行环境不可用", runbook)
        self.assertIn("退出码 `124` 表示启动或执行超时", runbook)
        self.assertIn("其它非零退出码表示 `verify-release-evidence.sh` 判定 release evidence 不满足", runbook)

    def test_issue_321_windows_preflight_starts_bash_with_captured_output(self):
        wrapper = read("script/docker/verify-release-evidence.ps1")

        for required in [
            "Get-Command git.exe",
            'foreach ($relativePath in @("bin\\bash.exe", "usr\\bin\\bash.exe"))',
            "[System.Diagnostics.ProcessStartInfo]::new()",
            "$processInfo.UseShellExecute = $false",
            "$processInfo.RedirectStandardOutput = $true",
            "$processInfo.RedirectStandardError = $true",
            "$processInfo.CreateNoWindow = $true",
            "$process.Start()",
            "failed to start bash process",
            "$stdoutTask = $process.StandardOutput.ReadToEndAsync()",
            "$stderrTask = $process.StandardError.ReadToEndAsync()",
            "Set-Content -LiteralPath $stdout -Value $stdoutText -Encoding utf8",
            "Set-Content -LiteralPath $stderr -Value $stderrText -Encoding utf8",
        ]:
            self.assertIn(required, wrapper)

        self.assertNotIn("Start-Process", wrapper)
        self.assertNotIn("-NoNewWindow", wrapper)

    def test_issue_315_release_evidence_docker_commands_have_timeouts(self):
        gate = read("script/docker/verify-release-evidence.sh")

        for required in [
            "run_docker_with_timeout()",
            "DOCKER_CLI_TIMEOUT_SECONDS:-30",
            "timeout \"$seconds\" docker \"$@\"",
            "run_docker_with_timeout image inspect",
            "run_docker_with_timeout pull \"$previous_ref\"",
            "command: docker image inspect",
            "command: docker pull",
        ]:
            self.assertIn(required, gate)


if __name__ == "__main__":
    unittest.main()
