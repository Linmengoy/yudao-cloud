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

    def test_issue_247_video_outputs_are_stored_as_urls_and_assets(self):
        archive_service_test = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/test/java/"
            "cn/iocoder/yudao/module/aigc/gen/service/media/AigcMediaArchiveServiceTest.java"
        )
        record_service = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/gen/service/record/AigcGenerateRecordServiceImpl.java"
        )

        self.assertIn("testArchiveOutputUrls_uploadsVideoDataUrlBeforePersistence", archive_service_test)
        self.assertIn('eq("video/mp4")', archive_service_test)
        self.assertIn('mediaArchiveService.assertNoPersistentInlineMedia(archived, "aigc_gen_record.output_urls")', archive_service_test)
        self.assertIn('case "VIDEO" -> assetApi.createVideoAsset(reqDTO).getCheckedData();', record_service)
        self.assertIn(".setOutputUrls(JSONUtil.toJsonStr(storedUrls))", record_service)
        self.assertIn(".setAssetIds(JSONUtil.toJsonStr(assetIds))", record_service)

    def test_issue_248_marketing_home_reuses_www_visuals_and_public_community(self):
        page = read("yudao-ui/draw2video-client/src/app/(marketing)/page.tsx")

        self.assertIn("/www-home/assets/images/hero-cinema.webp", page)
        self.assertIn("/www-home/assets/images/inspiration-portrait.webp", page)
        self.assertIn("getCommunityPosts({ pageNo: 1, pageSize: 6, sort: \"hot\" })", page)
        self.assertIn("CommunityPostCard", page)
        self.assertIn("公开作品", page)
        self.assertIn("Seedance 2.0", page)


if __name__ == "__main__":
    unittest.main()
