import shutil
import subprocess
import tempfile
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

    def test_issue_318_scope_audit_passes_for_308_and_excludes_processing_followups(self):
        powershell = shutil.which("powershell") or shutil.which("pwsh")
        if powershell is None:
            self.skipTest("PowerShell is required for release scope audit execution")

        script = ROOT / "script/release-scope-audit.ps1"
        with tempfile.TemporaryDirectory() as tmp:
            manifest = Path(tmp) / "candidate-manifest.txt"
            notes = Path(tmp) / "release-notes.md"
            manifest.write_text(
                "\n".join(
                    [
                        "script/release-candidate-evidence.ps1|#308|review:done",
                        "script/deployment-runbook.md|#999|completed-dependency",
                    ]
                ),
                encoding="utf-8",
            )
            notes.write_text(
                "\n".join(
                    [
                        "included issues:",
                        "- #308",
                        "",
                        "excluded processing issues:",
                        "- #306",
                        "- #176",
                        "- #170",
                        "- #150",
                        "",
                        "exclusion rationale:",
                        "- candidate commits and files map only to #308 or completed dependencies",
                    ]
                ),
                encoding="utf-8",
            )

            result = subprocess.run(
                [
                    powershell,
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    str(script),
                    "-AllowedIssues",
                    "#308",
                    "-IncludedIssues",
                    "#308",
                    "-ProcessingIssues",
                    "#306,#176,#170,#150",
                    "-FrontendTarget",
                    "admin",
                    "-BackendServices",
                    "aigc-workflow,aigc-gen",
                    "-ManifestPath",
                    str(manifest),
                    "-ReleaseNotesPath",
                    str(notes),
                ],
                cwd=ROOT,
                text=True,
                capture_output=True,
            )

        output = result.stdout + result.stderr
        self.assertEqual(0, result.returncode, output)
        self.assertIn("release scope audit passed", output)
        self.assertIn("included issues: #308", output)
        self.assertIn("excluded processing issues: #306, #176, #170, #150", output)

    def test_issue_316_scope_audit_rejects_release_notes_without_excluded_scope(self):
        powershell = shutil.which("powershell") or shutil.which("pwsh")
        if powershell is None:
            self.skipTest("PowerShell is required for release scope audit execution")

        script = ROOT / "script/release-scope-audit.ps1"
        with tempfile.TemporaryDirectory() as tmp:
            notes = Path(tmp) / "release-notes.md"
            notes.write_text(
                "\n".join(
                    [
                        "included issues:",
                        "- #308",
                        "",
                        "exclusion rationale:",
                        "- candidate files map only to allowed scope",
                    ]
                ),
                encoding="utf-8",
            )

            result = subprocess.run(
                [
                    powershell,
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    str(script),
                    "-AllowedIssues",
                    "#308",
                    "-IncludedIssues",
                    "#308",
                    "-ProcessingIssues",
                    "#306,#176,#170,#150",
                    "-FrontendTarget",
                    "admin",
                    "-ReleaseNotesPath",
                    str(notes),
                ],
                cwd=ROOT,
                text=True,
                capture_output=True,
            )

        output = result.stdout + result.stderr
        self.assertNotEqual(0, result.returncode)
        self.assertIn("release notes missing scope section field: excluded processing issues", output)

    def test_issue_320_candidate_evidence_script_executes_and_rejects_dirty_inputs(self):
        powershell = shutil.which("powershell") or shutil.which("pwsh")
        if powershell is None:
            self.skipTest("PowerShell is required for release candidate evidence execution")

        script = ROOT / "script/release-candidate-evidence.ps1"
        with tempfile.TemporaryDirectory() as tmp:
            evidence = Path(tmp) / "candidate.md"
            clean_result = subprocess.run(
                [
                    powershell,
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    str(script),
                    "-CandidateIssues",
                    "#308",
                    "-BuildInputPaths",
                    "script/release-candidate-evidence.ps1",
                    "-EvidencePath",
                    str(evidence),
                ],
                cwd=ROOT,
                text=True,
                capture_output=True,
            )
            clean_output = clean_result.stdout + clean_result.stderr

            self.assertEqual(0, clean_result.returncode, clean_output)
            evidence_text = evidence.read_text(encoding="utf-8-sig")
            self.assertIn("full_commit_sha:", evidence_text)
            self.assertIn("short_commit_sha:", evidence_text)
            self.assertIn("clean_result: clean for listed build input files", evidence_text)
            self.assertIn("- script/release-candidate-evidence.ps1", evidence_text)

            dirty_input = ROOT / "tests" / "__tmp_issue_320_dirty_input.txt"
            try:
                dirty_input.write_text("dirty candidate input\n", encoding="utf-8")
                dirty_result = subprocess.run(
                    [
                        powershell,
                        "-NoProfile",
                        "-ExecutionPolicy",
                        "Bypass",
                        "-File",
                        str(script),
                        "-CandidateIssues",
                        "#308",
                        "-BuildInputPaths",
                        "tests/__tmp_issue_320_dirty_input.txt",
                        "-EvidencePath",
                        str(Path(tmp) / "dirty.md"),
                    ],
                    cwd=ROOT,
                    text=True,
                    capture_output=True,
                )
            finally:
                dirty_input.unlink(missing_ok=True)

        dirty_output = dirty_result.stdout + dirty_result.stderr
        self.assertNotEqual(0, dirty_result.returncode)
        self.assertIn("Release candidate build input paths are not clean", dirty_output)
        self.assertIn("tests/__tmp_issue_320_dirty_input.txt", dirty_output)

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
