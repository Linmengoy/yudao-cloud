package cn.iocoder.yudao.module.aigc.gen.framework.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitRespDTO;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class GeminiImageProviderClient implements AigcProviderClient {

    private static final String PROVIDER_CODE = "gemini-image";

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public AigcProviderSubmitRespDTO submit(AigcProviderSubmitReqDTO reqDTO) {
        if (StrUtil.isBlank(reqDTO.getProviderBaseUrl()) || StrUtil.isBlank(reqDTO.getProviderApiKey())) {
            return failed("CONFIG_INVALID", "Gemini 图片渠道未配置 API 地址或 API Key");
        }
        long start = System.currentTimeMillis();
        try (HttpResponse response = AigcProviderProxyUtils.applyProxy(HttpRequest.post(resolveEndpoint(reqDTO))
                .contentType(ContentType.JSON.getValue())
                .body(buildRequestBody(reqDTO).toString())
                .timeout(timeoutMillis(reqDTO)), reqDTO)
                .execute()) {
            if (!response.isOk()) {
                return failed("HTTP_" + response.getStatus(), safeBody(response.body())).setDurationMillis(System.currentTimeMillis() - start);
            }
            return parseResponse(response.body(), reqDTO).setDurationMillis(System.currentTimeMillis() - start);
        } catch (Exception ex) {
            return failed("REQUEST_EXCEPTION", ex.getMessage()).setDurationMillis(System.currentTimeMillis() - start);
        }
    }

    @Override
    public AigcProviderSubmitRespDTO query(String providerTaskId) {
        return failed("QUERY_UNSUPPORTED", "Gemini 图片同步接口不支持任务查询");
    }

    @Override
    public boolean verifyCallback(AigcGenerateCallbackReqDTO reqDTO) {
        return true;
    }

    private JSONObject buildRequestBody(AigcProviderSubmitReqDTO reqDTO) {
        JSONObject body = new JSONObject();
        JSONArray parts = new JSONArray();
        parts.add(JSONUtil.createObj().set("text", StrUtil.blankToDefault(reqDTO.getPrompt(), "")));
        for (ImageInput image : parseInputImages(reqDTO)) {
            parts.add(JSONUtil.createObj().set("inlineData", JSONUtil.createObj()
                    .set("mimeType", image.mimeType())
                    .set("data", image.base64Data())));
        }
        body.set("contents", new JSONArray().put(JSONUtil.createObj().set("parts", parts)));

        JSONObject generationConfig = JSONUtil.createObj().set("responseModalities", new JSONArray().put("TEXT").put("IMAGE"));
        JSONObject imageConfig = buildImageConfig(reqDTO);
        if (!imageConfig.isEmpty()) {
            generationConfig.set("imageConfig", imageConfig);
        }
        body.set("generationConfig", generationConfig);
        return body;
    }

    private JSONObject buildImageConfig(AigcProviderSubmitReqDTO reqDTO) {
        JSONObject config = new JSONObject();
        JSONObject params = parseParams(reqDTO.getInputParams());
        String aspectRatio = firstNonBlank(params.getStr("aspectRatio"), params.getStr("ratio"), sizeToAspectRatio(params.getStr("size")));
        if (StrUtil.isNotBlank(aspectRatio)) {
            config.set("aspectRatio", aspectRatio);
        }
        String imageSize = firstNonBlank(params.getStr("imageSize"), params.getStr("resolution"));
        if (StrUtil.isNotBlank(imageSize) && supportsImageSize(resolveModelCode(reqDTO))) {
            config.set("imageSize", normalizeImageSize(imageSize));
        }
        return config;
    }

    private String resolveEndpoint(AigcProviderSubmitReqDTO reqDTO) {
        String baseUrl = StrUtil.removeSuffix(reqDTO.getProviderBaseUrl(), "/");
        String modelCode = resolveModelCode(reqDTO);
        String endpoint = "/models/" + modelCode + ":generateContent";
        String url = baseUrl.endsWith(":generateContent") ? baseUrl : baseUrl + endpoint;
        return url + (url.contains("?") ? "&" : "?") + "key=" + reqDTO.getProviderApiKey();
    }

    private String resolveModelCode(AigcProviderSubmitReqDTO reqDTO) {
        String mapped = resolveMappedModelCode(reqDTO);
        return firstNonBlank(mapped, reqDTO.getProviderModel(), reqDTO.getModelCode());
    }

    private String resolveMappedModelCode(AigcProviderSubmitReqDTO reqDTO) {
        if (StrUtil.isBlank(reqDTO.getProviderExtraConfig()) || !JSONUtil.isTypeJSON(reqDTO.getProviderExtraConfig())) {
            return null;
        }
        JSONObject extra = JSONUtil.parseObj(reqDTO.getProviderExtraConfig());
        JSONObject mapping = extra.getJSONObject("modelMapping");
        if (mapping == null) {
            return extra.getStr("model");
        }
        return mapping.getStr(reqDTO.getModelCode());
    }

    private AigcProviderSubmitRespDTO parseResponse(String body, AigcProviderSubmitReqDTO reqDTO) {
        JSONObject json = JSONUtil.parseObj(body);
        JSONArray candidates = json.getJSONArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return failed("EMPTY_CANDIDATES", "Gemini 图片返回结果为空");
        }
        JSONArray urls = new JSONArray();
        StringBuilder text = new StringBuilder();
        for (Object candidateItem : candidates) {
            JSONObject candidate = JSONUtil.parseObj(candidateItem);
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content == null ? null : content.getJSONArray("parts");
            if (parts == null) {
                continue;
            }
            for (Object partItem : parts) {
                JSONObject part = JSONUtil.parseObj(partItem);
                if (StrUtil.isNotBlank(part.getStr("text"))) {
                    text.append(part.getStr("text"));
                }
                JSONObject inlineData = part.getJSONObject("inlineData");
                if (inlineData != null && StrUtil.isNotBlank(inlineData.getStr("data"))) {
                    String mimeType = inlineData.getStr("mimeType", outputMimeType(reqDTO.getInputParams()));
                    urls.add("data:" + mimeType + ";base64," + inlineData.getStr("data"));
                }
            }
        }
        if (urls.isEmpty()) {
            return failed("EMPTY_IMAGE", "Gemini 未返回图片 inlineData");
        }
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(json.getStr("responseId", "GEMINI" + System.currentTimeMillis()))
                .setProviderStatus("SUCCESS")
                .setOutputText(text.isEmpty() ? null : text.toString())
                .setOutputData(body)
                .setOutputUrls(urls.toString())
                .setFinished(true)
                .setSuccess(true);
    }

    private JSONObject parseParams(String inputParams) {
        if (StrUtil.isBlank(inputParams) || !JSONUtil.isTypeJSON(inputParams)) {
            return new JSONObject();
        }
        return JSONUtil.parseObj(inputParams);
    }

    private JSONArray parseArray(JSONObject params, String key) {
        JSONArray array = params.getJSONArray(key);
        return array == null ? new JSONArray() : array;
    }

    private java.util.List<ImageInput> parseInputImages(AigcProviderSubmitReqDTO reqDTO) {
        JSONObject params = parseParams(reqDTO.getInputParams());
        java.util.List<ImageInput> images = new java.util.ArrayList<>();
        for (Object item : parseArray(params, "inputImages")) {
            JSONObject image = JSONUtil.parseObj(item);
            String source = firstNonBlank(image.getStr("dataUrl"), image.getStr("url"));
            if (StrUtil.isNotBlank(source)) {
                images.add(new ImageInput(toBase64(source, reqDTO), image.getStr("mimeType", "image/png")));
            }
        }
        for (Object item : parseArray(params, "inputImageUrls")) {
            String source = String.valueOf(item);
            if (StrUtil.isNotBlank(source)) {
                images.add(new ImageInput(toBase64(source, reqDTO), "image/png"));
            }
        }
        return images;
    }

    private String toBase64(String source, AigcProviderSubmitReqDTO reqDTO) {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            try (HttpResponse response = AigcProviderProxyUtils.applyProxy(HttpRequest.get(source).timeout(timeoutMillis(reqDTO)), reqDTO).execute()) {
                return Base64.getEncoder().encodeToString(response.bodyBytes());
            }
        }
        int commaIndex = source.indexOf(',');
        if (source.startsWith("data:") && commaIndex > 0) {
            String meta = source.substring(0, commaIndex);
            String payload = source.substring(commaIndex + 1);
            if (meta.contains(";base64")) {
                return payload;
            }
            return Base64.getEncoder().encodeToString(URLDecoder.decode(payload, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8));
        }
        return source;
    }

    private boolean supportsImageSize(String modelCode) {
        return modelCode != null && (modelCode.contains("3.1-flash-image") || modelCode.contains("3-pro-image"));
    }

    private String normalizeImageSize(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("512".equals(normalized) || "512P".equals(normalized) || "0.5K".equals(normalized)) {
            return "512";
        }
        if ("1024".equals(normalized) || "1K".equals(normalized)) {
            return "1K";
        }
        if ("2048".equals(normalized) || "2K".equals(normalized)) {
            return "2K";
        }
        if ("4096".equals(normalized) || "4K".equals(normalized)) {
            return "4K";
        }
        return value;
    }

    private String sizeToAspectRatio(String size) {
        if (StrUtil.isBlank(size) || "auto".equals(size)) {
            return null;
        }
        String[] parts = size.toLowerCase().split("x");
        if (parts.length != 2) {
            return null;
        }
        try {
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            int divisor = gcd(width, height);
            return (width / divisor) + ":" + (height / divisor);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return Math.abs(a);
    }

    private String outputMimeType(String inputParams) {
        String format = parseParams(inputParams).getStr("output_format", "png");
        return switch (format) {
            case "jpeg", "jpg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> "image/png";
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private int timeoutMillis(AigcProviderSubmitReqDTO reqDTO) {
        return (reqDTO.getProviderTimeoutSeconds() == null ? 120 : reqDTO.getProviderTimeoutSeconds()) * 1000;
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

    private record ImageInput(String base64Data, String mimeType) {
    }

}
