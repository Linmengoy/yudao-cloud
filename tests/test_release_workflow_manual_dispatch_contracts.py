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

    def test_test_workflow_defaults_image_tag_to_next_patch_after_stable_tag(self):
        workflow = read(".gitea/workflows/yudao-micro-cicd.yml")

        self.assertIn("INPUT_PREVIOUS_STABLE_IMAGE_TAG: ${{ github.event.inputs.previous_stable_image_tag }}", workflow)
        self.assertIn('stable_test_image_tag="$(tr -d \'[:space:]\' < "${test_image_version_file}")"', workflow)
        self.assertIn('previous_stable_image_tag="${INPUT_PREVIOUS_STABLE_IMAGE_TAG:-${stable_test_image_tag}}"', workflow)
        self.assertIn('previous_stable_image_tag_source="workflow_dispatch.previous_stable_image_tag"', workflow)
        self.assertIn('previous_stable_image_tag_source="${test_image_version_file}"', workflow)
        self.assertIn('previous_stable_image_tag must be a base semantic version', workflow)
        self.assertIn('base_version="${previous_stable_image_tag#v}"', workflow)
        self.assertIn('next_patch=$((patch + 1))', workflow)
        self.assertIn('image_tag="v${major}.${minor}.${next_patch}"', workflow)
        self.assertIn('image_tag_source="next patch after ${previous_stable_image_tag_source}"', workflow)
        self.assertIn('previous stable image tag source: ${PREVIOUS_STABLE_IMAGE_TAG_SOURCE}', workflow)
        self.assertIn("PREVIOUS_STABLE_IMAGE_TAG=${previous_stable_image_tag}", workflow)

    def test_runbook_step_six_documents_tag_and_project_registry_scope(self):
        runbook = read("script/deployment-runbook.md")

        self.assertIn("`image_tag` 填本次要发布的测试镜像版本", runbook)
        self.assertIn("上一稳定为 `v0.0.1` 时自动发布 `v0.0.2`", runbook)
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

    def test_prod_workflow_infers_previous_stable_sha_before_preflight(self):
        workflow = read(".gitea/workflows/yudao-micro-cicd-prod.yml")
        runbook = read("script/deployment-runbook.md")

        self.assertIn("required: false", workflow)
        self.assertIn("INPUT_PREVIOUS_STABLE_IMAGE_TAG: ${{ github.event.inputs.previous_stable_image_tag }}", workflow)
        self.assertIn('previous_stable_image_tag="${INPUT_PREVIOUS_STABLE_IMAGE_TAG:-}"', workflow)
        self.assertIn('auth_fetch --force origin "refs/tags/prod-stable-*:refs/tags/prod-stable-*" || true', workflow)
        self.assertIn("git tag --sort=-creatordate 'prod-stable-*'", workflow)
        self.assertIn('stable_sha="$(git rev-parse --short=12 "${stable_ref}^{commit}"', workflow)
        self.assertIn('[ "${stable_sha}" != "${image_tag}" ]', workflow)
        self.assertIn('previous_stable_image_tag_source="prod-stable tag ${stable_ref}"', workflow)
        self.assertIn('previous_stable_image_tag_source="prod-stable-* tag lookup returned no usable previous stable SHA"', workflow)
        self.assertIn('deploy_env_file="/opt/deploy/yudao-micro/.env"', workflow)
        self.assertIn('sed -n \'s/^MICRO_IMAGE_TAG=//p\' "${deploy_env_file}"', workflow)
        self.assertIn('inspect_containers="yudao-gateway-prod yudao-system-prod', workflow)
        self.assertIn('running_image="$(docker inspect "${container}" --format \'{{.Config.Image}}\'', workflow)
        self.assertIn('previous_stable_image_tag="${running_image##*:}"', workflow)
        self.assertIn('previous_stable_image_tag_source="running container ${container}"', workflow)
        self.assertIn("previous_stable_image_tag could not be inferred", workflow)
        self.assertIn("previous_stable_image_tag must be a 12-character Git SHA tag", workflow)
        self.assertIn("previous_stable_image_tag equals current image tag", workflow)
        self.assertIn("PREVIOUS_STABLE_IMAGE_TAG_SOURCE=${previous_stable_image_tag_source}", workflow)
        self.assertIn("previous stable image tag source: ${PREVIOUS_STABLE_IMAGE_TAG_SOURCE}", workflow)

        self.assertIn("prod 当前 tag 始终使用当前 12 位 Git SHA", runbook)
        self.assertIn("留空时 workflow 会先查 `prod-stable-*` tag", runbook)
        self.assertIn("prod-stable-* tag lookup returned no usable previous stable SHA", runbook)
        self.assertIn("不是当前候选短 SHA", runbook)

    def test_prod_preflight_records_previous_stable_source(self):
        gate = read("script/docker/verify-release-evidence.sh")

        self.assertIn("PREVIOUS_STABLE_IMAGE_TAG_SOURCE", gate)
        self.assertIn("- previous stable image tag source: ${PREVIOUS_STABLE_IMAGE_TAG_SOURCE:-not-provided}", gate)


if __name__ == "__main__":
    unittest.main()
