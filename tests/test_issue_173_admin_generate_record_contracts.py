import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class AdminGenerateRecordContractTest(unittest.TestCase):

    def test_admin_generate_record_list_exposes_monitoring_filters_and_actions(self):
        page = read("yudao-ui/draw2video-admin/src/views/aigc/gen/record/index.vue")
        api = read("yudao-ui/draw2video-admin/src/api/aigc/gen/record/index.ts")
        types = read("yudao-ui/draw2video-admin/src/api/aigc/gen/types.ts")

        for prop in [
            "userId",
            "taskId",
            "generateNo",
            "providerTaskId",
            "failReason",
            "createTime",
            "generateMode",
            "modelId",
            "providerCode",
            "status",
            "hasError",
        ]:
            self.assertIn(f'prop="{prop}"', page)
            self.assertRegex(types, rf"\b{prop}\??:")

        for column in [
            'prop="generateNo"',
            'prop="taskId"',
            'prop="userId"',
            'prop="providerTaskId"',
            'prop="providerStatus"',
            'prop="priceAmount"',
            'prop="costAmount"',
            'prop="failReason"',
            'prop="failMessage"',
        ]:
            self.assertIn(column, page)

        self.assertIn("AigcGenerateRecordApi.getGenerateRecordPage(queryParams)", page)
        self.assertIn("router.push({ path: '/aigc/gen/record/detail/' + id })", page)
        self.assertIn("AigcGenerateRecordApi.syncGenerateTask(taskId)", page)
        self.assertIn("v-hasPermi=\"['aigc:gen:query']\"", page)
        self.assertIn("v-hasPermi=\"['aigc:gen:update']\"", page)
        self.assertIn("url: '/aigc/gen/record/page'", api)
        self.assertIn("url: '/aigc/gen/record/get?id=' + id", api)
        self.assertIn("url: '/aigc/gen/record/sync?taskId=' + taskId", api)

    def test_admin_generate_record_detail_exposes_generation_and_provider_evidence(self):
        detail = read("yudao-ui/draw2video-admin/src/views/aigc/gen/record/detail.vue")
        types = read("yudao-ui/draw2video-admin/src/api/aigc/gen/types.ts")
        backend_vo = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/gen/controller/admin/record/vo/AigcGenerateRecordRespVO.java"
        )

        for label in [
            "Generate No",
            "Task ID",
            "User ID",
            "Client Request",
            "Provider Task",
            "Provider Status",
            "Freeze ID",
            "Sale Price",
            "Cost Price",
            "Fail Code",
            "Fail Message",
        ]:
            self.assertIn(f'label="{label}"', detail)

        for tab in [
            "Prompt",
            "Input Params",
            "Output Text",
            "Output Data",
            "Result URLs",
            "Asset IDs",
            "Provider Logs",
        ]:
            self.assertIn(f'label="{tab}"', detail)

        self.assertIn("AigcGenerateRecordApi.getGenerateRecord(Number(route.params.id))", detail)
        self.assertIn("AigcGenerateProviderLogApi.getGenerateProviderLogPage(providerLogQuery)", detail)
        self.assertIn("providerLogQuery.taskId = record.value.taskId", detail)
        self.assertIn("AigcGenerateRecordApi.syncGenerateTask(record.value.taskId)", detail)
        self.assertIn('title="Provider Log Detail"', detail)
        self.assertIn("requestSummary", detail)
        self.assertIn("responseSummary", detail)

        for field in [
            "clientRequestId",
            "providerTaskId",
            "providerStatus",
            "prompt",
            "inputParams",
            "outputText",
            "outputData",
            "outputUrls",
            "assetIds",
            "freezeId",
            "priceAmount",
            "costAmount",
            "failReason",
            "failMessage",
        ]:
            self.assertRegex(types, rf"\b{field}\??:")
            self.assertRegex(backend_vo, rf"private .* {field};")

    def test_admin_generate_record_backend_requires_permissions_for_query_and_sync(self):
        controller = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/gen/controller/admin/record/AigcGenerateRecordController.java"
        )
        page_req = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/gen/controller/admin/record/vo/AigcGenerateRecordPageReqVO.java"
        )

        for route in [
            '@GetMapping("/get")',
            '@GetMapping("/page")',
            '@PostMapping("/sync")',
        ]:
            self.assertIn(route, controller)
        self.assertEqual(2, controller.count("@ss.hasPermission('aigc:gen:query')"))
        self.assertEqual(1, controller.count("@ss.hasPermission('aigc:gen:update')"))
        self.assertIn("generateRecordService.validateGenerateRecordExists(id)", controller)
        self.assertIn("generateRecordService.getGenerateRecordPage(reqVO)", controller)
        self.assertIn("generateRecordService.syncTask(taskId)", controller)

        for field in [
            "userId",
            "taskId",
            "generateNo",
            "generateType",
            "generateMode",
            "modelId",
            "providerCode",
            "status",
            "providerTaskId",
            "failReason",
            "createTime",
            "submitTime",
            "hasError",
        ]:
            self.assertRegex(page_req, rf"private .* {field};")


if __name__ == "__main__":
    unittest.main()
