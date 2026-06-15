package cn.iocoder.yudao.module.aigc.gen.framework.client;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitRespDTO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenApiGenerationsProviderClientTest {

    private final OpenApiGenerationsProviderClient client = new OpenApiGenerationsProviderClient();

    @Test
    public void testGetProviderCode() {
        assertEquals("openapi-generations", client.getProviderCode());
        assertEquals("OPENAPI_GENERATIONS", client.getClientType());
    }

    @Test
    public void testFactoryUsesClientTypeFromExtraConfig() {
        OpenApiGenerationsProviderClient openApiClient = new OpenApiGenerationsProviderClient();
        MockAigcProviderClient mockClient = new MockAigcProviderClient();
        AigcProviderClientFactory factory = new AigcProviderClientFactory(List.of(openApiClient, mockClient));

        AigcProviderClient resolved = factory.getClient(new AigcProviderSubmitReqDTO()
                .setProviderCode("any-provider")
                .setProviderExtraConfig("{\"clientType\":\"OPENAPI_GENERATIONS\"}"));

        assertSame(openApiClient, resolved);
    }

    @Test
    public void testFactoryUsesModelClientTypeBeforeProviderExtraConfig() {
        GptImageProviderClient gptImageClient = new GptImageProviderClient();
        MidjourneyProviderClient midjourneyClient = new MidjourneyProviderClient();
        VolcImagesGenerationsProviderClient volcClient = new VolcImagesGenerationsProviderClient();
        MockAigcProviderClient mockClient = new MockAigcProviderClient();
        AigcProviderClientFactory factory = new AigcProviderClientFactory(List.of(
                gptImageClient, midjourneyClient, volcClient, mockClient));

        AigcProviderClient resolvedGptImage = factory.getClient(new AigcProviderSubmitReqDTO()
                .setProviderCode("provider_avman")
                .setProviderModel("gpt-image-2")
                .setProviderExtraConfig("{\"clientType\":\"MIDJOURNEY\"}"));
        AigcProviderClient resolvedMidjourney = factory.getClient(new AigcProviderSubmitReqDTO()
                .setProviderCode("provider_avman")
                .setProviderModel("midjourney-fast"));
        AigcProviderClient resolvedVolc = factory.getClient(new AigcProviderSubmitReqDTO()
                .setProviderCode("provider_apilio_volcv")
                .setProviderModel("doubao-seedream-5-0-260128"));

        assertSame(gptImageClient, resolvedGptImage);
        assertSame(midjourneyClient, resolvedMidjourney);
        assertSame(volcClient, resolvedVolc);
    }

    @Test
    public void testSubmit_textToVideo() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer("/openapi/v1/generations", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, """
                    {"id":"gen-task-001","status":"pending"}
                    """);
        });
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setGenerateType("VIDEO")
                    .setGenerateMode("TEXT_TO_VIDEO")
                    .setPrompt("一只猫在跳舞"));

            assertTrue(resp.getSuccess(), resp.getErrorCode() + ": " + resp.getErrorMessage());
            assertEquals("gen-task-001", resp.getProviderTaskId());
            assertEquals("pending", resp.getProviderStatus());
            assertFalse(resp.getFinished());

            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals("seedance-2.0", body.getStr("model"));
            assertEquals("text_to_video", body.getStr("mode"));
            assertEquals("一只猫在跳舞", body.getStr("prompt"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_processingKeepsProviderTaskId() throws Exception {
        HttpServer server = startServer("/openapi/v1/generations", exchange -> sendJson(exchange, 200, """
                {"id":"gen-123","status":"processing"}
                """));
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setGenerateType("VIDEO")
                    .setGenerateMode("TEXT_TO_VIDEO")
                    .setPrompt("processing task"));

            assertTrue(resp.getSuccess(), resp.getErrorMessage());
            assertFalse(resp.getFinished());
            assertEquals("gen-123", resp.getProviderTaskId());
            assertEquals("processing", resp.getProviderStatus());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_processingWithoutIdFails() throws Exception {
        HttpServer server = startServer("/openapi/v1/generations", exchange -> sendJson(exchange, 200, """
                {"status":"processing"}
                """));
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setGenerateType("VIDEO")
                    .setGenerateMode("TEXT_TO_VIDEO")
                    .setPrompt("missing id"));

            assertFalse(resp.getSuccess());
            assertTrue(resp.getFinished());
            assertEquals("MISSING_PROVIDER_TASK_ID", resp.getErrorCode());
            assertEquals("processing", resp.getProviderStatus());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_imageToVideo() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer("/openapi/v1/generations", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, """
                    {"id":"gen-task-002","status":"pending"}
                    """);
        });
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setGenerateType("VIDEO")
                    .setGenerateMode("IMAGE_TO_VIDEO")
                    .setPrompt("人物向前走")
                    .setInputParams("""
                            {"image_url":"https://example.com/img.jpg","ratio":"16:9","duration":5}
                            """));

            assertTrue(resp.getSuccess(), resp.getErrorCode() + ": " + resp.getErrorMessage());
            assertEquals("gen-task-002", resp.getProviderTaskId());
            assertFalse(resp.getFinished());

            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals("image_to_video", body.getStr("mode"));
            assertEquals("https://example.com/img.jpg", body.getStr("image_url"));
            assertEquals("16:9", body.getStr("ratio"));
            assertEquals(5, body.getInt("duration"));
            assertFalse(body.containsKey("referenceImages"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_firstLastFrame() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer("/openapi/v1/generations", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, """
                    {"id":"gen-task-003","status":"pending"}
                    """);
        });
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setGenerateType("VIDEO")
                    .setGenerateMode("FIRST_LAST_FRAME_VIDEO")
                    .setPrompt("人物从站立变成坐下")
                    .setInputParams("""
                            {"referenceImages":["https://example.com/first.jpg","https://example.com/last.jpg"],"duration":8}
                            """));

            assertTrue(resp.getSuccess(), resp.getErrorCode() + ": " + resp.getErrorMessage());
            assertEquals("gen-task-003", resp.getProviderTaskId());

            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals("first_last_frame", body.getStr("mode"));
            JSONArray images = body.getJSONArray("image_urls");
            assertNotNull(images);
            assertEquals(2, images.size());
            assertEquals("https://example.com/first.jpg", images.getStr(0));
            assertEquals("https://example.com/last.jpg", images.getStr(1));
            assertFalse(body.containsKey("referenceImages"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_multiRef() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer("/openapi/v1/generations", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, """
                    {"id":"gen-task-004","status":"pending"}
                    """);
        });
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setGenerateType("VIDEO")
                    .setGenerateMode("MULTI_REF_VIDEO")
                    .setPrompt("参考视频风格，生成新的内容")
                    .setInputParams("""
                            {"referenceImages":["https://example.com/ref1.jpg"],"referenceVideos":["https://example.com/vid1.mp4"],"referenceAudios":["https://example.com/audio1.mp3"],"duration":10}
                            """));

            assertTrue(resp.getSuccess(), resp.getErrorCode() + ": " + resp.getErrorMessage());
            assertEquals("gen-task-004", resp.getProviderTaskId());

            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals("multi_ref", body.getStr("mode"));
            assertFalse(body.containsKey("image_url"));
            assertEquals(1, body.getJSONArray("image_urls").size());
            assertEquals("https://example.com/ref1.jpg", body.getJSONArray("image_urls").getStr(0));
            assertEquals(1, body.getJSONArray("video_urls").size());
            assertEquals("https://example.com/vid1.mp4", body.getJSONArray("video_urls").getStr(0));
            assertEquals(1, body.getJSONArray("audio_urls").size());
            assertEquals("https://example.com/audio1.mp3", body.getJSONArray("audio_urls").getStr(0));
            assertFalse(body.containsKey("referenceImages"));
            assertFalse(body.containsKey("referenceVideos"));
            assertFalse(body.containsKey("referenceAudios"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_officialSeedanceReviewsImageUrls() throws Exception {
        AtomicReference<String> assetUploadBody = new AtomicReference<>();
        AtomicReference<String> generationBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/openapi/v1/assets", exchange -> {
            assetUploadBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, """
                    {"officialId":"asset-cn-001","status":"Active"}
                    """);
        });
        server.createContext("/openapi/v1/generations", exchange -> {
            generationBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, """
                    {"id":"gen-official-cn","status":"pending"}
                    """);
        });
        server.start();
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setProviderModel("seedance-2-official2")
                    .setProviderExtraConfig("{\"seedanceAssetGroupCn\":\"ag-cn\",\"seedanceAssetReviewPollIntervalMillis\":0}")
                    .setGenerateType("VIDEO")
                    .setGenerateMode("MULTI_REF_VIDEO")
                    .setPrompt("官方模型素材提审")
                    .setInputParams("""
                            {"referenceImages":["https://example.com/ref.jpg"],"duration":6}
                            """));

            assertTrue(resp.getSuccess(), resp.getErrorCode() + ": " + resp.getErrorMessage());
            JSONObject assetBody = JSONUtil.parseObj(assetUploadBody.get());
            assertEquals("ag-cn", assetBody.getStr("groupId"));
            assertEquals("https://example.com/ref.jpg", assetBody.getStr("url"));

            JSONObject body = JSONUtil.parseObj(generationBody.get());
            assertEquals("seedance-2-official2", body.getStr("model"));
            assertEquals("asset://asset-cn-001", body.getJSONArray("image_urls").getStr(0));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_intlSeedanceCreatesIntlAssetGroup() throws Exception {
        AtomicReference<String> groupBody = new AtomicReference<>();
        AtomicReference<String> assetUploadBody = new AtomicReference<>();
        AtomicReference<String> generationBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/openapi/v1/asset-groups", exchange -> {
            groupBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, """
                    {"officialId":"ag-intl"}
                    """);
        });
        server.createContext("/openapi/v1/assets", exchange -> {
            assetUploadBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, """
                    {"officialId":"asset-intl-001","status":"Active"}
                    """);
        });
        server.createContext("/openapi/v1/generations", exchange -> {
            generationBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, """
                    {"id":"gen-official-intl","status":"pending"}
                    """);
        });
        server.start();
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setProviderModel("seedance-2-intl-xl-fast")
                    .setProviderExtraConfig("{\"seedanceAssetReviewPollIntervalMillis\":0}")
                    .setGenerateType("VIDEO")
                    .setGenerateMode("IMAGE_TO_VIDEO")
                    .setPrompt("国际模型素材提审")
                    .setInputParams("""
                            {"image_url":"https://example.com/intl.jpg","duration":6}
                            """));

            assertTrue(resp.getSuccess(), resp.getErrorCode() + ": " + resp.getErrorMessage());
            assertEquals("intl", JSONUtil.parseObj(groupBody.get()).getStr("region"));
            JSONObject assetBody = JSONUtil.parseObj(assetUploadBody.get());
            assertEquals("ag-intl", assetBody.getStr("groupId"));
            assertEquals("https://example.com/intl.jpg", assetBody.getStr("url"));

            JSONObject body = JSONUtil.parseObj(generationBody.get());
            assertEquals("asset://asset-intl-001", body.getStr("image_url"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_officialSeedanceSkipsExistingAssetUrls() throws Exception {
        AtomicReference<String> generationBody = new AtomicReference<>();
        HttpServer server = startServer("/openapi/v1/generations", exchange -> {
            generationBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, """
                    {"id":"gen-existing-asset","status":"pending"}
                    """);
        });
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setProviderModel("seedance-2")
                    .setProviderExtraConfig("{\"assetGroupCn\":\"ag-cn\",\"seedanceAssetReviewPollIntervalMillis\":0}")
                    .setGenerateType("VIDEO")
                    .setGenerateMode("IMAGE_TO_VIDEO")
                    .setPrompt("已有 asset 引用")
                    .setInputParams("""
                            {"image_url":"asset://asset-existing"}
                            """));

            assertTrue(resp.getSuccess(), resp.getErrorCode() + ": " + resp.getErrorMessage());
            JSONObject body = JSONUtil.parseObj(generationBody.get());
            assertEquals("asset://asset-existing", body.getStr("image_url"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_officialSeedanceFailsWhenAssetReviewFails() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/openapi/v1/assets", exchange -> sendJson(exchange, 200, """
                {"officialId":"asset-failed","status":"Processing"}
                """));
        server.createContext("/openapi/v1/assets/asset-failed", exchange -> sendJson(exchange, 200, """
                {"officialId":"asset-failed","status":"Failed"}
                """));
        server.createContext("/openapi/v1/generations", exchange -> sendJson(exchange, 200, """
                {"id":"should-not-submit","status":"pending"}
                """));
        server.start();
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setProviderModel("seedance-2-fast")
                    .setProviderExtraConfig("""
                            {"assetGroupCn":"ag-cn","seedanceAssetReviewMaxPolls":1,"seedanceAssetReviewPollIntervalMillis":0}
                            """)
                    .setGenerateType("VIDEO")
                    .setGenerateMode("IMAGE_TO_VIDEO")
                    .setPrompt("审核失败")
                    .setInputParams("""
                            {"image_url":"https://example.com/face.jpg"}
                            """));

            assertFalse(resp.getSuccess());
            assertEquals("ASSET_REVIEW_FAILED", resp.getErrorCode());
            assertTrue(resp.getErrorMessage().contains("Failed"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_nonOfficialSeedanceKeepsDirectImageUrl() throws Exception {
        AtomicReference<String> generationBody = new AtomicReference<>();
        HttpServer server = startServer("/openapi/v1/generations", exchange -> {
            generationBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, """
                    {"id":"gen-beta","status":"pending"}
                    """);
        });
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setProviderModel("seedance-2-beta-face")
                    .setGenerateType("VIDEO")
                    .setGenerateMode("IMAGE_TO_VIDEO")
                    .setPrompt("beta 模型不走素材库")
                    .setInputParams("""
                            {"image_url":"https://example.com/direct.jpg"}
                            """));

            assertTrue(resp.getSuccess(), resp.getErrorCode() + ": " + resp.getErrorMessage());
            JSONObject body = JSONUtil.parseObj(generationBody.get());
            assertEquals("https://example.com/direct.jpg", body.getStr("image_url"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_completedWithResultUrls() throws Exception {
        HttpServer server = startServer("/openapi/v1/generations", exchange -> sendJson(exchange, 200, """
                {"id":"gen-task-sync","status":"success","result_urls":["https://cdn.example.com/result-1.mp4","https://cdn.example.com/result-2.mp4"]}
                """));
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setGenerateType("VIDEO")
                    .setGenerateMode("TEXT_TO_VIDEO")
                    .setPrompt("同步返回结果"));

            assertTrue(resp.getSuccess(), resp.getErrorCode() + ": " + resp.getErrorMessage());
            assertTrue(resp.getFinished());
            JSONArray urls = JSONUtil.parseArray(resp.getOutputUrls());
            assertEquals(2, urls.size());
            assertEquals("https://cdn.example.com/result-1.mp4", urls.getStr(0));
            assertEquals("https://cdn.example.com/result-2.mp4", urls.getStr(1));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testQuery_pendingStatus() throws Exception {
        HttpServer server = startServer("/openapi/v1/generations/gen-task-001", exchange -> sendJson(exchange, 200, """
                {"id":"gen-task-001","status":"processing"}
                """));
        try {
            AigcProviderSubmitRespDTO resp = client.query(baseReq(server).setProviderTaskId("gen-task-001"));

            assertTrue(resp.getSuccess(), resp.getErrorCode() + ": " + resp.getErrorMessage());
            assertEquals("gen-task-001", resp.getProviderTaskId());
            assertEquals("processing", resp.getProviderStatus());
            assertFalse(resp.getFinished());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testQuery_completedWithVideoUrl() throws Exception {
        HttpServer server = startServer("/openapi/v1/generations/gen-task-001", exchange -> sendJson(exchange, 200, """
                {"id":"gen-task-001","status":"success","output":{"video_url":"https://cdn.example.com/result.mp4","duration":5}}
                """));
        try {
            AigcProviderSubmitRespDTO resp = client.query(baseReq(server).setProviderTaskId("gen-task-001"));

            assertTrue(resp.getSuccess(), resp.getErrorCode() + ": " + resp.getErrorMessage());
            assertTrue(resp.getFinished());
            JSONArray urls = JSONUtil.parseArray(resp.getOutputUrls());
            assertEquals(1, urls.size());
            assertEquals("https://cdn.example.com/result.mp4", urls.getStr(0));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testQuery_completedWithTopLevelResultUrl() throws Exception {
        HttpServer server = startServer("/openapi/v1/generations/gen-task-001", exchange -> sendJson(exchange, 200, """
                {"id":"gen-task-001","status":"success","result_url":"https://cdn.example.com/result.mp4"}
                """));
        try {
            AigcProviderSubmitRespDTO resp = client.query(baseReq(server).setProviderTaskId("gen-task-001"));

            assertTrue(resp.getSuccess(), resp.getErrorCode() + ": " + resp.getErrorMessage());
            assertTrue(resp.getFinished());
            JSONArray urls = JSONUtil.parseArray(resp.getOutputUrls());
            assertEquals(1, urls.size());
            assertEquals("https://cdn.example.com/result.mp4", urls.getStr(0));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testQuery_failedStatus() throws Exception {
        HttpServer server = startServer("/openapi/v1/generations/gen-task-bad", exchange -> sendJson(exchange, 200, """
                {"id":"gen-task-bad","status":"failed","error":"内容审核未通过"}
                """));
        try {
            AigcProviderSubmitRespDTO resp = client.query(baseReq(server).setProviderTaskId("gen-task-bad"));

            assertFalse(resp.getSuccess());
            assertTrue(resp.getFinished());
            assertEquals("PROVIDER_FAILED", resp.getErrorCode());
            assertTrue(resp.getErrorMessage().contains("内容审核未通过"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testQuery_failedReturnsReadableErrorMessage() throws Exception {
        HttpServer server = startServer("/openapi/v1/generations/gen-task-face", exchange -> sendJson(exchange, 200, """
                {"id":"gen-task-face","status":"failed","error":{"message":"The uploaded image may contain a real human face. Please replace the image and try again."}}
                """));
        try {
            AigcProviderSubmitRespDTO resp = client.query(baseReq(server).setProviderTaskId("gen-task-face"));

            assertFalse(resp.getSuccess());
            assertTrue(resp.getFinished());
            assertEquals("gen-task-face", resp.getProviderTaskId());
            assertEquals("failed", resp.getProviderStatus());
            assertEquals("The uploaded image may contain a real human face. Please replace the image and try again.",
                    resp.getErrorMessage());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testQuery_requiresContext() {
        AigcProviderSubmitRespDTO resp = client.query("some-task-id");
        assertFalse(resp.getSuccess());
        assertEquals("QUERY_CONTEXT_REQUIRED", resp.getErrorCode());
    }

    @Test
    public void testSubmit_missingBaseUrl() {
        AigcProviderSubmitRespDTO resp = client.submit(new AigcProviderSubmitReqDTO()
                .setProviderBaseUrl("")
                .setProviderApiKey("key"));
        assertFalse(resp.getSuccess());
        assertEquals("CONFIG_INVALID", resp.getErrorCode());
    }

    @Test
    public void testSubmit_httpError() throws Exception {
        HttpServer server = startServer("/openapi/v1/generations", exchange -> sendJson(exchange, 429, """
                {"error":"rate limit exceeded"}
                """));
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setGenerateType("VIDEO")
                    .setGenerateMode("TEXT_TO_VIDEO")
                    .setPrompt("test"));

            assertFalse(resp.getSuccess());
            assertEquals("HTTP_429", resp.getErrorCode());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_http400ParameterRejectedClassified() throws Exception {
        HttpServer server = startServer("/openapi/v1/generations", exchange -> sendJson(exchange, 400, """
                {"error":{"message":"The request parameters were rejected. Please verify the prompt and media URLs, then try again."}}
                """));
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setGenerateType("VIDEO")
                    .setGenerateMode("TEXT_TO_VIDEO")
                    .setPrompt("test"));

            assertFalse(resp.getSuccess());
            assertEquals("MEDIA_URL_INVALID", resp.getErrorCode());
            assertEquals("The request parameters were rejected. Please verify the prompt and media URLs, then try again.",
                    resp.getErrorMessage());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_failedStatusParameterRejectedClassified() throws Exception {
        HttpServer server = startServer("/openapi/v1/generations", exchange -> sendJson(exchange, 200, """
                {"id":"gen-rejected","status":"failed","error_code":"INVALID_PARAMETER","message":"invalid parameter: duration"}
                """));
        try {
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setGenerateType("VIDEO")
                    .setGenerateMode("TEXT_TO_VIDEO")
                    .setPrompt("test"));

            assertFalse(resp.getSuccess());
            assertEquals("PARAM_REJECTED", resp.getErrorCode());
            assertEquals("gen-rejected", resp.getProviderTaskId());
            assertEquals("failed", resp.getProviderStatus());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testVerifyCallback_returnsTrue() {
        assertTrue(client.verifyCallback(null));
    }

    private AigcProviderSubmitReqDTO baseReq(HttpServer server) {
        return new AigcProviderSubmitReqDTO()
                .setProviderBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/openapi/v1")
                .setProviderApiKey("test-api-key")
                .setProviderModel("seedance-2.0")
                .setProviderTimeoutSeconds(60);
    }

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private HttpServer startServer(String path, Handler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> handler.handle(exchange));
        server.start();
        return server;
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

}
