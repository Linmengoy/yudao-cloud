package cn.iocoder.yudao.module.aigc.gen.framework.client;

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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenApiGenerationsProviderClientTest {

    @Test
    public void testFactoryUsesClientTypeFromExtraConfig() {
        OpenApiGenerationsProviderClient openApiClient = new OpenApiGenerationsProviderClient();
        MockAigcProviderClient mockClient = new MockAigcProviderClient();
        AigcProviderClientFactory factory = new AigcProviderClientFactory(List.of(openApiClient, mockClient));

        AigcProviderClient client = factory.getClient(new AigcProviderSubmitReqDTO()
                .setProviderCode("any-provider")
                .setProviderExtraConfig("{\"clientType\":\"OPENAPI_GENERATIONS\"}"));

        assertSame(openApiClient, client);
    }

    @Test
    public void testSubmit_textToVideo() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> handleSubmit(exchange, requestBody, "{\"id\":\"task-1\",\"status\":\"queued\"}"));
        try {
            OpenApiGenerationsProviderClient client = new OpenApiGenerationsProviderClient();
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setProviderModel("seedance-2")
                    .setGenerateMode("TEXT_TO_VIDEO")
                    .setInputParams("{\"ratio\":\"16:9\",\"duration\":5,\"resolution\":\"720p\",\"generate_audio\":true}"));

            assertTrue(resp.getSuccess(), resp.getErrorMessage());
            assertFalse(resp.getFinished());
            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals("seedance-2", body.getStr("model"));
            assertEquals("text_to_video", body.getStr("mode"));
            assertEquals("16:9", body.getStr("ratio"));
            assertEquals(5, body.getInt("duration"));
            assertEquals("720p", body.getStr("resolution"));
            assertEquals(Boolean.TRUE, body.getBool("generate_audio"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_processingKeepsProviderTaskId() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> handleSubmit(exchange, requestBody, "{\"id\":\"gen-123\",\"status\":\"processing\"}"));
        try {
            OpenApiGenerationsProviderClient client = new OpenApiGenerationsProviderClient();
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setProviderModel("seedance-2")
                    .setGenerateMode("TEXT_TO_VIDEO"));

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
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> handleSubmit(exchange, requestBody, "{\"status\":\"processing\"}"));
        try {
            OpenApiGenerationsProviderClient client = new OpenApiGenerationsProviderClient();
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setProviderModel("seedance-2")
                    .setGenerateMode("TEXT_TO_VIDEO"));

            assertFalse(resp.getSuccess());
            assertTrue(resp.getFinished());
            assertEquals("MISSING_PROVIDER_TASK_ID", resp.getErrorCode());
            assertEquals("processing", resp.getProviderStatus());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_imageToVideoMapsReferenceImage() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> handleSubmit(exchange, requestBody, "{\"id\":\"task-2\",\"status\":\"queued\"}"));
        try {
            OpenApiGenerationsProviderClient client = new OpenApiGenerationsProviderClient();
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setProviderModel("seedance-2")
                    .setGenerateMode("IMAGE_TO_VIDEO")
                    .setInputParams("""
                            {"referenceImages":["https://example.com/a.png"],"referenceImageIds":["n1"],"duration":5}
                            """));

            assertTrue(resp.getSuccess(), resp.getErrorMessage());
            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals("image_to_video", body.getStr("mode"));
            assertEquals("https://example.com/a.png", body.getStr("image_url"));
            assertFalse(body.containsKey("referenceImages"));
            assertFalse(body.containsKey("referenceImageIds"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_firstLastFrameMapsTwoImages() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> handleSubmit(exchange, requestBody, "{\"id\":\"task-3\",\"status\":\"queued\"}"));
        try {
            OpenApiGenerationsProviderClient client = new OpenApiGenerationsProviderClient();
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setProviderModel("seedance-2")
                    .setGenerateMode("FIRST_LAST_FRAME_VIDEO")
                    .setInputParams("""
                            {"referenceImages":["https://example.com/first.png","https://example.com/last.png"],"duration":6}
                            """));

            assertTrue(resp.getSuccess(), resp.getErrorMessage());
            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals("first_last_frame", body.getStr("mode"));
            assertEquals(2, body.getJSONArray("image_urls").size());
            assertEquals("https://example.com/first.png", body.getJSONArray("image_urls").getStr(0));
            assertFalse(body.containsKey("referenceImages"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_multiRefMapsMediaArrays() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startServer(exchange -> handleSubmit(exchange, requestBody, "{\"id\":\"task-4\",\"status\":\"queued\"}"));
        try {
            OpenApiGenerationsProviderClient client = new OpenApiGenerationsProviderClient();
            AigcProviderSubmitRespDTO resp = client.submit(baseReq(server)
                    .setProviderModel("seedance-2")
                    .setGenerateMode("MULTI_REF_VIDEO")
                    .setInputParams("""
                            {"referenceImages":["https://example.com/a.png"],"referenceVideos":["https://example.com/v.mp4"],"referenceAudios":["https://example.com/a.mp3"],"ratio":"9:16"}
                            """));

            assertTrue(resp.getSuccess(), resp.getErrorMessage());
            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals("multi_ref", body.getStr("mode"));
            assertEquals("https://example.com/a.png", body.getJSONArray("image_urls").getStr(0));
            assertEquals("https://example.com/v.mp4", body.getJSONArray("video_urls").getStr(0));
            assertEquals("https://example.com/a.mp3", body.getJSONArray("audio_urls").getStr(0));
            assertFalse(body.containsKey("referenceImages"));
            assertFalse(body.containsKey("referenceVideos"));
            assertFalse(body.containsKey("referenceAudios"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testQuery_succeededReturnsOutputUrls() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startQueryServer("task-5", exchange -> handleSubmit(exchange, requestBody, """
                {"id":"task-5","status":"succeeded","result_url":"https://cdn.example.com/out.mp4"}
                """));
        try {
            OpenApiGenerationsProviderClient client = new OpenApiGenerationsProviderClient();
            AigcProviderSubmitRespDTO resp = client.query(baseReq(server).setProviderTaskId("task-5"));

            assertTrue(resp.getSuccess(), resp.getErrorMessage());
            assertTrue(resp.getFinished());
            assertEquals("succeeded", resp.getProviderStatus());
            assertEquals("[\"https://cdn.example.com/out.mp4\"]", resp.getOutputUrls());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testQuery_failedReturnsReadableErrorMessage() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = startQueryServer("task-6", exchange -> handleSubmit(exchange, requestBody, """
                {"id":"task-6","status":"failed","error":{"message":"The uploaded image may contain a real human face. Please replace the image and try again."}}
                """));
        try {
            OpenApiGenerationsProviderClient client = new OpenApiGenerationsProviderClient();
            AigcProviderSubmitRespDTO resp = client.query(baseReq(server).setProviderTaskId("task-6"));

            assertFalse(resp.getSuccess());
            assertTrue(resp.getFinished());
            assertEquals("task-6", resp.getProviderTaskId());
            assertEquals("failed", resp.getProviderStatus());
            assertEquals("The uploaded image may contain a real human face. Please replace the image and try again.",
                    resp.getErrorMessage());
        } finally {
            server.stop(0);
        }
    }

    private AigcProviderSubmitReqDTO baseReq(HttpServer server) {
        return new AigcProviderSubmitReqDTO()
                .setProviderBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/openapi/v1")
                .setProviderApiKey("test-key")
                .setModelCode("seedance-2")
                .setGenerateType("VIDEO")
                .setPrompt("A cinematic scene");
    }

    private HttpServer startServer(ThrowingHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/openapi/v1/generations", handler::handle);
        server.start();
        return server;
    }

    private HttpServer startQueryServer(String providerTaskId, ThrowingHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/openapi/v1/generations/" + providerTaskId, handler::handle);
        server.start();
        return server;
    }

    private void handleSubmit(HttpExchange exchange, AtomicReference<String> requestBody, String responseJson) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    @FunctionalInterface
    private interface ThrowingHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

}
