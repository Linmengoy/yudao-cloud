import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class Issue311ReleaseCandidateContractTest(unittest.TestCase):

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

        self.assertIn("./script/release-candidate-evidence.ps1", runbook)
        self.assertIn("完整 commit SHA", runbook)
        self.assertIn("短 SHA", runbook)
        self.assertIn("构建输入文件清单", runbook)
        self.assertIn("clean 证明", runbook)


if __name__ == "__main__":
    unittest.main()
