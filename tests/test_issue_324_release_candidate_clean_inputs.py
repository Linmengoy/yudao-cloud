import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def ps_quote(value: Path | str) -> str:
    return "'" + str(value).replace("'", "''") + "'"


class Issue324ReleaseCandidateCleanInputsTest(unittest.TestCase):

    def test_candidate_evidence_ignores_pycache_noise_but_records_workflow_inputs(self):
        powershell = shutil.which("powershell") or shutil.which("pwsh")
        if powershell is None:
            self.skipTest("PowerShell is required for release candidate evidence execution")

        script = ROOT / "script/release-candidate-evidence.ps1"
        cache_noise = ROOT / "tests" / "__pycache__" / "__issue_324_noise.pyc"
        cache_noise.parent.mkdir(exist_ok=True)
        try:
            cache_noise.write_bytes(b"issue 324 local cache noise\n")
            with tempfile.TemporaryDirectory() as tmp:
                evidence = Path(tmp) / "candidate.md"
                command = " ".join(
                    [
                        "&",
                        ps_quote(script),
                        "-CandidateIssues '#316','#318','#320'",
                        "-BuildInputPaths '.gitea/workflows/yudao-micro-cicd.yml','.gitea/workflows/yudao-micro-cicd-prod.yml','script/release-candidate-evidence.ps1'",
                        "-EvidencePath",
                        ps_quote(evidence),
                    ]
                )
                result = subprocess.run(
                    [
                        powershell,
                        "-NoProfile",
                        "-ExecutionPolicy",
                        "Bypass",
                        "-Command",
                        command,
                    ],
                    cwd=ROOT,
                    text=True,
                    capture_output=True,
                )
                output = result.stdout + result.stderr
                self.assertEqual(0, result.returncode, output)

                evidence_text = evidence.read_text(encoding="utf-8-sig")
        finally:
            cache_noise.unlink(missing_ok=True)

        self.assertIn("candidate_issues: #316, #318, #320", evidence_text)
        self.assertIn("clean_result: clean for listed build input files", evidence_text)
        self.assertIn("- .gitea/workflows/yudao-micro-cicd.yml", evidence_text)
        self.assertIn("- .gitea/workflows/yudao-micro-cicd-prod.yml", evidence_text)
        self.assertNotIn("tests/__pycache__", evidence_text)

    def test_candidate_evidence_rejects_dirty_workflow_build_input(self):
        powershell = shutil.which("powershell") or shutil.which("pwsh")
        if powershell is None:
            self.skipTest("PowerShell is required for release candidate evidence execution")

        script = ROOT / "script/release-candidate-evidence.ps1"
        dirty_workflow = ROOT / ".gitea" / "workflows" / "__issue_324_dirty.yml"
        try:
            dirty_workflow.write_text("name: issue-324-dirty-workflow\n", encoding="utf-8")
            with tempfile.TemporaryDirectory() as tmp:
                result = subprocess.run(
                    [
                        powershell,
                        "-NoProfile",
                        "-ExecutionPolicy",
                        "Bypass",
                        "-File",
                        str(script),
                        "-CandidateIssues",
                        "#316,#318,#320",
                        "-BuildInputPaths",
                        ".gitea/workflows/__issue_324_dirty.yml",
                        "-EvidencePath",
                        str(Path(tmp) / "dirty.md"),
                    ],
                    cwd=ROOT,
                    text=True,
                    capture_output=True,
                )
        finally:
            dirty_workflow.unlink(missing_ok=True)

        output = result.stdout + result.stderr
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Release candidate build input paths are not clean", output)
        self.assertIn(".gitea/workflows/__issue_324_dirty.yml", output)

    def test_runbook_documents_issue_324_recovery_boundary(self):
        runbook = read("script/deployment-runbook.md")

        self.assertIn("#308 证据回写必须包含", runbook)
        self.assertIn("tests/__pycache__", runbook)
        self.assertIn("不能作为构建输入变更", runbook)
        self.assertIn("若构建输入文件存在未提交、未暂存或未跟踪变更，脚本必须失败", runbook)


if __name__ == "__main__":
    unittest.main()
