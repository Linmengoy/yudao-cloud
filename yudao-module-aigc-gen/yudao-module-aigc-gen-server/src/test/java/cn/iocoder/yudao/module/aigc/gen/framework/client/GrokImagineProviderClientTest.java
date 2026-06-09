package cn.iocoder.yudao.module.aigc.gen.framework.client;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitRespDTO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GrokImagineProviderClientTest {

    @Test
    public void testSubmit_imageToImageUsesInputImage() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/images/edits", exchange -> handleImageSubmit(exchange, requestBody));
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            String imageUrl = "data:image/png;base64,aW1hZ2UtYnl0ZXM=";
            GrokImagineProviderClient client = new GrokImagineProviderClient();

            AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                    .setProviderBaseUrl(baseUrl)
                    .setProviderApiKey("test-key")
                    .setProviderModel("gpt-image-2")
                    .setPrompt("{{Image 1}} 帮我给她穿上红色的晚礼服")
                    .setGenerateType("IMAGE")
                    .setGenerateMode("IMAGE_TO_IMAGE")
                    .setInputParams("""
                            {"resolution":"1k","inputImages":[{"dataUrl":"%s","fileName":"Image","mimeType":"image/png"}],"inputImageUrls":["%s"]}
                            """.formatted(imageUrl, imageUrl)));

            assertTrue(respDTO.getSuccess(), respDTO.getErrorCode() + ": " + respDTO.getErrorMessage());
            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals("gpt-image-2", body.getStr("model"));
            assertEquals("{{Image 1}} 帮我给她穿上红色的晚礼服", body.getStr("prompt"));
            assertEquals(imageUrl, body.getJSONObject("image").getStr("url"));
            assertEquals(1, body.getInt("n"));
            assertFalse(body.containsKey("inputImages"));
            assertFalse(body.containsKey("inputImageUrls"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_imageToImageUsesMultipleInputImages() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/images/edits", exchange -> handleImageSubmit(exchange, requestBody));
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/images/generations";
            GrokImagineProviderClient client = new GrokImagineProviderClient();

            AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                    .setProviderBaseUrl(baseUrl)
                    .setProviderApiKey("test-key")
                    .setProviderModel("grok-imagine-image")
                    .setPrompt("把三张参考图中的主体合成到同一个画面")
                    .setGenerateType("IMAGE")
                    .setGenerateMode("IMAGE_TO_IMAGE")
                    .setInputParams("""
                            {"inputImages":[{"dataUrl":"data:image/png;base64,aW1hZ2Ux"},{"dataUrl":"data:image/png;base64,aW1hZ2Uy"}],"inputImageUrls":["data:image/png;base64,aW1hZ2Uz","data:image/png;base64,aW1hZ2U0"],"aspect_ratio":"3:2"}
                            """));

            assertTrue(respDTO.getSuccess(), respDTO.getErrorCode() + ": " + respDTO.getErrorMessage());
            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertFalse(body.containsKey("image"));
            assertEquals(3, body.getJSONArray("images").size());
            assertEquals("data:image/png;base64,aW1hZ2Ux", body.getJSONArray("images").getJSONObject(0).getStr("url"));
            assertEquals("data:image/png;base64,aW1hZ2Uy", body.getJSONArray("images").getJSONObject(1).getStr("url"));
            assertEquals("data:image/png;base64,aW1hZ2Uz", body.getJSONArray("images").getJSONObject(2).getStr("url"));
            assertEquals("3:2", body.getStr("aspect_ratio"));
            assertFalse(body.containsKey("inputImages"));
            assertFalse(body.containsKey("inputImageUrls"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_imageToImageCompressesLargeInputImage() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        byte[] largeImage = createLargeImageBytes(1600, 2200);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/images/edits", exchange -> handleImageSubmit(exchange, requestBody));
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            String imageUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(largeImage);
            GrokImagineProviderClient client = new GrokImagineProviderClient();

            AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                    .setProviderBaseUrl(baseUrl)
                    .setProviderApiKey("test-key")
                    .setProviderModel("grok-imagine-image")
                    .setPrompt("{{Image 1}} 改成红色礼服")
                    .setGenerateType("IMAGE")
                    .setGenerateMode("IMAGE_TO_IMAGE")
                    .setInputParams("""
                            {"inputImageUrls":["%s"]}
                            """.formatted(imageUrl)));

            assertTrue(respDTO.getSuccess(), respDTO.getErrorCode() + ": " + respDTO.getErrorMessage());
            String providerImage = JSONUtil.parseObj(requestBody.get()).getJSONObject("image").getStr("url");
            assertTrue(providerImage.startsWith("data:image/jpeg;base64,"));
            assertTrue(providerImage.getBytes(StandardCharsets.UTF_8).length <= 960 * 1024);
        } finally {
            server.stop(0);
        }
    }

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

    @Test
    public void testSubmit_imageToVideoInfersPortraitSizeFromReferenceImage() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/videos", exchange -> handleVideoSubmit(exchange, requestBody));
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            String imageUrl = createDataImage(90, 160);
            GrokImagineProviderClient client = new GrokImagineProviderClient();

            AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                    .setProviderBaseUrl(baseUrl)
                    .setProviderApiKey("test-key")
                    .setProviderModel("grok-imagine-video-1.5-preview")
                    .setPrompt("图中的人物在四处观望")
                    .setGenerateType("VIDEO")
                    .setGenerateMode("IMAGE_TO_VIDEO")
                    .setInputParams("""
                            {"duration":"5","resolution":"480p","providerModel":"grok-imagine-video-1.5-preview","referenceImages":["%s"]}
                            """.formatted(imageUrl)));

            assertTrue(respDTO.getSuccess(), respDTO.getErrorCode() + ": " + respDTO.getErrorMessage());
            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals("1024x1792", body.getStr("size"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_imageToVideoUsesSnakeCaseAspectRatio() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/videos", exchange -> handleVideoSubmit(exchange, requestBody));
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            GrokImagineProviderClient client = new GrokImagineProviderClient();

            AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                    .setProviderBaseUrl(baseUrl)
                    .setProviderApiKey("test-key")
                    .setProviderModel("grok-imagine-video-1.5-preview")
                    .setPrompt("图片中的人物在左右观望")
                    .setGenerateType("VIDEO")
                    .setGenerateMode("IMAGE_TO_VIDEO")
                    .setInputParams("""
                            {"aspect_ratio":"9:16","duration":"5","resolution":"480p","providerModel":"grok-imagine-video-1.5-preview","referenceImages":["data:image/jpeg;base64,aW1hZ2UtYnl0ZXM="]}
                            """));

            assertTrue(respDTO.getSuccess(), respDTO.getErrorCode() + ": " + respDTO.getErrorMessage());
            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals("1024x1792", body.getStr("size"));
            assertFalse(body.containsKey("aspect_ratio"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSubmit_videoMapsAspectRatiosToGrokAllowedSizes() throws Exception {
        assertVideoSize("1:1", "1024x1024");
        assertVideoSize("16:9", "1280x720");
        assertVideoSize("9:16", "1024x1792");
        assertVideoSize("4:3", "1792x1024");
        assertVideoSize("3:4", "1024x1792");
        assertVideoSize("3:2", "1792x1024");
        assertVideoSize("2:3", "1024x1792");
    }

    @Test
    public void testSubmit_videoNormalizesUnsupportedSizeToGrokAllowedSize() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/videos", exchange -> handleVideoSubmit(exchange, requestBody));
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            GrokImagineProviderClient client = new GrokImagineProviderClient();

            AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                    .setProviderBaseUrl(baseUrl)
                    .setProviderApiKey("test-key")
                    .setProviderModel("grok-imagine-video-1.5-preview")
                    .setPrompt("海边散步")
                    .setGenerateType("VIDEO")
                    .setGenerateMode("TEXT_TO_VIDEO")
                    .setInputParams("""
                            {"size":"720*1280","duration":"5","providerModel":"grok-imagine-video-1.5-preview"}
                            """));

            assertTrue(respDTO.getSuccess(), respDTO.getErrorCode() + ": " + respDTO.getErrorMessage());
            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals("1024x1792", body.getStr("size"));
        } finally {
            server.stop(0);
        }
    }

    private void assertVideoSize(String aspectRatio, String expectedSize) throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/videos", exchange -> handleVideoSubmit(exchange, requestBody));
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            GrokImagineProviderClient client = new GrokImagineProviderClient();

            AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                    .setProviderBaseUrl(baseUrl)
                    .setProviderApiKey("test-key")
                    .setProviderModel("grok-imagine-video-1.5-preview")
                    .setPrompt("海边散步")
                    .setGenerateType("VIDEO")
                    .setGenerateMode("TEXT_TO_VIDEO")
                    .setInputParams("""
                            {"aspectRatio":"%s","duration":"5","resolution":"1080p","providerModel":"grok-imagine-video-1.5-preview"}
                            """.formatted(aspectRatio)));

            assertTrue(respDTO.getSuccess(), respDTO.getErrorCode() + ": " + respDTO.getErrorMessage());
            JSONObject body = JSONUtil.parseObj(requestBody.get());
            assertEquals(expectedSize, body.getStr("size"));
            assertFalse(body.containsKey("aspectRatio"));
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

    private void handleImageSubmit(HttpExchange exchange, AtomicReference<String> requestBody) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = "{\"id\":\"image-task-1\",\"data\":[{\"url\":\"https://example.com/result.png\"}]}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private String createDataImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, Color.WHITE.getRGB());
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private byte[] createLargeImageBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int red = (x * 31 + y * 7) & 0xFF;
                int green = (x * 13 + y * 17) & 0xFF;
                int blue = (x * 5 + y * 29) & 0xFF;
                image.setRGB(x, y, new Color(red, green, blue).getRGB());
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        return outputStream.toByteArray();
    }

}
