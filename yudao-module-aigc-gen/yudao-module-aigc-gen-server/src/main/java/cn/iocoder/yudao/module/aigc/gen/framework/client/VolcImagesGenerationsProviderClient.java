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

import java.util.Map;

@Component
public class VolcImagesGenerationsProviderClient implements AigcProviderClient {

    public static final String CLIENT_TYPE = "VOLC_IMAGES_GENERATIONS";
    private static final String PROVIDER_CODE = "volc-images-generations";

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public String getClientType() {
        return CLIENT_TYPE;
    }

    @Override
    public AigcProviderSubmitRespDTO submit(AigcProviderSubmitReqDTO reqDTO) {
        if (StrUtil.isBlank(reqDTO.getProviderBaseUrl()) || StrUtil.isBlank(reqDTO.getProviderApiKey())) {
            return failed("CONFIG_INVALID", "豆包图片生成渠道未配置 API 地址或 API Key");
        }
        long start = System.currentTimeMillis();
        try (HttpResponse response = AigcProviderProxyUtils.execute(HttpRequest.post(resolveEndpoint(reqDTO))
                .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                .contentType(ContentType.JSON.getValue())
                .body(buildRequestBody(reqDTO).toString())
                .timeout(timeoutMillis(reqDTO)), reqDTO)) {
            if (!response.isOk()) {
                return failed("HTTP_" + response.getStatus(), safeBody(response.body()))
                        .setDurationMillis(System.currentTimeMillis() - start);
            }
            return parseResponse(response.body()).setDurationMillis(System.currentTimeMillis() - start);
        } catch (Exception ex) {
            return failed("REQUEST_EXCEPTION", ex.getMessage()).setDurationMillis(System.currentTimeMillis() - start);
        }
    }

    @Override
    public AigcProviderSubmitRespDTO query(String providerTaskId) {
        return failed("QUERY_UNSUPPORTED", "豆包图片生成按同步响应处理");
    }

    @Override
    public boolean verifyCallback(AigcGenerateCallbackReqDTO reqDTO) {
        return true;
    }

    private JSONObject buildRequestBody(AigcProviderSubmitReqDTO reqDTO) {
        JSONObject body = JSONUtil.createObj()
                .set("model", StrUtil.blankToDefault(reqDTO.getProviderModel(), reqDTO.getModelCode()))
                .set("prompt", StrUtil.blankToDefault(reqDTO.getPrompt(), ""));
        JSONObject params = parseParams(reqDTO.getInputParams());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() != null && !isInternalParam(entry.getKey())) {
                body.set(entry.getKey(), normalizeParamValue(entry.getKey(), entry.getValue()));
            }
        }
        applyReferenceImages(body, params);
        applyImageCount(body, params);
        body.set("size", StrUtil.blankToDefault(body.getStr("size"), "2048x2048"));
        body.set("response_format", StrUtil.blankToDefault(body.getStr("response_format"), "url"));
        return body;
    }

    private void applyReferenceImages(JSONObject body, JSONObject params) {
        JSONArray images = firstNonEmptyArray(params, "image", "referenceImages", "inputImageUrls", "image_urls");
        if (images != null) {
            body.set("image", images.size() == 1 ? images.get(0) : images);
            return;
        }
        String image = firstNonBlank(params.getStr("image"), params.getStr("image_url"));
        if (StrUtil.isNotBlank(image)) {
            body.set("image", image);
        }
    }

    private void applyImageCount(JSONObject body, JSONObject params) {
        Object count = firstNonNull(params.get("n"), params.get("max_images"));
        if (count != null) {
            body.set("n", normalizeInteger(count));
        } else if (!body.containsKey("n")) {
            body.set("n", 1);
        }
        body.remove("max_images");
    }

    private Object normalizeParamValue(String key, Object value) {
        if ("watermark".equals(key) || "stream".equals(key)) {
            return normalizeBoolean(value);
        }
        if ("n".equals(key) || "max_images".equals(key) || "output_compression".equals(key)) {
            return normalizeInteger(value);
        }
        return value;
    }

    private Boolean normalizeBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return Boolean.TRUE.equals(value);
    }

    private Object normalizeInteger(Object value) {
        if (value instanceof Number) {
            return value;
        }
        if (value instanceof String string && StrUtil.isNotBlank(string)) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                return value;
            }
        }
        return value;
    }

    private AigcProviderSubmitRespDTO parseResponse(String body) {
        JSONObject json = JSONUtil.parseObj(body);
        JSONObject error = json.getJSONObject("error");
        if (error != null) {
            return failed(firstNonBlank(error.getStr("code"), "PROVIDER_ERROR"), firstNonBlank(error.getStr("message"), body));
        }
        JSONArray urls = extractUrls(json);
        if (urls.isEmpty()) {
            return failed("EMPTY_IMAGE", "豆包图片生成未返回图片地址").setOutputData(body);
        }
        JSONObject usage = json.getJSONObject("usage");
        AigcProviderSubmitRespDTO resp = new AigcProviderSubmitRespDTO()
                .setProviderTaskId(firstNonBlank(json.getStr("id"), json.getStr("request_id"), "VOLCIMG" + System.currentTimeMillis()))
                .setProviderStatus("SUCCESS")
                .setOutputData(body)
                .setOutputUrls(urls.toString())
                .setFinished(true)
                .setSuccess(true);
        if (usage != null) {
            resp.setPromptTokens(usage.getLong("input_tokens"))
                    .setCompletionTokens(usage.getLong("output_tokens"))
                    .setTotalTokens(usage.getLong("total_tokens"));
        }
        return resp;
    }

    private JSONArray extractUrls(JSONObject json) {
        JSONArray result = new JSONArray();
        JSONArray data = json.getJSONArray("data");
        if (data != null) {
            for (Object item : data) {
                JSONObject image = JSONUtil.parseObj(item);
                addIfPresent(result, image.getStr("url"));
                String b64 = image.getStr("b64_json");
                if (StrUtil.isNotBlank(b64)) {
                    result.add("data:image/jpeg;base64," + b64);
                }
            }
        }
        return result;
    }

    private String resolveEndpoint(AigcProviderSubmitReqDTO reqDTO) {
        String base = StrUtil.removeSuffix(reqDTO.getProviderBaseUrl(), "/");
        return base.endsWith("/images/generations") ? base : base + "/images/generations";
    }

    private JSONObject parseParams(String inputParams) {
        if (StrUtil.isBlank(inputParams) || !JSONUtil.isTypeJSON(inputParams)) {
            return JSONUtil.createObj();
        }
        return JSONUtil.parseObj(inputParams);
    }

    private JSONArray firstNonEmptyArray(JSONObject params, String... keys) {
        for (String key : keys) {
            JSONArray array = params.getJSONArray(key);
            if (array != null && !array.isEmpty()) {
                return array;
            }
        }
        return null;
    }

    private boolean isInternalParam(String key) {
        return "referenceAssetIds".equals(key) || "referenceImageIds".equals(key) || "providerModel".equals(key);
    }

    private void addIfPresent(JSONArray array, String value) {
        if (StrUtil.isNotBlank(value)) {
            array.add(value);
        }
    }

    private int timeoutMillis(AigcProviderSubmitReqDTO reqDTO) {
        return (reqDTO.getProviderTimeoutSeconds() == null || reqDTO.getProviderTimeoutSeconds() <= 0 ? 120 : reqDTO.getProviderTimeoutSeconds()) * 1000;
    }

    private AigcProviderSubmitRespDTO failed(String code, String message) {
        return new AigcProviderSubmitRespDTO()
                .setSuccess(false)
                .setFinished(true)
                .setErrorCode(code)
                .setErrorMessage(StrUtil.maxLength(message, 512));
    }

    private String safeBody(String body) {
        return body == null ? null : StrUtil.maxLength(body, 1000);
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

}
