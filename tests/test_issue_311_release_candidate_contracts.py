import unittest
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class Issue311ReleaseCandidateContractTest(unittest.TestCase):

    def test_release_candidate_commit_message_binds_issue_ownership(self):
        log = subprocess.run(
            ["git", "-C", str(ROOT), "log", "-n", "20", "--format=%B"],
            check=True,
            text=True,
            stdout=subprocess.PIPE,
        ).stdout

        self.assertIn("release: bind candidate scope evidence (#311 #146 #173 #174)", log)
        self.assertIn("records full/short commit SHA, build input file list, and clean proof for #311", log)
        self.assertIn("release scope audit for #146/#173/#174", log)
        self.assertIn("exclude processing work such as #310/#311 from release candidates", log)

    def test_release_candidate_evidence_script_records_sha_inputs_and_clean_proof(self):
        script = read("script/release-candidate-evidence.ps1")
        runbook = read("script/deployment-runbook.md")

        for required in [
            "full_commit_sha",
            "short_commit_sha",
            "Build Input Files",
            "git status --short --",
            "Release candidate build input paths are not clean",
            "#146",
            "#173",
            "#174",
            "script/release-scope-audit.ps1",
            "script/docker/verify-release-evidence.ps1",
        ]:
            self.assertIn(required, script)

        self.assertIn('[string[]]$CandidateIssues = @("#146", "#173", "#174")', script)
        self.assertNotIn('[string[]]$CandidateIssues = @("#311"', script)
        self.assertIn("release-candidate-$ShortSha.md", script)

        self.assertIn("./script/release-candidate-evidence.ps1", runbook)
        self.assertIn("完整 commit SHA", runbook)
        self.assertIn("短 SHA", runbook)
        self.assertIn("构建输入文件清单", runbook)
        self.assertIn("clean 证明", runbook)
        self.assertIn("避免手写 SHA 与实际构建输入不一致", runbook)

    def test_release_scope_audit_excludes_processing_work_from_candidate(self):
        script = read("script/release-scope-audit.ps1")
        runbook = read("script/deployment-runbook.md")

        for required in [
            "AllowedIssues",
            "IncludedIssues",
            "ProcessingIssues",
            "processing issue entered release candidate",
            "issue is outside release scope",
            "manifest line $lineNumber maps a candidate file to processing work",
        ]:
            self.assertIn(required, script)

        self.assertIn("-AllowedIssues '#146','#173','#174'", runbook)
        self.assertIn("-ProcessingIssues '#310'", runbook)
        self.assertIn("excluded processing issues:", runbook)
        self.assertIn("#310: parent release audit remains processing; no unreviewed files included", runbook)
        self.assertIn("candidate commits and files map only to #146/#173/#174 or completed dependencies", runbook)


if __name__ == "__main__":
    unittest.main()
