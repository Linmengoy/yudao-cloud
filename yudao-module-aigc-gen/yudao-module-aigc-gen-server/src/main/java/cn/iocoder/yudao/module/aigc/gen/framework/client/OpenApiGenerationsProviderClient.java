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
import java.util.Set;

@Component
public class OpenApiGenerationsProviderClient implements AigcProviderClient {

    public static final String CLIENT_TYPE = "OPENAPI_GENERATIONS";
    private static final String PROVIDER_CODE = "openapi-generations";

    private static final String MODE_TEXT_TO_VIDEO = "text_to_video";
    private static final String MODE_IMAGE_TO_VIDEO = "image_to_video";
    private static final String MODE_FIRST_LAST_FRAME = "first_last_frame";
    private static final String MODE_MULTI_REF = "multi_ref";
    private static final Set<String> REVIEWED_ASSET_MODELS = Set.of(
            "seedance-2",
            "seedance-2-fast",
            "seedance-2-official2",
            "seedance-2-official2-fast",
            "seedance-2-intl",
            "seedance-2-intl-fast",
            "seedance-2-intl-xl",
            "seedance-2-intl-xl-fast");

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
        try {
            JSONObject body = buildSubmitBody(reqDTO);
            prepareReviewedAssets(reqDTO, body);
            try (HttpResponse response = AigcProviderProxyUtils.execute(
                HttpRequest.post(resolveSubmitEndpoint(reqDTO))
                        .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                        .contentType(ContentType.JSON.getValue())
                        .body(body.toString())
                        .timeout(timeoutMillis(reqDTO)),
                    reqDTO)) {
                if (!response.isOk()) {
                    return failed("HTTP_" + response.getStatus(), safeErrorMessage(response.body()))
                            .setDurationMillis(System.currentTimeMillis() - start);
                }
                return parseSubmitResponse(response.body(), reqDTO).setDurationMillis(System.currentTimeMillis() - start);
            }
        } catch (AssetReviewException ex) {
            return failed(ex.code, ex.getMessage()).setDurationMillis(System.currentTimeMillis() - start);
        } catch (Exception ex) {
            return failed("REQUEST_EXCEPTION", ex.getMessage())
                    .setDurationMillis(System.currentTimeMillis() - start);
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
        try (HttpResponse response = AigcProviderProxyUtils.execute(
                HttpRequest.get(resolveQueryEndpoint(reqDTO))
                        .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                        .timeout(timeoutMillis(reqDTO)),
                reqDTO)) {
            if (!response.isOk()) {
                return failed("HTTP_" + response.getStatus(), safeErrorMessage(response.body()))
                        .setDurationMillis(System.currentTimeMillis() - start);
            }
            return parseQueryResponse(response.body(), reqDTO.getProviderTaskId())
                    .setDurationMillis(System.currentTimeMillis() - start);
        } catch (Exception ex) {
            return failed("REQUEST_EXCEPTION", ex.getMessage())
                    .setDurationMillis(System.currentTimeMillis() - start);
        }
    }

    @Override
    public boolean verifyCallback(AigcGenerateCallbackReqDTO reqDTO) {
        return true;
    }

    private JSONObject buildSubmitBody(AigcProviderSubmitReqDTO reqDTO) {
        String model = StrUtil.blankToDefault(reqDTO.getProviderModel(), reqDTO.getModelCode());
        JSONObject body = JSONUtil.createObj()
                .set("model", model)
                .set("mode", resolveMode(reqDTO))
                .set("prompt", StrUtil.blankToDefault(reqDTO.getPrompt(), ""));

        JSONObject params = parseParams(reqDTO.getInputParams());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value != null && !isInternalParam(key)) {
                body.set(key, value);
            }
        }

        applyReferences(body, params, reqDTO.getGenerateMode());
        return body;
    }

    private String resolveMode(AigcProviderSubmitReqDTO reqDTO) {
        JSONObject params = parseParams(reqDTO.getInputParams());
        String explicitMode = params.getStr("mode");
        if (StrUtil.isNotBlank(explicitMode)) {
            return explicitMode;
        }
        String mode = reqDTO.getGenerateMode();
        if (mode == null) {
            return MODE_TEXT_TO_VIDEO;
        }
        return switch (mode) {
            case "TEXT_TO_VIDEO" -> MODE_TEXT_TO_VIDEO;
            case "IMAGE_TO_VIDEO" -> MODE_IMAGE_TO_VIDEO;
            case "FIRST_LAST_FRAME_VIDEO" -> MODE_FIRST_LAST_FRAME;
            case "MULTI_REF_VIDEO", "MULTI_REFERENCE_VIDEO" -> MODE_MULTI_REF;
            default -> MODE_TEXT_TO_VIDEO;
        };
    }

    private void applyReferences(JSONObject body, JSONObject params, String generateMode) {
        String imageUrl = firstNonBlank(params.getStr("image_url"), params.getStr("image"));
        if (StrUtil.isNotBlank(imageUrl)) {
            body.set("image_url", imageUrl);
        }

        JSONArray referenceImages = firstNonEmptyArray(params, "image_urls", "referenceImages", "inputImageUrls");
        if (referenceImages != null) {
            if ("MULTI_REF_VIDEO".equals(generateMode) || "MULTI_REFERENCE_VIDEO".equals(generateMode)
                    || "FIRST_LAST_FRAME_VIDEO".equals(generateMode)) {
                body.set("image_urls", referenceImages);
            } else if (!body.containsKey("image_url") && referenceImages.size() == 1) {
                body.set("image_url", referenceImages.getStr(0));
            } else {
                body.set("image_urls", referenceImages);
            }
        }

        copyFirstNonEmptyArray(body, params, "video_urls", "referenceVideos", "inputVideoUrls");
        copyFirstNonEmptyArray(body, params, "audio_urls", "referenceAudios", "inputAudioUrls");
    }

    private void prepareReviewedAssets(AigcProviderSubmitReqDTO reqDTO, JSONObject body) {
        String model = StrUtil.blankToDefault(reqDTO.getProviderModel(), reqDTO.getModelCode());
        if (!requiresReviewedAssets(model)) {
            return;
        }
        String region = reviewedAssetRegion(model);
        String groupId = resolveReviewedAssetGroup(reqDTO, region);
        if (body.containsKey("image_url")) {
            body.set("image_url", reviewAssetUrl(reqDTO, groupId, body.getStr("image_url")));
        }
        JSONArray imageUrls = body.getJSONArray("image_urls");
        if (imageUrls != null && !imageUrls.isEmpty()) {
            JSONArray reviewed = new JSONArray();
            for (Object imageUrl : imageUrls) {
                reviewed.add(reviewAssetUrl(reqDTO, groupId, String.valueOf(imageUrl)));
            }
            body.set("image_urls", reviewed);
        }
    }

    private boolean requiresReviewedAssets(String model) {
        return StrUtil.isNotBlank(model) && REVIEWED_ASSET_MODELS.contains(model.trim().toLowerCase());
    }

    private String reviewedAssetRegion(String model) {
        return model != null && model.trim().toLowerCase().startsWith("seedance-2-intl") ? "intl" : "cn";
    }

    private String resolveReviewedAssetGroup(AigcProviderSubmitReqDTO reqDTO, String region) {
        JSONObject extraConfig = parseParams(reqDTO.getProviderExtraConfig());
        String configured = "intl".equals(region)
                ? firstNonBlank(extraConfig.getStr("seedanceAssetGroupIntl"), extraConfig.getStr("assetGroupIntl"))
                : firstNonBlank(extraConfig.getStr("seedanceAssetGroupCn"), extraConfig.getStr("assetGroupCn"));
        if (StrUtil.isNotBlank(configured)) {
            return configured;
        }
        JSONObject body = JSONUtil.createObj()
                .set("name", "Copse Seedance " + region)
                .set("description", "Reviewed references for Copse Seedance " + region)
                .set("region", region);
        JSONObject response = postJson(reqDTO, resolveAssetGroupsEndpoint(reqDTO), body);
        String groupId = firstNonBlank(response.getStr("officialId"), response.getStr("id"));
        if (StrUtil.isBlank(groupId)) {
            throw new AssetReviewException("ASSET_GROUP_CREATE_FAILED", "OpenAPI reviewed asset group response missing officialId");
        }
        return groupId;
    }

    private String reviewAssetUrl(AigcProviderSubmitReqDTO reqDTO, String groupId, String url) {
        if (StrUtil.startWithIgnoreCase(url, "asset://")) {
            return url;
        }
        if (!StrUtil.startWithIgnoreCase(url, "http://") && !StrUtil.startWithIgnoreCase(url, "https://")) {
            throw new AssetReviewException("ASSET_REVIEW_URL_INVALID", "Reviewed asset requires a public HTTP URL");
        }
        JSONObject uploadBody = JSONUtil.createObj()
                .set("groupId", groupId)
                .set("url", url)
                .set("name", "Copse reference asset");
        JSONObject upload = postJson(reqDTO, resolveAssetsEndpoint(reqDTO), uploadBody);
        String officialId = firstNonBlank(upload.getStr("officialId"), upload.getStr("id"));
        if (StrUtil.isBlank(officialId)) {
            throw new AssetReviewException("ASSET_UPLOAD_FAILED", "OpenAPI asset upload response missing officialId");
        }
        waitReviewedAssetActive(reqDTO, officialId, upload.getStr("status"));
        return "asset://" + officialId;
    }

    private void waitReviewedAssetActive(AigcProviderSubmitReqDTO reqDTO, String officialId, String initialStatus) {
        String status = initialStatus;
        int maxPolls = reviewedAssetMaxPolls(reqDTO);
        for (int i = 0; i <= maxPolls; i++) {
            if ("Active".equalsIgnoreCase(status)) {
                return;
            }
            if ("Failed".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status)) {
                throw new AssetReviewException("ASSET_REVIEW_FAILED", "Reviewed asset " + officialId + " status is " + status);
            }
            if (i == maxPolls) {
                break;
            }
            sleepQuietly(reviewedAssetPollIntervalMillis(reqDTO));
            JSONObject detail = getJson(reqDTO, resolveAssetDetailEndpoint(reqDTO, officialId));
            status = detail.getStr("status");
        }
        throw new AssetReviewException("ASSET_REVIEW_TIMEOUT", "Reviewed asset " + officialId + " did not become Active");
    }

    private JSONObject postJson(AigcProviderSubmitReqDTO reqDTO, String endpoint, JSONObject body) {
        try (HttpResponse response = AigcProviderProxyUtils.execute(
                HttpRequest.post(endpoint)
                        .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                        .contentType(ContentType.JSON.getValue())
                        .body(body.toString())
                        .timeout(timeoutMillis(reqDTO)),
                reqDTO)) {
            if (!response.isOk()) {
                throw new AssetReviewException("HTTP_" + response.getStatus(), safeBody(response.body()));
            }
            return JSONUtil.parseObj(response.body());
        }
    }

    private JSONObject getJson(AigcProviderSubmitReqDTO reqDTO, String endpoint) {
        try (HttpResponse response = AigcProviderProxyUtils.execute(
                HttpRequest.get(endpoint)
                        .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                        .timeout(timeoutMillis(reqDTO)),
                reqDTO)) {
            if (!response.isOk()) {
                throw new AssetReviewException("HTTP_" + response.getStatus(), safeBody(response.body()));
            }
            return JSONUtil.parseObj(response.body());
        }
    }

    private AigcProviderSubmitRespDTO parseSubmitResponse(String responseBody, AigcProviderSubmitReqDTO reqDTO) {
        JSONObject json = JSONUtil.parseObj(responseBody);
        String taskId = resolveProviderTaskId(json, null);
        String status = json.getStr("status", "pending");
        if (isFailed(status)) {
            return failed(json.getStr("error_code", "PROVIDER_FAILED"), resolveErrorMessage(json))
                    .setProviderTaskId(taskId)
                    .setProviderStatus(status)
                    .setOutputData(responseBody);
        }
        boolean completed = isCompleted(status);
        if (!completed && StrUtil.isBlank(taskId)) {
            return failed("MISSING_PROVIDER_TASK_ID", "OpenAPI Generations 提交响应缺少上游任务编号")
                    .setProviderStatus(status)
                    .setOutputData(responseBody);
        }
        String outputUrls = completed ? outputUrls(json) : null;
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(taskId)
                .setProviderStatus(status)
                .setOutputData(responseBody)
                .setOutputUrls(outputUrls)
                .setFinished(completed)
                .setSuccess(true);
    }

    private AigcProviderSubmitRespDTO parseQueryResponse(String responseBody, String fallbackTaskId) {
        JSONObject json = JSONUtil.parseObj(responseBody);
        String taskId = resolveProviderTaskId(json, fallbackTaskId);
        String status = json.getStr("status", "pending");

        if (isFailed(status)) {
            return failed(json.getStr("error_code", "PROVIDER_FAILED"), resolveErrorMessage(json))
                    .setProviderTaskId(taskId)
                    .setProviderStatus(status)
                    .setOutputData(responseBody);
        }

        boolean completed = isCompleted(status);
        String outputUrls = completed ? outputUrls(json) : null;
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(taskId)
                .setProviderStatus(status)
                .setOutputData(responseBody)
                .setOutputUrls(outputUrls)
                .setFinished(completed)
                .setSuccess(true);
    }

    private String outputUrls(JSONObject json) {
        JSONArray urls = extractOutputUrls(json);
        return urls.isEmpty() ? null : urls.toString();
    }

    private JSONArray extractOutputUrls(JSONObject json) {
        JSONArray urls = new JSONArray();
        String videoUrl = json.getByPath("output.video_url", String.class);
        if (StrUtil.isNotBlank(videoUrl)) {
            urls.add(videoUrl);
            return urls;
        }
        JSONArray resultUrls = json.getJSONArray("result_urls");
        if (resultUrls != null && !resultUrls.isEmpty()) {
            return resultUrls;
        }
        String resultUrl = json.getStr("result_url");
        if (StrUtil.isNotBlank(resultUrl)) {
            urls.add(resultUrl);
            return urls;
        }
        JSONArray videoUrls = json.getByPath("output.video_urls", JSONArray.class);
        if (videoUrls != null && !videoUrls.isEmpty()) {
            return videoUrls;
        }
        String topVideoUrl = json.getStr("video_url");
        if (StrUtil.isNotBlank(topVideoUrl)) {
            urls.add(topVideoUrl);
            return urls;
        }
        JSONArray results = json.getJSONArray("results");
        if (results != null) {
            for (Object item : results) {
                JSONObject result = JSONUtil.parseObj(item);
                String url = firstNonBlank(result.getStr("video_url"), result.getStr("url"));
                if (StrUtil.isNotBlank(url)) {
                    urls.add(url);
                }
            }
        }
        return urls;
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

    private String resolveProviderTaskId(JSONObject json, String fallbackTaskId) {
        return firstNonBlank(json.getStr("id"), json.getStr("provider_task_id"), json.getStr("providerTaskId"),
                json.getStr("task_id"), json.getStr("taskId"), fallbackTaskId);
    }

    private String resolveSubmitEndpoint(AigcProviderSubmitReqDTO reqDTO) {
        String baseUrl = StrUtil.removeSuffix(reqDTO.getProviderBaseUrl(), "/");
        return baseUrl.endsWith("/generations") ? baseUrl : baseUrl + "/generations";
    }

    private String resolveAssetGroupsEndpoint(AigcProviderSubmitReqDTO reqDTO) {
        return resolveOpenApiBase(reqDTO) + "/asset-groups";
    }

    private String resolveAssetsEndpoint(AigcProviderSubmitReqDTO reqDTO) {
        return resolveOpenApiBase(reqDTO) + "/assets";
    }

    private String resolveAssetDetailEndpoint(AigcProviderSubmitReqDTO reqDTO, String officialId) {
        return resolveAssetsEndpoint(reqDTO) + "/" + officialId;
    }

    private String resolveOpenApiBase(AigcProviderSubmitReqDTO reqDTO) {
        String baseUrl = StrUtil.removeSuffix(reqDTO.getProviderBaseUrl(), "/");
        if (baseUrl.endsWith("/generations")) {
            return StrUtil.removeSuffix(baseUrl, "/generations");
        }
        return baseUrl;
    }

    private String resolveQueryEndpoint(AigcProviderSubmitReqDTO reqDTO) {
        String baseUrl = StrUtil.removeSuffix(reqDTO.getProviderBaseUrl(), "/");
        String taskId = reqDTO.getProviderTaskId();
        if (baseUrl.endsWith("/generations/" + taskId)) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/generations")) {
            return baseUrl + "/" + taskId;
        }
        return baseUrl + "/generations/" + taskId;
    }

    private boolean isInternalParam(String key) {
        return "providerModel".equals(key)
                || "referenceImages".equals(key)
                || "referenceImageIds".equals(key)
                || "referenceAssetIds".equals(key)
                || "referenceVideos".equals(key)
                || "referenceVideoIds".equals(key)
                || "referenceAudios".equals(key)
                || "referenceAudioIds".equals(key)
                || "inputImages".equals(key)
                || "inputImageIds".equals(key)
                || "inputImageUrls".equals(key)
                || "inputVideoUrls".equals(key)
                || "inputAudioUrls".equals(key);
    }

    private boolean isCompleted(String status) {
        return "completed".equalsIgnoreCase(status)
                || "success".equalsIgnoreCase(status)
                || "succeeded".equalsIgnoreCase(status);
    }

    private boolean isFailed(String status) {
        return "failed".equalsIgnoreCase(status)
                || "error".equalsIgnoreCase(status)
                || "cancelled".equalsIgnoreCase(status)
                || "canceled".equalsIgnoreCase(status);
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

    private void copyFirstNonEmptyArray(JSONObject body, JSONObject params, String targetKey, String... sourceKeys) {
        if (body.containsKey(targetKey)) {
            return;
        }
        JSONArray value = firstNonEmptyArray(params, sourceKeys);
        if (value != null) {
            body.set(targetKey, value);
        }
    }

    private int timeoutMillis(AigcProviderSubmitReqDTO reqDTO) {
        return (reqDTO.getProviderTimeoutSeconds() == null || reqDTO.getProviderTimeoutSeconds() <= 0 ? 120 : reqDTO.getProviderTimeoutSeconds()) * 1000;
    }

    private int reviewedAssetMaxPolls(AigcProviderSubmitReqDTO reqDTO) {
        JSONObject extraConfig = parseParams(reqDTO.getProviderExtraConfig());
        Integer configured = extraConfig.getInt("seedanceAssetReviewMaxPolls");
        return configured == null || configured < 0 ? 30 : configured;
    }

    private long reviewedAssetPollIntervalMillis(AigcProviderSubmitReqDTO reqDTO) {
        JSONObject extraConfig = parseParams(reqDTO.getProviderExtraConfig());
        Integer configured = extraConfig.getInt("seedanceAssetReviewPollIntervalMillis");
        return configured == null || configured < 0 ? 1000L : configured.longValue();
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssetReviewException("ASSET_REVIEW_INTERRUPTED", "Reviewed asset polling was interrupted");
        }
    }

    private AigcProviderSubmitRespDTO failed(String code, String message) {
        return new AigcProviderSubmitRespDTO()
                .setSuccess(false)
                .setFinished(true)
                .setErrorCode(classifyErrorCode(code, message))
                .setErrorMessage(message);
    }

    private String classifyErrorCode(String code, String message) {
        String normalizedCode = StrUtil.nullToEmpty(code).toUpperCase();
        String normalizedMessage = StrUtil.nullToEmpty(message).toLowerCase();
        if (normalizedCode.contains("ASSET_REVIEW")) {
            return code;
        }
        if (normalizedCode.contains("MEDIA_URL")
                || normalizedMessage.contains("media url") || normalizedMessage.contains("media_urls")
                || normalizedMessage.contains("image url") || normalizedMessage.contains("image_url")
                || normalizedMessage.contains("reference") || normalizedMessage.contains("asset")) {
            return "MEDIA_URL_INVALID";
        }
        if (normalizedCode.equals("HTTP_400") || normalizedCode.contains("PARAM")
                || normalizedMessage.contains("request parameters were rejected")
                || normalizedMessage.contains("verify the prompt")
                || normalizedMessage.contains("invalid parameter")
                || normalizedMessage.contains("invalid params")
                || normalizedMessage.contains("bad request")) {
            return "PARAM_REJECTED";
        }
        return code;
    }

    private String safeErrorMessage(String body) {
        if (StrUtil.isBlank(body)) {
            return body;
        }
        try {
            return resolveErrorMessage(JSONUtil.parseObj(body));
        } catch (Exception ignored) {
            return safeBody(body);
        }
    }

    private String safeBody(String body) {
        if (body == null) {
            return null;
        }
        return StrUtil.maxLength(body, 1000);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static class AssetReviewException extends RuntimeException {

        private final String code;

        AssetReviewException(String code, String message) {
            super(message);
            this.code = code;
        }

    }

}
