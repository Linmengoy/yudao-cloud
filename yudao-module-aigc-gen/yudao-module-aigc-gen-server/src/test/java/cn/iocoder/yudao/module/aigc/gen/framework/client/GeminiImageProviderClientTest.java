package cn.iocoder.yudao.module.aigc.gen.framework.client;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeminiImageProviderClientTest {

    @Test
    public void testSubmit_withoutApiKey() {
        GeminiImageProviderClient client = new GeminiImageProviderClient();

        AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                .setProviderBaseUrl("https://example.com/v1beta")
                .setModelCode("gemini-2.5-flash-image")
                .setPrompt("测试图片")
                .setGenerateType("IMAGE")
                .setGenerateMode("TEXT_TO_IMAGE"));

        assertFalse(respDTO.getSuccess());
    }

    @Test
    public void testSubmit_usesReferenceImages() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1beta/models/gemini-2.5-flash-image:generateContent", exchange -> handleGenerateContent(exchange, requestBody));
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta";
            GeminiImageProviderClient client = new GeminiImageProviderClient();

            AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                    .setProviderBaseUrl(baseUrl)
                    .setProviderApiKey("test-key")
                    .setProviderModel("gemini-2.5-flash-image")
                    .setPrompt("{{Image 1}} 改成红色礼服")
                    .setGenerateType("IMAGE")
                    .setGenerateMode("IMAGE_TO_IMAGE")
                    .setInputParams("""
                            {"referenceImages":["data:image/png;base64,aW1hZ2UtYnl0ZXM="]}
                            """));

            assertTrue(respDTO.getSuccess(), respDTO.getErrorCode() + ": " + respDTO.getErrorMessage());
            String body = requestBody.get();
            assertTrue(JSONUtil.parseObj(body)
                    .getJSONArray("contents")
                    .getJSONObject(0)
                    .getJSONArray("parts")
                    .getJSONObject(1)
                    .containsKey("inlineData"));
            assertTrue(body.contains("aW1hZ2UtYnl0ZXM="));
        } finally {
            server.stop(0);
        }
    }

    private void handleGenerateContent(HttpExchange exchange, AtomicReference<String> requestBody) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = "{\"responseId\":\"gemini-task-1\",\"candidates\":[{\"content\":{\"parts\":[{\"inlineData\":{\"mimeType\":\"image/png\",\"data\":\"cmVzdWx0\"}}]}}]}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
