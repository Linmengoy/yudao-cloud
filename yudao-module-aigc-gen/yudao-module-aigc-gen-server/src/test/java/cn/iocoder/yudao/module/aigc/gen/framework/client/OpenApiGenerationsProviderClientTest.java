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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenApiGenerationsProviderClientTest {

    private final OpenApiGenerationsProviderClient client = new OpenApiGenerationsProviderClient();

    @Test
    public void testGetProviderCode() {
        assertEquals("openapi-generations", client.getProviderCode());
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
    public void testSubmit_completedWithResultUrls() throws Exception {
        HttpServer server = startServer("/openapi/v1/generations", exchange -> {
            sendJson(exchange, 200, """
                    {"id":"gen-task-sync","status":"success","result_urls":["https://cdn.example.com/result-1.mp4","https://cdn.example.com/result-2.mp4"]}
                    """);
        });
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
        HttpServer server = startServer("/openapi/v1/generations/gen-task-001", exchange -> {
            sendJson(exchange, 200, """
                    {"id":"gen-task-001","status":"processing"}
                    """);
        });
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
        HttpServer server = startServer("/openapi/v1/generations/gen-task-001", exchange -> {
            sendJson(exchange, 200, """
                    {"id":"gen-task-001","status":"success","output":{"video_url":"https://cdn.example.com/result.mp4","duration":5}}
                    """);
        });
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
        HttpServer server = startServer("/openapi/v1/generations/gen-task-001", exchange -> {
            sendJson(exchange, 200, """
                    {"id":"gen-task-001","status":"success","result_url":"https://cdn.example.com/result.mp4"}
                    """);
        });
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
        HttpServer server = startServer("/openapi/v1/generations/gen-task-bad", exchange -> {
            sendJson(exchange, 200, """
                    {"id":"gen-task-bad","status":"failed","error":"内容审核未通过"}
                    """);
        });
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
        HttpServer server = startServer("/openapi/v1/generations", exchange -> {
            sendJson(exchange, 429, """
                    {"error":"rate limit exceeded"}
                    """);
        });
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
    public void testVerifyCallback_returnsTrue() {
        assertTrue(client.verifyCallback(null));
    }

    // ========== helpers ==========

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
