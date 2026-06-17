import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def require_section(source: str, pattern: str) -> str:
    match = re.search(pattern, source, re.S)
    if match is None:
        raise AssertionError(f"missing section matching {pattern!r}")
    return match.group("body")


class Issue308ImageToImageContractsTest(unittest.TestCase):

    def test_canvas_image_to_image_validates_and_normalizes_references_before_submit(self):
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )
        error_codes = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-api/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/enums/ErrorCodeConstants.java"
        )

        self.assertIn("CANVAS_NODE_RUN_INPUT_PARAMS_INVALID", error_codes)
        self.assertIn("输入参数必须是 JSON 对象", error_codes)
        self.assertIn("CANVAS_NODE_RUN_REFERENCE_IMAGE_REQUIRED", error_codes)
        self.assertIn("图生图缺少可用参考图", error_codes)
        self.assertIn("CANVAS_NODE_RUN_REFERENCE_IMAGE_INVALID", error_codes)

        run_node = require_section(
            service,
            r"public AigcCanvasNodeRunRespVO runNode\(AigcCanvasNodeRunReqVO reqVO, Long userId\) \{(?P<body>.*?)\n    \}",
        )
        self.assertIn("projectService.validateEditableProject(reqVO.getProjectId(), userId)", run_node)
        self.assertIn("reqVO.setInputParams(normalizeNodeRunInputParams(reqVO));", run_node)
        self.assertLess(
            run_node.index("reqVO.setInputParams(normalizeNodeRunInputParams(reqVO));"),
            run_node.index("generateApi.submit(new AigcGenerateSubmitReqDTO()"),
        )

        normalizer = require_section(
            service,
            r"private String normalizeNodeRunInputParams\(AigcCanvasNodeRunReqVO reqVO\) \{(?P<body>.*?)\n    private LinkedHashSet<String> collectStableReferenceImages",
        )
        self.assertIn('!"IMAGE_TO_IMAGE".equals(reqVO.getGenerateMode())', normalizer)
        self.assertIn("JSONUtil.isTypeJSONObject(reqVO.getInputParams())", normalizer)
        self.assertIn("throw serviceException(CANVAS_NODE_RUN_INPUT_PARAMS_INVALID)", normalizer)
        self.assertIn("collectStableReferenceImages(params)", normalizer)
        self.assertIn("resolveAssetReferenceUrls(params)", normalizer)
        self.assertIn("throw serviceException(CANVAS_NODE_RUN_REFERENCE_IMAGE_REQUIRED)", normalizer)
        self.assertIn('params.set("referenceImages", normalizedReferences)', normalizer)
        self.assertIn('params.set("inputImageUrls", normalizedReferences)', normalizer)
        self.assertIn("normalizeInputImageSnapshots", normalizer)

    def test_canvas_image_to_image_rejects_local_or_draft_ids_until_stable_asset_is_available(self):
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )

        stable_reference_guard = require_section(
            service,
            r"private boolean isStableImageReference\(String source\) \{(?P<body>.*?)\n    \}",
        )
        self.assertIn('StrUtil.startWithAnyIgnoreCase(value, "http://", "https://", "data:image/")', stable_reference_guard)
        self.assertNotRegex(stable_reference_guard, r"draft_|node_|localInputImageIds")

        asset_resolution = require_section(
            service,
            r"private List<String> resolveAssetReferenceUrls\(JSONObject params\) \{(?P<body>.*?)\n    \}",
        )
        for field in ["referenceAssetIds", "inputAssetIds", "inputImageIds"]:
            self.assertIn(f'collectAssetIds(assetIds, params.getJSONArray("{field}"))', asset_resolution)
        self.assertIn("assetApi.getAssets(new ArrayList<>(assetIds)).getCheckedData()", asset_resolution)
        self.assertIn("throw serviceException(CANVAS_NODE_RUN_REFERENCE_IMAGE_INVALID)", asset_resolution)

        collect_asset_ids = require_section(
            service,
            r"private void collectAssetIds\(Set<Long> assetIds, JSONArray values\) \{(?P<body>.*?)\n    \}",
        )
        self.assertIn("Long assetId = parseLongQuietly(item)", collect_asset_ids)
        self.assertIn("assetId != null", collect_asset_ids)

        parse_long = require_section(
            service,
            r"private Long parseLongQuietly\(Object value\) \{(?P<body>.*?)\n    \}",
        )
        self.assertIn("Long.valueOf(text)", parse_long)
        self.assertIn("catch (NumberFormatException ex)", parse_long)
        self.assertIn("return null", parse_long)

    def test_gpt_image_provider_ignores_draft_ids_instead_of_decoding_as_base64(self):
        provider = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/gen/framework/client/GptImageProviderClient.java"
        )

        self.assertIn("isSupportedImageSource(url)", provider)
        self.assertIn("isSupportedImageSource(source)", provider)
        self.assertIn('StrUtil.startWithAnyIgnoreCase(value, "http://", "https://", "data:image/")', provider)
        self.assertIn("looksLikeBase64Image(value)", provider)
        self.assertIn('throw new IllegalArgumentException("图生图参考图片格式不支持")', provider)

    def test_gpt_image_provider_only_forwards_supported_reference_sources(self):
        provider = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/gen/framework/client/GptImageProviderClient.java"
        )

        parser = require_section(
            provider,
            r"private List<ImageInput> parseInputImages\(String inputParams\) \{(?P<body>.*?)\n    private boolean isSupportedImageSource",
        )
        self.assertIn("JSONArray referenceImages = params.getJSONArray(\"referenceImages\")", parser)
        self.assertIn("JSONArray inputImages = params.getJSONArray(\"inputImages\")", parser)
        self.assertIn("JSONArray inputImageUrls = params.getJSONArray(\"inputImageUrls\")", parser)
        self.assertGreaterEqual(parser.count("isSupportedImageSource("), 3)
        self.assertIn("images.stream().noneMatch(image -> image.source().equals(url))", parser)
        self.assertNotIn("Base64.getDecoder().decode", parser)

    def test_frontend_does_not_submit_local_image_ids_as_server_reference_ids(self):
        image_node = read("yudao-ui/draw2video-client/src/features/canvas/ImageNode.tsx")

        build_params = require_section(
            image_node,
            r"function buildServerInputParams\(params: Record<string, unknown>, ids: string\[\], snapshots: ResultNodeData\[\"inputImages\"\]\) \{(?P<body>.*?)\n\}",
        )
        self.assertIn("localInputImageIds: ids", build_params)
        self.assertNotIn("inputImageIds: ids", build_params)
        self.assertIn("inputImageUrls: snapshots", build_params)
        self.assertIn("inputImages: snapshots.map", build_params)


if __name__ == "__main__":
    unittest.main()
