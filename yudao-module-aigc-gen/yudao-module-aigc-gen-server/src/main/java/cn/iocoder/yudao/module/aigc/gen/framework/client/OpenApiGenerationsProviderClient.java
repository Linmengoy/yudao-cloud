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

import java.util.List;
import java.util.Map;

@Component
public class OpenApiGenerationsProviderClient implements AigcProviderClient {

    public static final String CLIENT_TYPE = "OPENAPI_GENERATIONS";
    private static final String PROVIDER_CODE = "openapi-generations";
    private static final List<String> INTERNAL_PARAMS = List.of(
            "providerModel", "referenceImageIds", "referenceAssetIds", "inputImages", "inputImageIds",
            "referenceImages", "inputImageUrls", "referenceVideos", "inputVideoUrls", "referenceAudios", "inputAudioUrls");

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
            return failed("CONFIG_INVALID", "OpenAPI Generations 渠道未配置 API 地址或 API Key");
        }
        long start = System.currentTimeMillis();
        try (HttpResponse response = AigcProviderProxyUtils.execute(HttpRequest.post(resolveGenerationsEndpoint(reqDTO))
                .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                .contentType(ContentType.JSON.getValue())
                .body(buildSubmitBody(reqDTO).toString())
                .timeout(timeoutMillis(reqDTO)), reqDTO)) {
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
        return failed("QUERY_CONTEXT_REQUIRED", "OpenAPI Generations 查询需要渠道配置上下文");
    }

    @Override
    public AigcProviderSubmitRespDTO query(AigcProviderSubmitReqDTO reqDTO) {
        if (StrUtil.isBlank(reqDTO.getProviderBaseUrl()) || StrUtil.isBlank(reqDTO.getProviderApiKey())) {
            return failed("CONFIG_INVALID", "OpenAPI Generations 渠道未配置 API 地址或 API Key");
        }
        if (StrUtil.isBlank(reqDTO.getProviderTaskId())) {
            return failed("TASK_ID_EMPTY", "OpenAPI Generations 查询缺少任务编号");
        }
        long start = System.currentTimeMillis();
        try (HttpResponse response = AigcProviderProxyUtils.execute(HttpRequest.get(resolveGenerationQueryEndpoint(reqDTO))
                .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                .timeout(timeoutMillis(reqDTO)), reqDTO)) {
            if (!response.isOk()) {
                return failed("HTTP_" + response.getStatus(), safeBody(response.body())).setDurationMillis(System.currentTimeMillis() - start);
            }
            return parseResponse(response.body(), reqDTO).setDurationMillis(System.currentTimeMillis() - start);
        } catch (Exception ex) {
            return failed("REQUEST_EXCEPTION", ex.getMessage()).setDurationMillis(System.currentTimeMillis() - start);
        }
    }

    @Override
    public boolean verifyCallback(AigcGenerateCallbackReqDTO reqDTO) {
        return true;
    }

    private JSONObject buildSubmitBody(AigcProviderSubmitReqDTO reqDTO) {
        JSONObject params = parseParams(reqDTO.getInputParams());
        JSONObject body = JSONUtil.createObj()
                .set("model", StrUtil.blankToDefault(reqDTO.getProviderModel(), reqDTO.getModelCode()))
                .set("mode", resolveMode(reqDTO.getGenerateMode(), params))
                .set("prompt", StrUtil.blankToDefault(reqDTO.getPrompt(), ""));
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() != null && !isInternalParam(entry.getKey())) {
                body.set(entry.getKey(), entry.getValue());
            }
        }
        applyReferenceAliases(body, params, reqDTO.getGenerateMode());
        return body;
    }

    private void applyReferenceAliases(JSONObject body, JSONObject params, String generateMode) {
        if ("IMAGE_TO_VIDEO".equals(generateMode)) {
            String image = firstNonBlank(params.getStr("image_url"), params.getStr("image"), firstArrayValue(params, "referenceImages"),
                    firstArrayValue(params, "inputImageUrls"));
            if (StrUtil.isNotBlank(image)) {
                body.set("image_url", image);
            }
            return;
        }
        if ("FIRST_LAST_FRAME_VIDEO".equals(generateMode)) {
            JSONArray images = firstNonEmptyArray(params, "image_urls", "referenceImages", "inputImageUrls");
            if (images != null) {
                body.set("image_urls", images);
            }
            return;
        }
        if ("MULTI_REF_VIDEO".equals(generateMode) || "MULTI_REFERENCE_VIDEO".equals(generateMode)) {
            copyFirstNonEmptyArray(body, params, "image_urls", "referenceImages", "inputImageUrls");
            copyFirstNonEmptyArray(body, params, "video_urls", "referenceVideos", "inputVideoUrls");
            copyFirstNonEmptyArray(body, params, "audio_urls", "referenceAudios", "inputAudioUrls");
        }
    }

    private void copyFirstNonEmptyArray(JSONObject body, JSONObject params, String targetKey, String... sourceKeys) {
        if (body.containsKey(targetKey)) {
            return;
        }
        JSONArray value = firstNonEmptyArray(params, sourceKeys);
        if (value != null) {
            body.set(targetKey, value);
        }
    }

    private String resolveMode(String generateMode, JSONObject params) {
        String explicitMode = params.getStr("mode");
        if (StrUtil.isNotBlank(explicitMode)) {
            return explicitMode;
        }
        if ("TEXT_TO_VIDEO".equals(generateMode)) {
            return "text_to_video";
        }
        if ("IMAGE_TO_VIDEO".equals(generateMode)) {
            return "image_to_video";
        }
        if ("FIRST_LAST_FRAME_VIDEO".equals(generateMode)) {
            return "first_last_frame";
        }
        if ("MULTI_REF_VIDEO".equals(generateMode) || "MULTI_REFERENCE_VIDEO".equals(generateMode)) {
            return "multi_ref";
        }
        return StrUtil.blankToDefault(generateMode, "text_to_video").toLowerCase();
    }

    private AigcProviderSubmitRespDTO parseResponse(String body, AigcProviderSubmitReqDTO reqDTO) {
        JSONObject json = JSONUtil.parseObj(body);
        String status = json.getStr("status", "queued");
        String providerTaskId = resolveProviderTaskId(json, reqDTO);
        if (isFailed(status)) {
            return failed(json.getStr("error_code", "PROVIDER_FAILED"), resolveErrorMessage(json))
                    .setProviderTaskId(providerTaskId)
                    .setProviderStatus(status)
                    .setOutputData(json.toString());
        }
        JSONArray resultUrls = resolveResultUrls(json);
        boolean completed = isCompleted(status);
        if (!completed && StrUtil.isBlank(providerTaskId)) {
            return failed("MISSING_PROVIDER_TASK_ID", "OpenAPI Generations 提交响应缺少上游任务编号")
                    .setProviderStatus(status)
                    .setOutputData(json.toString());
        }
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(providerTaskId)
                .setProviderStatus(status)
                .setFinished(completed)
                .setSuccess(true)
                .setOutputData(json.toString())
                .setOutputUrls(resultUrls == null || resultUrls.isEmpty() ? null : resultUrls.toString());
    }

    private String resolveProviderTaskId(JSONObject json, AigcProviderSubmitReqDTO reqDTO) {
        return firstNonBlank(json.getStr("id"), json.getStr("provider_task_id"), json.getStr("providerTaskId"),
                json.getStr("task_id"), json.getStr("taskId"), reqDTO.getProviderTaskId());
    }

    private JSONArray resolveResultUrls(JSONObject json) {
        JSONArray resultUrls = json.getJSONArray("result_urls");
        if (resultUrls != null && !resultUrls.isEmpty()) {
            return resultUrls;
        }
        String resultUrl = json.getStr("result_url");
        if (StrUtil.isNotBlank(resultUrl)) {
            return new JSONArray().put(resultUrl);
        }
        return null;
    }

    private String resolveErrorMessage(JSONObject json) {
        Object error = json.get("error");
        if (error == null) {
            return json.getStr("message", "OpenAPI Generations 任务失败");
        }
        if (error instanceof JSONObject errorObj) {
            return StrUtil.blankToDefault(errorObj.getStr("message"), errorObj.toString());
        }
        return String.valueOf(error);
    }

    private String resolveGenerationsEndpoint(AigcProviderSubmitReqDTO reqDTO) {
        String baseUrl = StrUtil.removeSuffix(reqDTO.getProviderBaseUrl(), "/");
        return baseUrl.endsWith("/generations") ? baseUrl : baseUrl + "/generations";
    }

    private String resolveGenerationQueryEndpoint(AigcProviderSubmitReqDTO reqDTO) {
        String baseUrl = StrUtil.removeSuffix(reqDTO.getProviderBaseUrl(), "/");
        if (baseUrl.endsWith("/generations/" + reqDTO.getProviderTaskId())) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/generations")) {
            return baseUrl + "/" + reqDTO.getProviderTaskId();
        }
        return baseUrl + "/generations/" + reqDTO.getProviderTaskId();
    }

    private JSONObject parseParams(String inputParams) {
        return StrUtil.isBlank(inputParams) || !JSONUtil.isTypeJSON(inputParams) ? new JSONObject() : JSONUtil.parseObj(inputParams);
    }

    private boolean isInternalParam(String key) {
        return INTERNAL_PARAMS.contains(key);
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

    private String firstArrayValue(JSONObject params, String key) {
        JSONArray array = params.getJSONArray(key);
        return array == null || array.isEmpty() ? null : array.getStr(0);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isCompleted(String status) {
        return "completed".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status) || "succeeded".equalsIgnoreCase(status);
    }

    private boolean isFailed(String status) {
        return "failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status);
    }

    private int timeoutMillis(AigcProviderSubmitReqDTO reqDTO) {
        return (reqDTO.getProviderTimeoutSeconds() == null || reqDTO.getProviderTimeoutSeconds() <= 0 ? 120 : reqDTO.getProviderTimeoutSeconds()) * 1000;
    }

    private String safeBody(String body) {
        return StrUtil.maxLength(body, 1000);
    }

    private AigcProviderSubmitRespDTO failed(String code, String message) {
        return new AigcProviderSubmitRespDTO().setSuccess(false).setFinished(true).setErrorCode(code).setErrorMessage(message);
    }

}
