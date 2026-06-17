import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class ReleaseWorkflowManualDispatchContractsTest(unittest.TestCase):

    def test_manual_dispatch_checkout_fetches_selected_ref_with_token(self):
        for path in [
            ".gitea/workflows/yudao-micro-cicd.yml",
            ".gitea/workflows/yudao-micro-cicd-prod.yml",
        ]:
            with self.subTest(path=path):
                workflow = read(path)

                self.assertIn("auth_fetch()", workflow)
                self.assertIn('git -c http.extraHeader="Authorization: token ${GITEA_TOKEN}" fetch "$@"', workflow)
                self.assertIn("auth_fetch --prune origin", workflow)
                self.assertNotIn("git fetch --prune origin", workflow)
                self.assertIn('checkout_ref="${GITEA_REF:-HEAD}"', workflow)
                self.assertIn('echo "Checkout ref: ${checkout_ref}"', workflow)
                self.assertIn('echo "Checkout sha: ${GITEA_SHA:-not-set}"', workflow)
                self.assertIn('auth_fetch --depth=1 origin "${GITEA_SHA}" || auth_fetch --depth=1 origin "${checkout_ref}"', workflow)

    def test_test_workflow_accepts_explicit_image_tag_for_each_release(self):
        workflow = read(".gitea/workflows/yudao-micro-cicd.yml")

        self.assertIn("image_tag:", workflow)
        self.assertIn("INPUT_IMAGE_TAG: ${{ github.event.inputs.image_tag }}", workflow)
        self.assertIn('image_tag_source="${test_image_version_file}"', workflow)
        self.assertIn('image_tag_source="workflow_dispatch.image_tag"', workflow)
        self.assertIn("Invalid test image tag from ${image_tag_source}", workflow)
        self.assertIn("TEST_IMAGE_TAG_SOURCE=${image_tag_source}", workflow)
        self.assertIn("test image tag source: ${TEST_IMAGE_TAG_SOURCE}", workflow)

    def test_runbook_step_six_documents_tag_and_project_registry_scope(self):
        runbook = read("script/deployment-runbook.md")

        self.assertIn("`image_tag` 填本次要发布的测试镜像版本", runbook)
        self.assertIn("后端镜像版本没有更新", runbook)
        self.assertIn("导致每次都推送和拉取同一个 tag", runbook)
        self.assertIn("127.0.0.1:3000/root/manman/<service>:<image_tag>", runbook)
        self.assertIn("root/manman/<service>:<tag>", runbook)

    def test_registry_login_uses_registry_credentials_not_github_token(self):
        for path in [
            ".gitea/workflows/yudao-micro-cicd.yml",
            ".gitea/workflows/yudao-micro-cicd-prod.yml",
        ]:
            with self.subTest(path=path):
                workflow = read(path)

                self.assertIn("GITEA_REGISTRY_USERNAME: ${{ secrets.GITEA_REGISTRY_USERNAME }}", workflow)
                self.assertIn("GITEA_REGISTRY_PASSWORD: ${{ secrets.GITEA_REGISTRY_PASSWORD }}", workflow)
                self.assertIn('registry_username="${GITEA_REGISTRY_USERNAME:-root}"', workflow)
                self.assertIn('registry_password="${GITEA_REGISTRY_PASSWORD:-root}"', workflow)
                self.assertIn('docker login "${REGISTRY_HOST}" -u "${registry_username}" --password-stdin', workflow)
                self.assertNotIn('echo "${GITEA_TOKEN}" | docker login', workflow)


if __name__ == "__main__":
    unittest.main()
