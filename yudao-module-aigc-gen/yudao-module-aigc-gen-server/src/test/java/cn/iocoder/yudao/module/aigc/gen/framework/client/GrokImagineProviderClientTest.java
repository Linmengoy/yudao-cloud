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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GrokImagineProviderClientTest {

    @Test
    public void testSubmit_imageToVideoUsesReferenceImage() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/videos", exchange -> handleVideoSubmit(exchange, requestBody));
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            String imageUrl = "data:image/jpeg;base64,aW1hZ2UtYnl0ZXM=";
            GrokImagineProviderClient client = new GrokImagineProviderClient();

            AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                    .setProviderBaseUrl(baseUrl)
                    .setProviderApiKey("test-key")
                    .setProviderModel("grok-imagine-video-1.5-preview")
                    .setPrompt("Angelina jolie 在海边散步")
                    .setGenerateType("VIDEO")
                    .setGenerateMode("IMAGE_TO_VIDEO")
                    .setInputParams("""
                            {"duration":"5","resolution":"480p","ratio":"16:9","providerModel":"grok-imagine-video-1.5-preview","referenceImageIds":["draft_1"],"referenceImages":["%s"]}
                            """.formatted(imageUrl)));

            assertTrue(respDTO.getSuccess(), respDTO.getErrorCode() + ": " + respDTO.getErrorMessage());
            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals("grok-imagine-video-1.5-preview", body.getStr("model"));
            assertEquals("Angelina jolie 在海边散步", body.getStr("prompt"));
            assertFalse(body.containsKey("image_url"));
            assertEquals("6", body.getStr("seconds"));
            assertEquals("1280x720", body.getStr("size"));
            assertEquals(2, body.getJSONArray("images").size());
            assertTrue(body.getJSONArray("images").getStr(0).startsWith("data:image/jpeg;base64,"));
            assertEquals(body.getJSONArray("images").getStr(0), body.getJSONArray("images").getStr(1));
            assertFalse(body.containsKey("providerModel"));
            assertFalse(body.containsKey("referenceImages"));
            assertFalse(body.containsKey("referenceImageIds"));
            assertFalse(body.containsKey("duration"));
            assertFalse(body.containsKey("resolution"));
            assertFalse(body.containsKey("ratio"));
        } finally {
            server.stop(0);
        }
    }

    private void handleVideoSubmit(HttpExchange exchange, AtomicReference<String> requestBody) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = "{\"id\":\"video-task-1\",\"status\":\"queued\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

}
