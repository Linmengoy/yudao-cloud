import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class TaskDetailContractTest(unittest.TestCase):

    def test_app_task_detail_hides_structured_output_data_from_public_responses(self):
        controller = read(
            "yudao-module-aigc-task/yudao-module-aigc-task-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/task/controller/app/AigcTaskAppController.java"
        )

        self.assertIn("taskService.getUserTaskWithResult(id, getLoginUserId())", controller)
        hide_match = re.search(
            r"private AigcTaskRespDTO hideInternalFields\(AigcTaskRespDTO respDTO\) \{(?P<body>.*?)\n    \}",
            controller,
            re.S,
        )
        self.assertIsNotNone(hide_match)
        body = hide_match.group("body")
        for field in (
            ".setCostPrice(null)",
            ".setProviderId(null)",
            ".setExternalTaskId(null)",
            ".setFailCode(null)",
            ".setOutputData(null)",
        ):
            self.assertIn(field, body)

    def test_admin_task_detail_loads_readable_result_but_does_not_return_raw_output_data(self):
        controller = read(
            "yudao-module-aigc-task/yudao-module-aigc-task-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/task/controller/admin/task/AigcTaskController.java"
        )

        detail_match = re.search(
            r"public CommonResult<AigcTaskRespDTO> getTask\(@RequestParam\(\"id\"\) Long id\) \{(?P<body>.*?)\n    \}",
            controller,
            re.S,
        )
        self.assertIsNotNone(detail_match)
        body = detail_match.group("body")
        self.assertIn("taskService.validateTaskExists(id)", body)
        self.assertIn("taskService.getTaskWithResult(id)", body)
        self.assertIn(".setOutputData(null)", body)

    def test_admin_task_detail_page_shows_human_detail_not_structured_json(self):
        page = read("yudao-ui/draw2video-admin/src/views/aigc/task/detail.vue")
        types = read("yudao-ui/draw2video-admin/src/api/aigc/task/types.ts")

        self.assertIn('label="详细信息"', page)
        self.assertIn("displayOutput", page)
        self.assertIn("task.value.outputText || task.value.outputSummary || '-'", page)
        self.assertNotIn('label="结构化输出"', page)
        self.assertNotIn("formatJson(task.outputData)", page)
        resp_match = re.search(r"export interface AigcTaskRespVO \{(?P<body>.*?)\n\}", types, re.S)
        self.assertIsNotNone(resp_match)
        self.assertNotIn("outputData?: string", resp_match.group("body"))
        self.assertIn("outputSummary?: string", types)

    def test_client_task_detail_does_not_type_or_render_structured_output_data(self):
        result = read("yudao-ui/draw2video-client/src/features/tasks/components/task-result.tsx")
        types = read("yudao-ui/draw2video-client/src/features/tasks/task-types.ts")

        self.assertNotIn("outputData?: string", types)
        self.assertNotIn("formatJson", result)
        self.assertNotIn("task.outputData", result)
        self.assertNotIn("结构化结果", result)
        self.assertNotIn("<pre", result)
        self.assertIn("task.outputSummary", result)
        self.assertIn("结果摘要", result)


if __name__ == "__main__":
    unittest.main()
