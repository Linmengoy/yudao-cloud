import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class GenerationPersistenceContractsTest(unittest.TestCase):

    def test_issue_263_generation_outputs_are_archived_and_checked_before_persistence(self):
        archive_service = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/gen/service/media/AigcMediaArchiveService.java"
        )
        record_service = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/gen/service/record/AigcGenerateRecordServiceImpl.java"
        )
        test_file = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/test/java/"
            "cn/iocoder/yudao/module/aigc/gen/service/media/AigcMediaArchiveServiceTest.java"
        )

        self.assertIn("archiveOutputData(String outputData)", archive_service)
        self.assertIn("archiveOutputUrls(String outputUrls)", archive_service)
        self.assertIn("isInlineMediaSource(source)", archive_service)
        self.assertIn("assertNoPersistentInlineMedia(String value, String fieldName)", archive_service)
        self.assertIn("must not contain inline base64 media", archive_service)
        self.assertIn("String archivedOutputUrls = mediaArchiveService.archiveOutputUrls(reqDTO.getOutputUrls())", record_service)
        self.assertIn("String archivedOutputUrls = mediaArchiveService.archiveOutputUrls(resp.getOutputUrls())", record_service)
        self.assertIn("assertNoInlinePersistentMedia(archivedOutputData, archivedOutputUrls)", record_service)
        self.assertIn("aigc_gen_record.output_data", record_service)
        self.assertIn("aigc_gen_record.output_urls", record_service)
        self.assertIn("testArchiveOutputData_uploadsInlineMediaArrayValues", test_file)
        self.assertIn("testAssertNoPersistentInlineMedia_rejectsBase64Media", test_file)
        self.assertIn("testArchiveOutputUrls_uploadsDataUrlBeforePersistence", test_file)

    def test_issue_269_workspace_model_selection_respects_reference_count(self):
        contracts = read("yudao-ui/draw2video-client/src/app/(app)/app/workspace-page-contracts.ts")
        page = read("yudao-ui/draw2video-client/src/app/(app)/app/page.tsx")
        test_file = read("yudao-ui/draw2video-client/src/app/(app)/app/workspace-page-contracts.test.ts")

        self.assertIn("getModelMaxReferenceImages", contracts)
        self.assertIn("getCompatibleModels", contracts)
        self.assertIn("referenceImageCount", page)
        self.assertIn("getCompatibleModels(quickModels, selectedTab, quickGenerationMode, referenceImageCount)", page)
        self.assertIn("pickDefaultModelId(quickModels, selectedTab, quickGenerationMode, referenceImageCount)", page)
        self.assertIn("switches default video model away from first-frame-only models", test_file)
        self.assertIn("IMAGE_TO_VIDEO", test_file)


if __name__ == "__main__":
    unittest.main()
