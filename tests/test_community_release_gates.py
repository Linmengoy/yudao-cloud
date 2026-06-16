import re
import unittest
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


if __name__ == "__main__":
    unittest.main()
