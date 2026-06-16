import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


EXPECTED_TABLES = {
    "aigc_community_post",
    "aigc_community_post_like",
    "aigc_community_comment",
    "aigc_community_share_log",
    "aigc_community_follow",
    "aigc_community_author_stats",
    "aigc_community_audit_log",
    "aigc_guide_content",
}


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def parse_pom(path: str) -> ET.Element:
    return ET.fromstring(read(path))


def pom_texts(root: ET.Element, xpath: str) -> list[str]:
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    return [
        element.text.strip()
        for element in root.findall(xpath, namespace)
        if element.text and element.text.strip()
    ]


def service_block(compose: str, service: str) -> str:
    match = re.search(rf"^  {re.escape(service)}:\n(?P<body>(?:    .*\n|      .*\n|        .*\n|          .*\n|            .*\n|              .*\n|                .*\n|$)+)", compose, re.MULTILINE)
    if not match:
        raise AssertionError(f"Service block not found: {service}")
    return match.group("body")


class CommunityReleaseGateTest(unittest.TestCase):

    def test_workflow_builds_and_verifies_aigc_community(self):
        workflow = read(".gitea/workflows/yudao-micro-cicd.yml")

        self.assertIn("- aigc-community", workflow)
        self.assertIn('aigc-community) module="yudao-module-aigc-community/yudao-module-aigc-community-server" ;;', workflow)
        self.assertIn("docker compose -f script/docker/docker-compose-micro.yml config --services | grep -Fx \"${service}\"", workflow)

        verify_match = re.search(r'verify_services="(?P<services>[^"]+)"', workflow)
        self.assertIsNotNone(verify_match)
        verify_services = verify_match.group("services").split()
        self.assertIn("aigc-workflow", verify_services)
        self.assertIn("aigc-community", verify_services)
        self.assertIn("break", workflow)
        self.assertNotIn("exit 0", workflow)

    def test_compose_service_has_runtime_gate_dependencies_and_healthcheck(self):
        compose = read("script/docker/docker-compose-micro.yml")
        block = service_block(compose, "aigc-community")

        self.assertIn("context: ../../yudao-module-aigc-community/yudao-module-aigc-community-server", block)
        self.assertIn('"48097:48097"', block)
        self.assertIn("SPRING_PROFILES_ACTIVE: dev", block)
        self.assertIn("SPRING_CLOUD_NACOS_SERVER_ADDR: nacos:8848", block)
        self.assertIn("/opt/data/yudao-logs:/root/logs", block)
        self.assertIn('["CMD", "curl", "-fsS", "http://127.0.0.1:48097/actuator/health"]', block)

        for dependency in ["mysql", "redis", "nacos", "yudao-system", "yudao-infra", "yudao-member", "aigc-asset", "aigc-workflow"]:
            self.assertIn(f"{dependency}:", block)

    def test_maven_aggregator_and_server_dependencies_cover_build_gate(self):
        root_pom = parse_pom("pom.xml")
        community_pom = parse_pom("yudao-module-aigc-community/pom.xml")
        server_pom = parse_pom("yudao-module-aigc-community/yudao-module-aigc-community-server/pom.xml")

        self.assertIn("yudao-module-aigc-community", pom_texts(root_pom, "m:modules/m:module"))
        self.assertEqual(
            ["yudao-module-aigc-community-api", "yudao-module-aigc-community-server"],
            pom_texts(community_pom, "m:modules/m:module"),
        )

        artifact_ids = pom_texts(server_pom, "m:dependencies/m:dependency/m:artifactId")
        for artifact_id in [
            "yudao-module-aigc-community-api",
            "yudao-module-aigc-asset-api",
            "yudao-module-aigc-workflow-api",
            "yudao-module-member-api",
            "yudao-spring-boot-starter-rpc",
        ]:
            self.assertIn(artifact_id, artifact_ids)

        self.assertNotIn("system", pom_texts(server_pom, "m:dependencies/m:dependency/m:scope"))
        self.assertEqual([], pom_texts(server_pom, "m:dependencies/m:dependency/m:systemPath"))

    def test_boot_repackage_is_enabled_for_runnable_community_jar(self):
        server_pom = parse_pom("yudao-module-aigc-community/yudao-module-aigc-community-server/pom.xml")

        artifact_ids = pom_texts(server_pom, "m:build/m:plugins/m:plugin/m:artifactId")
        self.assertIn("spring-boot-maven-plugin", artifact_ids)
        self.assertIn("repackage", pom_texts(server_pom, "m:build/m:plugins/m:plugin/m:executions/m:execution/m:goals/m:goal"))
        self.assertEqual(["${project.artifactId}"], pom_texts(server_pom, "m:build/m:finalName"))

    def test_dockerfile_supports_healthcheck_probe(self):
        dockerfile = read("yudao-module-aigc-community/yudao-module-aigc-community-server/Dockerfile")

        self.assertIn("apt-get install -y --no-install-recommends curl", dockerfile)
        self.assertIn("COPY ./target/yudao-module-aigc-community-server.jar app.jar", dockerfile)
        self.assertIn("EXPOSE 48097", dockerfile)

    def test_community_nacos_configs_define_datasource_redis_and_actuator(self):
        for path, host in [
            ("script/nacos/community/aigc-community-server-dev.yaml", "mysql"),
            ("script/nacos/community/aigc-community-server-local.yaml", "111.228.39.103"),
        ]:
            with self.subTest(path=path):
                config = read(path)
                self.assertIn("spring:", config)
                self.assertIn("datasource:", config)
                self.assertIn("dynamic:", config)
                self.assertIn("primary: master", config)
                self.assertIn(f"jdbc:mysql://{host}:3306/community_db", config)
                self.assertIn("username: root", config)
                self.assertIn("data:", config)
                self.assertIn("redis:", config)
                self.assertIn("database: 3", config)
                self.assertIn("base-path: /actuator", config)
                self.assertRegex(config, r"include:\s+(health,info,prometheus|'\*')")

    def test_rpc_configuration_registers_required_feign_clients(self):
        config = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/framework/rpc/config/RpcConfiguration.java"
        )

        self.assertIn("@EnableFeignClients", config)
        for client in ["AigcAssetApi.class", "AigcWorkflowApi.class", "MemberUserApi.class"]:
            self.assertIn(client, config)

    def test_gateway_routes_and_docs_cover_community_api_boundaries(self):
        for path in ["script/nacos/gateway/gateway-server-dev.yaml", "script/nacos/gateway/gateway-server-local.yaml"]:
            with self.subTest(path=path):
                gateway = read(path)
                self.assertIn("id: aigc-community-admin-api", gateway)
                self.assertIn("id: aigc-community-app-api", gateway)
                self.assertIn("grayLb://aigc-community-server", gateway)
                self.assertIn("Path=/admin-api/aigc/community/**, /admin-api/aigc/guide/**", gateway)
                self.assertIn("Path=/app-api/aigc/community/**, /app-api/aigc/guide/**", gateway)
                self.assertIn("service-name: aigc-community-server", gateway)

    def test_runbook_verifies_every_table_index_and_rollback_boundary(self):
        sql = read("yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/resources/schema/community_db.sql")
        runbook = read("yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/resources/schema/community_db_release_runbook.md")

        created_tables = set(re.findall(r"CREATE TABLE IF NOT EXISTS `([^`]+)`", sql))
        self.assertEqual(EXPECTED_TABLES, created_tables)

        for table in EXPECTED_TABLES:
            self.assertIn(table, runbook)
            self.assertIn(f"SHOW INDEX FROM {table};", runbook)

        self.assertIn("sha256sum /opt/data/mysql-backup/community/<backup-file>.sql", runbook)
        self.assertIn("does not insert initialization data", runbook)
        self.assertIn("DROP TABLE IF EXISTS aigc_community_post;", runbook)
        self.assertIn("Verification SQL output summary", runbook)

    def test_release_gate_document_archives_ci_version_db_and_smoke_evidence(self):
        gates = read("script/docker/community-release-gates.md")

        for issue in [
            "#144", "#145", "#146", "#147", "#148", "#149", "#150", "#151", "#152", "#153", "#154",
            "#161", "#162", "#163", "#164", "#165",
        ]:
            self.assertIn(issue, gates)

        for required in [
            "script/docker/community-release-evidence-index.md",
            "review result, test result, release decision",
            "reviewer:",
            "contract test command:",
            "Do not mark #150 as `test:done`",
            "workflow run url:",
            "release evidence file:",
            "docker compose version:",
            "current version tag:",
            "previous stable tag:",
            "backup sha256:",
            "sql commit sha:",
            "post-deploy health result:",
            "/actuator/health result:",
            "test account",
            "Gateway admin route",
            "User like",
            "Admin audit approve",
            "rollback decision",
            "aigc-community runner docker evidence",
            "docker daemon status:",
            "aigc-community stable rollback target",
            "previous stable workflow run url:",
            "community_db archived migration evidence",
            "rollback drill:",
            "aigc-community smoke route evidence",
            "gateway admin status and response summary:",
            "aigc-community release bridge archive",
            "related issue writebacks: #125, #126, #127, #128, #159",
        ]:
            self.assertIn(required, gates)

    def test_workflow_writes_release_evidence_with_rollback_tag(self):
        workflow = read(".gitea/workflows/yudao-micro-cicd.yml")

        self.assertIn("previous_stable_image_tag", workflow)
        self.assertIn("Write release evidence summary", workflow)
        self.assertIn("RELEASE_EVIDENCE_FILE=tmp/release-evidence/${service}-${image_tag}.md", workflow)
        self.assertIn("workflow run url: ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}", workflow)
        self.assertIn("immutable image tag: ${MICRO_IMAGE_TAG}", workflow)
        self.assertIn("previous stable image tag: ${PREVIOUS_STABLE_IMAGE_TAG:-not-provided}", workflow)
        self.assertIn("rollback command: MICRO_IMAGE_TAG=<previous-stable-sha>", workflow)
        self.assertIn("Append release verification evidence", workflow)
        self.assertIn("deployment verification", workflow)
        self.assertIn("docker compose -f script/docker/docker-compose-micro.yml ps", workflow)
        self.assertIn("curl -fsS http://127.0.0.1:48097/actuator/health", workflow)
        self.assertIn("curl -fsS http://127.0.0.1:48080/admin-api/aigc/community/admin/post/page?pageNo=1&pageSize=1", workflow)
        self.assertIn("script/docker/community-release-gates.md", workflow)
        self.assertIn("script/docker/community-release-evidence-index.md", workflow)

    def test_prod_workflow_records_runner_recovery_and_smoke_route_evidence(self):
        workflow = read(".gitea/workflows/yudao-micro-cicd-prod.yml")

        for required in [
            "runs-on: manman2-prod",
            "docker version",
            "docker compose version",
            "docker compose -f script/docker/docker-compose-micro-prod.yml config --services | grep -Fx \"${service}\"",
            "runner: ${RUNNER_NAME:-manman2-prod}",
            "target environment: prod",
            "deploy host: local manman2",
            "Prepare local prod network",
            "docker network inspect yudao-network-prod",
            "Required infra container is missing",
            "docker compose -f docker-compose-micro.yml up -d --no-deps",
            "docker compose -f docker-compose-micro.yml ps --status running --services",
            "docker compose -f docker-compose-micro.yml logs --tail=100",
            "curl -fsS http://127.0.0.1:48097/actuator/health",
            "curl -i -sS ${admin_url}",
            "curl -i -sS ${app_url}",
            "gateway admin smoke result:",
            "gateway app smoke result:",
            "service logs tail:",
        ]:
            self.assertIn(required, workflow)

    def test_prod_workflow_uses_sha_tags_and_writes_release_evidence(self):
        workflow = read(".gitea/workflows/yudao-micro-cicd-prod.yml")

        self.assertIn("previous_stable_image_tag", workflow)
        self.assertIn("RELEASE_EVIDENCE_FILE=tmp/release-evidence/prod-${service}-${image_tag}.md", workflow)
        self.assertIn("Write release evidence summary", workflow)
        self.assertIn("immutable image tag: ${MICRO_IMAGE_TAG}", workflow)
        self.assertIn("previous stable image tag: ${PREVIOUS_STABLE_IMAGE_TAG:-not-provided}", workflow)
        self.assertIn("MICRO_IMAGE_TAG=${MICRO_IMAGE_TAG}", workflow)
        self.assertIn("FRONTEND_IMAGE_TAG=${FRONTEND_IMAGE_TAG}", workflow)
        self.assertIn('export MICRO_IMAGE_TAG="${MICRO_IMAGE_TAG}" FRONTEND_IMAGE_TAG="${FRONTEND_IMAGE_TAG}"', workflow)
        self.assertIn("rollback command: MICRO_IMAGE_TAG=<previous-stable-sha>", workflow)
        self.assertIn("Append release verification evidence", workflow)
        self.assertIn("deployment verification", workflow)
        self.assertIn("docker compose -f docker-compose-micro.yml ps", workflow)
        self.assertIn("curl -fsS http://127.0.0.1:48097/actuator/health", workflow)
        self.assertIn("script/docker/community-release-evidence-index.md", workflow)

    def test_release_evidence_index_covers_all_release_gate_templates(self):
        index = read("script/docker/community-release-evidence-index.md")

        for issue in ["#145", "#146", "#147", "#148", "#149", "#150", "#151", "#152", "#153", "#154", "#161", "#162", "#163", "#164", "#165"]:
            self.assertIn(issue, index)

        for required in [
            "aigc-community review evidence",
            "reviewed files:",
            "blocking findings:",
            "release decision: review:ready | review:done | blocked",
            "aigc-community test evidence",
            "contract test command: python -m pytest tests/test_community_release_gates.py tests/test_review_ready_contracts.py",
            "skipped tests: none | <reason>",
            "Do not use `test:done`",
            "task:failed",
            "task:ready + task:done",
            "aigc-community smoke evidence",
            "test account:",
            "Gateway admin route result:",
            "Gateway app route result:",
            "Admin audit approve result:",
            "aigc-community CI build evidence",
            "workflow run url:",
            "release evidence file:",
            "immutable image tag:",
            "community_db migration record",
            "backup sha256:",
            "verification SQL summary:",
            "aigc-community rollback version record",
            "previous stable git sha:",
            "nacos config boundary:",
            "database rollback boundary:",
            "aigc-community deployment health evidence",
            "compose deploy command:",
            "/actuator/health result:",
            "rollback executed:",
            "aigc-community runner docker evidence",
            "docker daemon status:",
            "compose service check:",
            "aigc-community stable rollback target",
            "previous stable workflow run url:",
            "missing evidence or \"none\":",
            "community_db archived migration evidence",
            "rollback drill:",
            "aigc-community smoke route evidence",
            "nacos config source:",
            "gateway app status and response summary:",
            "aigc-community release bridge archive",
            "service log summary:",
            "related issue writebacks: #125, #126, #127, #128, #159",
        ]:
            self.assertIn(required, index)

    def test_runbook_archives_issue_163_required_fields(self):
        runbook = read("yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/resources/schema/community_db_release_runbook.md")

        for required in [
            "For #163 archival evidence",
            "backup file",
            "backup sha256",
            "sql commit sha",
            "approval owner",
            "executor",
            "verifier",
            "execution window",
            "utf8mb4",
            "rollback drill",
            "no writes yet: run the `DROP TABLE IF EXISTS ...` block",
            "writes already happened: restore `/opt/data/mysql-backup/community/<backup-file>.sql`",
        ]:
            self.assertIn(required, runbook)


if __name__ == "__main__":
    unittest.main()
