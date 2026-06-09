package cn.iocoder.yudao.module.aigc.gen.framework.client;

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

public class GptImageProviderClientTest {

    @Test
    public void testSubmit_withoutApiKey() {
        GptImageProviderClient client = new GptImageProviderClient();

        AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                .setProviderBaseUrl("https://copse.top/v1/images/generations")
                .setPrompt("测试图片")
                .setGenerateType("IMAGE")
                .setGenerateMode("TEXT_TO_IMAGE"));

        assertFalse(respDTO.getSuccess());
    }

    @Test
    public void testSubmit_editUsesReferenceImages() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/images/edits", exchange -> handleImageEdit(exchange, requestBody));
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            GptImageProviderClient client = new GptImageProviderClient();

            AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                    .setProviderBaseUrl(baseUrl)
                    .setProviderApiKey("test-key")
                    .setProviderModel("gpt-image-2")
                    .setPrompt("{{Image 1}} 改成红色礼服")
                    .setGenerateType("IMAGE")
                    .setGenerateMode("IMAGE_TO_IMAGE")
                    .setInputParams("""
                            {"referenceImages":["data:image/png;base64,aW1hZ2UtYnl0ZXM="]}
                            """));

            assertTrue(respDTO.getSuccess(), respDTO.getErrorCode() + ": " + respDTO.getErrorMessage());
            assertTrue(requestBody.get().contains("name=\"image\""));
            assertTrue(requestBody.get().contains("name=\"prompt\""));
        } finally {
            server.stop(0);
        }
    }

    private void handleImageEdit(HttpExchange exchange, AtomicReference<String> requestBody) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
        byte[] response = "{\"id\":\"image-task-1\",\"data\":[{\"url\":\"https://example.com/result.png\"}]}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
