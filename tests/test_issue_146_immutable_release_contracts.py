import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class Issue146ImmutableReleaseContractTest(unittest.TestCase):

    def test_test_workflow_records_semver_test_tag_and_rollback_command(self):
        workflow = read(".gitea/workflows/yudao-micro-cicd.yml")

        self.assertIn("previous_stable_image_tag", workflow)
        self.assertIn('test_image_version_file="script/docker/test-image-version"', workflow)
        self.assertIn('stable_test_image_tag="$(tr -d \'[:space:]\' < "${test_image_version_file}")"', workflow)
        self.assertIn('image_tag="v${major}.${minor}.${next_patch}"', workflow)
        self.assertIn("^v[0-9]+\\.[0-9]+\\.[0-9]+", workflow)
        self.assertIn("MICRO_IMAGE_TAG=${image_tag}", workflow)
        self.assertIn("FRONTEND_IMAGE_TAG=${image_tag}", workflow)
        self.assertIn("INPUT_PREVIOUS_STABLE_IMAGE_TAG: ${{ github.event.inputs.previous_stable_image_tag }}", workflow)
        self.assertIn("PREVIOUS_STABLE_IMAGE_TAG=${previous_stable_image_tag}", workflow)
        self.assertIn("immutable image tag: ${MICRO_IMAGE_TAG}", workflow)
        self.assertIn("previous stable image tag: ${PREVIOUS_STABLE_IMAGE_TAG:-not-provided}", workflow)
        self.assertIn("rollback command: MICRO_IMAGE_TAG=<previous-test-version>", workflow)
        self.assertIn("--no-build --no-deps --force-recreate", workflow)
        self.assertIn("RELEASE_EVIDENCE_FILE=tmp/release-evidence/${service}-${image_tag}.md", workflow)

    def test_prod_workflow_uses_git_sha_tag_and_explicit_previous_stable_rollback(self):
        workflow = read(".gitea/workflows/yudao-micro-cicd-prod.yml")

        self.assertIn("previous_stable_image_tag", workflow)
        self.assertIn('image_tag="$(git rev-parse --short=12 HEAD)"', workflow)
        self.assertIn("MICRO_IMAGE_TAG=${image_tag}", workflow)
        self.assertIn("FRONTEND_IMAGE_TAG=${image_tag}", workflow)
        self.assertIn("PREVIOUS_STABLE_IMAGE_TAG=${{ github.event.inputs.previous_stable_image_tag }}", workflow)
        self.assertIn("RELEASE_EVIDENCE_FILE=tmp/release-evidence/prod-${service}-${image_tag}.md", workflow)
        self.assertIn("commit sha: $(git rev-parse HEAD)", workflow)
        self.assertIn("immutable image tag: ${MICRO_IMAGE_TAG}", workflow)
        self.assertIn("previous stable image tag: ${PREVIOUS_STABLE_IMAGE_TAG:-not-provided}", workflow)
        self.assertIn("rollback command: MICRO_IMAGE_TAG=<previous-stable-sha>", workflow)
        self.assertIn("rollback command: MICRO_IMAGE_TAG=${PREVIOUS_STABLE_IMAGE_TAG}", workflow)
        self.assertIn("--no-build --no-deps --force-recreate", workflow)

    def test_release_templates_make_latest_a_local_only_fallback(self):
        gates = read("script/docker/community-release-gates.md")
        index = read("script/docker/community-release-evidence-index.md")

        self.assertIn("## Immutable version evidence for #146, #153, and #162", gates)
        self.assertIn("Use the 12-character commit SHA from the workflow", gates)
        self.assertIn("Do not use `latest` as", gates)
        self.assertIn("Local development may omit the tag and fall back to `latest`", gates)
        self.assertIn("production evidence must record a commit SHA tag", gates)
        self.assertIn("previous stable tag:", gates)
        self.assertIn("rollback target tag:", gates)
        self.assertIn("image list:", gates)
        self.assertIn("rollback command:", gates)

        issue_146_row = re.search(r"\| #146 \|(?P<body>[^\n]+)", index)
        self.assertIsNotNone(issue_146_row)
        row = issue_146_row.group("body")
        for required in [
            "immutable image version",
            "current SHA tag",
            "previous stable tag",
            "image list",
            "rollback command",
            "local `latest` fallback boundary",
            "task:ready + task:done",
        ]:
            self.assertIn(required, row)

    def test_compose_files_keep_local_latest_fallback_while_accepting_release_tags(self):
        for path in [
            "script/docker/docker-compose-micro.yml",
            "script/docker/docker-compose-micro-prod.yml",
        ]:
            with self.subTest(path=path):
                compose = read(path)
                for service in [
                    "yudao-system",
                    "yudao-infra",
                    "yudao-member",
                    "yudao-pay",
                    "yudao-gateway",
                    "aigc-model",
                    "aigc-billing",
                    "aigc-task",
                    "aigc-asset",
                    "aigc-safety",
                    "aigc-gen",
                    "aigc-workflow",
                    "aigc-community",
                ]:
                    self.assertRegex(
                        compose,
                        rf"image:\s+\$\{{MICRO_IMAGE_REGISTRY_PREFIX:-\}}?{re.escape(service)}:\$\{{MICRO_IMAGE_TAG:-latest\}}|"
                        rf"image:\s+{re.escape(service)}:\$\{{MICRO_IMAGE_TAG:-latest\}}",
                    )


if __name__ == "__main__":
    unittest.main()
