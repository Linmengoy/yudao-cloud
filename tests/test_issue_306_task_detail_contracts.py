import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class TaskDetailContractTest(unittest.TestCase):

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


if __name__ == "__main__":
    unittest.main()
