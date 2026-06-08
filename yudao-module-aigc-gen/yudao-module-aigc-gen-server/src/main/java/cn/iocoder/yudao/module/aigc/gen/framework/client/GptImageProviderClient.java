package cn.iocoder.yudao.module.aigc.gen.framework.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitRespDTO;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GptImageProviderClient implements AigcProviderClient {

    @Override
    public String getProviderCode() {
        return "gpt-image-2";
    }

    @Override
    public AigcProviderSubmitRespDTO submit(AigcProviderSubmitReqDTO reqDTO) {
        if (StrUtil.isBlank(reqDTO.getProviderBaseUrl()) || StrUtil.isBlank(reqDTO.getProviderApiKey())) {
            return failed("CONFIG_INVALID", "GPT Image 渠道未配置 API 地址或 API Key");
        }
        long start = System.currentTimeMillis();
        try {
            try (HttpResponse response = isImageEdit(reqDTO) ? submitEdit(reqDTO) : submitGeneration(reqDTO)) {
                if (!response.isOk()) {
                    return failed("HTTP_" + response.getStatus(), safeBody(response.body())).setDurationMillis(System.currentTimeMillis() - start);
                }
                return parseResponse(response.body(), reqDTO).setDurationMillis(System.currentTimeMillis() - start);
            }
        } catch (Exception ex) {
            return failed("REQUEST_EXCEPTION", ex.getMessage()).setDurationMillis(System.currentTimeMillis() - start);
        }
    }

    @Override
    public AigcProviderSubmitRespDTO query(String providerTaskId) {
        return failed("QUERY_UNSUPPORTED", "GPT Image 同步接口不支持任务查询");
    }

    @Override
    public boolean verifyCallback(AigcGenerateCallbackReqDTO reqDTO) {
        return true;
    }

    private JSONObject buildRequestBody(AigcProviderSubmitReqDTO reqDTO) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", StrUtil.blankToDefault(reqDTO.getProviderModel(), reqDTO.getModelCode()));
        body.put("prompt", reqDTO.getPrompt());
        if (StrUtil.isNotBlank(reqDTO.getInputParams()) && JSONUtil.isTypeJSON(reqDTO.getInputParams())) {
            JSONObject params = JSONUtil.parseObj(reqDTO.getInputParams());
            params.forEach((key, value) -> {
                if (value != null && !"inputImages".equals(key) && !"inputImageIds".equals(key) && !"inputImageUrls".equals(key)) {
                    body.put(key, value);
                }
            });
        }
        if ("png".equals(body.get("output_format"))) {
            body.remove("output_compression");
        }
        body.putIfAbsent("n", 1);
        return JSONUtil.parseObj(body);
    }

    private HttpResponse submitGeneration(AigcProviderSubmitReqDTO reqDTO) {
        JSONObject body = buildRequestBody(reqDTO);
        return AigcProviderProxyUtils.execute(HttpRequest.post(resolveEndpoint(reqDTO.getProviderBaseUrl(), false))
                .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                .contentType(ContentType.JSON.getValue())
                .body(body.toString())
                .timeout(timeoutMillis(reqDTO)), reqDTO);
    }

    private HttpResponse submitEdit(AigcProviderSubmitReqDTO reqDTO) throws Exception {
        List<File> tempFiles = new ArrayList<>();
        try {
            JSONObject body = buildRequestBody(reqDTO);
            HttpRequest request = AigcProviderProxyUtils.applyProxy(HttpRequest.post(resolveEndpoint(reqDTO.getProviderBaseUrl(), true))
                    .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                    .timeout(timeoutMillis(reqDTO)), reqDTO);

            body.forEach((key, value) -> {
                if (value != null && !"n".equals(key)) {
                    request.form(key, String.valueOf(value));
                }
            });
            if (body.containsKey("n") && body.getInt("n", 1) > 1) {
                request.form("n", String.valueOf(body.getInt("n", 1)));
            }

            for (ImageInput image : parseInputImages(reqDTO.getInputParams())) {
                File file = createTempImageFile(image, reqDTO);
                tempFiles.add(file);
                request.form("image", file);
            }
            if (tempFiles.isEmpty()) {
                throw new IllegalArgumentException("图生图缺少参考图片");
            }
            return AigcProviderProxyUtils.execute(request, reqDTO);
        } finally {
            for (File file : tempFiles) {
                try {
                    Files.deleteIfExists(file.toPath());
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean isImageEdit(AigcProviderSubmitReqDTO reqDTO) {
        return "IMAGE_TO_IMAGE".equals(reqDTO.getGenerateMode());
    }

    private int timeoutMillis(AigcProviderSubmitReqDTO reqDTO) {
        return (reqDTO.getProviderTimeoutSeconds() == null ? 60 : reqDTO.getProviderTimeoutSeconds()) * 1000;
    }

    private String resolveEndpoint(String baseUrl, boolean edit) {
        String url = StrUtil.removeSuffix(baseUrl, "/");
        String target = edit ? "/images/edits" : "/images/generations";
        if (url.endsWith("/images/generations") || url.endsWith("/images/edits")) {
            return url.substring(0, url.lastIndexOf("/images/")) + target;
        }
        return url + target;
    }

    private List<ImageInput> parseInputImages(String inputParams) {
        if (StrUtil.isBlank(inputParams) || !JSONUtil.isTypeJSON(inputParams)) {
            return List.of();
        }
        JSONObject params = JSONUtil.parseObj(inputParams);
        List<ImageInput> images = new ArrayList<>();
        JSONArray inputImages = params.getJSONArray("inputImages");
        if (inputImages != null) {
            for (Object item : inputImages) {
                JSONObject image = JSONUtil.parseObj(item);
                String source = image.getStr("dataUrl");
                if (StrUtil.isBlank(source)) {
                    source = image.getStr("url");
                }
                if (StrUtil.isNotBlank(source)) {
                    images.add(new ImageInput(source, image.getStr("fileName", "input.png"), image.getStr("mimeType", "image/png")));
                }
            }
        }
        JSONArray inputImageUrls = params.getJSONArray("inputImageUrls");
        if (inputImageUrls != null) {
            for (Object item : inputImageUrls) {
                String url = String.valueOf(item);
                if (StrUtil.isNotBlank(url) && images.stream().noneMatch(image -> image.source().equals(url))) {
                    images.add(new ImageInput(url, "input.png", "image/png"));
                }
            }
        }
        return images;
    }

    private File createTempImageFile(ImageInput image, AigcProviderSubmitReqDTO reqDTO) throws Exception {
        byte[] bytes = readImageBytes(image.source(), reqDTO);
        File file = File.createTempFile("aigc-edit-", normalizeExtension(image));
        Files.write(file.toPath(), bytes);
        return file;
    }

    private byte[] readImageBytes(String source, AigcProviderSubmitReqDTO reqDTO) {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            try (HttpResponse response = AigcProviderProxyUtils.execute(HttpRequest.get(source).timeout(timeoutMillis(reqDTO)), reqDTO)) {
                return response.bodyBytes();
            }
        }
        int commaIndex = source.indexOf(',');
        if (source.startsWith("data:") && commaIndex > 0) {
            String meta = source.substring(0, commaIndex);
            String payload = source.substring(commaIndex + 1);
            if (meta.contains(";base64")) {
                return Base64.getDecoder().decode(payload);
            }
            return URLDecoder.decode(payload, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
        }
        return Base64.getDecoder().decode(source);
    }

    private String normalizeExtension(ImageInput image) {
        String name = image.fileName();
        int dotIndex = name == null ? -1 : name.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < name.length() - 1) {
            return name.substring(dotIndex);
        }
        return switch (image.mimeType()) {
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> ".png";
        };
    }

    private AigcProviderSubmitRespDTO parseResponse(String body, AigcProviderSubmitReqDTO reqDTO) {
        JSONObject json = JSONUtil.parseObj(body);
        JSONArray data = json.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return failed("EMPTY_DATA", "GPT Image 返回结果为空");
        }
        JSONArray urls = new JSONArray();
        for (Object item : data) {
            JSONObject image = JSONUtil.parseObj(item);
            String url = image.getStr("url");
            if (StrUtil.isNotBlank(url)) {
                urls.add(url);
                continue;
            }
            String b64Json = image.getStr("b64_json");
            if (StrUtil.isNotBlank(b64Json)) {
                urls.add("data:" + outputMimeType(reqDTO.getInputParams()) + ";base64," + b64Json);
            }
        }
        if (urls.isEmpty()) {
            return failed("EMPTY_URL", "GPT Image 未返回图片 URL");
        }
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(json.getStr("id", "GPTIMG" + System.currentTimeMillis()))
                .setProviderStatus("SUCCESS")
                .setOutputData(body)
                .setOutputUrls(urls.toString())
                .setFinished(true)
                .setSuccess(true);
    }

    private AigcProviderSubmitRespDTO failed(String code, String message) {
        return new AigcProviderSubmitRespDTO().setSuccess(false).setFinished(true).setErrorCode(code).setErrorMessage(message);
    }

    private String safeBody(String body) {
        if (body == null) {
            return null;
        }
        return body.length() <= 512 ? body : body.substring(0, 512);
    }

    private String outputMimeType(String inputParams) {
        if (StrUtil.isBlank(inputParams) || !JSONUtil.isTypeJSON(inputParams)) {
            return "image/png";
        }
        String format = JSONUtil.parseObj(inputParams).getStr("output_format", "png");
        return switch (format) {
            case "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> "image/png";
        };
    }

    private record ImageInput(String source, String fileName, String mimeType) {
    }
}
