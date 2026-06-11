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
public class OpenApiGenerationsProviderClient implements AigcProviderClient {

    private static final String PROVIDER_CODE = "openapi-generations";

    private static final String MODE_TEXT_TO_VIDEO = "text_to_video";
    private static final String MODE_IMAGE_TO_VIDEO = "image_to_video";
    private static final String MODE_FIRST_LAST_FRAME = "first_last_frame";
    private static final String MODE_MULTI_REF = "multi_ref";

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public AigcProviderSubmitRespDTO submit(AigcProviderSubmitReqDTO reqDTO) {
        if (StrUtil.isBlank(reqDTO.getProviderBaseUrl()) || StrUtil.isBlank(reqDTO.getProviderApiKey())) {
            return failed("CONFIG_INVALID", "OpenAPI Generations 渠道未配置 API 地址或 API Key");
        }
        long start = System.currentTimeMillis();
        try (HttpResponse response = AigcProviderProxyUtils.execute(
                HttpRequest.post(resolveSubmitEndpoint(reqDTO))
                        .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                        .contentType(ContentType.JSON.getValue())
                        .body(buildSubmitBody(reqDTO).toString())
                        .timeout(timeoutMillis(reqDTO)),
                reqDTO)) {
            if (!response.isOk()) {
                return failed("HTTP_" + response.getStatus(), safeBody(response.body()))
                        .setDurationMillis(System.currentTimeMillis() - start);
            }
            return parseSubmitResponse(response.body()).setDurationMillis(System.currentTimeMillis() - start);
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
                return failed("HTTP_" + response.getStatus(), safeBody(response.body()))
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

    // ========== submit helpers ==========

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
        String mode = reqDTO.getGenerateMode();
        if (mode == null) {
            return MODE_TEXT_TO_VIDEO;
        }
        return switch (mode) {
            case "TEXT_TO_VIDEO" -> MODE_TEXT_TO_VIDEO;
            case "IMAGE_TO_VIDEO" -> MODE_IMAGE_TO_VIDEO;
            case "FIRST_LAST_FRAME_VIDEO" -> MODE_FIRST_LAST_FRAME;
            case "MULTI_REF_VIDEO" -> MODE_MULTI_REF;
            default -> MODE_TEXT_TO_VIDEO;
        };
    }

    private void applyReferences(JSONObject body, JSONObject params, String generateMode) {
        // image_url (single string)
        String imageUrl = params.getStr("image_url");
        if (StrUtil.isNotBlank(imageUrl)) {
            body.set("image_url", imageUrl);
        }

        // image_urls (array from referenceImages)
        JSONArray referenceImages = params.getJSONArray("referenceImages");
        if (referenceImages != null && !referenceImages.isEmpty()) {
            if ("MULTI_REF_VIDEO".equals(generateMode)) {
                body.set("image_urls", referenceImages);
            } else if (!body.containsKey("image_url") && referenceImages.size() == 1) {
                body.set("image_url", referenceImages.getStr(0));
            } else {
                body.set("image_urls", referenceImages);
            }
        }

        // video_urls
        JSONArray referenceVideos = params.getJSONArray("referenceVideos");
        if (referenceVideos != null && !referenceVideos.isEmpty()) {
            body.set("video_urls", referenceVideos);
        }

        // audio_urls
        JSONArray referenceAudios = params.getJSONArray("referenceAudios");
        if (referenceAudios != null && !referenceAudios.isEmpty()) {
            body.set("audio_urls", referenceAudios);
        }
    }

    // ========== response parsing ==========

    private AigcProviderSubmitRespDTO parseSubmitResponse(String responseBody) {
        JSONObject json = JSONUtil.parseObj(responseBody);
        String taskId = firstNonBlank(json.getStr("id"), json.getStr("task_id"));
        if (StrUtil.isBlank(taskId)) {
            return failed("TASK_ID_EMPTY", "OpenAPI Generations 提交未返回任务编号");
        }
        String status = json.getStr("status", "pending");
        boolean completed = isCompleted(status);
        String outputUrls = null;
        if (completed) {
            JSONArray urls = extractOutputUrls(json);
            outputUrls = urls.isEmpty() ? null : urls.toString();
        }
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
        String taskId = firstNonBlank(json.getStr("id"), json.getStr("task_id"), fallbackTaskId);
        String status = json.getStr("status", "pending");

        if (isFailed(status)) {
            String error = firstNonBlank(
                    json.getByPath("error.message", String.class),
                    json.getStr("error"),
                    json.getStr("message"),
                    "OpenAPI Generations 任务失败");
            return failed("PROVIDER_FAILED", error)
                    .setProviderTaskId(taskId)
                    .setProviderStatus(status)
                    .setOutputData(responseBody);
        }

        boolean completed = isCompleted(status);
        String outputUrls = null;
        if (completed) {
            JSONArray urls = extractOutputUrls(json);
            outputUrls = urls.isEmpty() ? null : urls.toString();
        }
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(taskId)
                .setProviderStatus(status)
                .setOutputData(responseBody)
                .setOutputUrls(outputUrls)
                .setFinished(completed)
                .setSuccess(true);
    }

    private JSONArray extractOutputUrls(JSONObject json) {
        JSONArray urls = new JSONArray();
        // output.video_url (single)
        String videoUrl = json.getByPath("output.video_url", String.class);
        if (StrUtil.isNotBlank(videoUrl)) {
            urls.add(videoUrl);
            return urls;
        }
        // top-level result_urls/result_url (HKCOPP OpenAPI response)
        JSONArray resultUrls = json.getJSONArray("result_urls");
        if (resultUrls != null && !resultUrls.isEmpty()) {
            return resultUrls;
        }
        String resultUrl = json.getStr("result_url");
        if (StrUtil.isNotBlank(resultUrl)) {
            urls.add(resultUrl);
            return urls;
        }
        // output.video_urls (array)
        JSONArray videoUrls = json.getByPath("output.video_urls", JSONArray.class);
        if (videoUrls != null && !videoUrls.isEmpty()) {
            return videoUrls;
        }
        // top-level video_url
        String topVideoUrl = json.getStr("video_url");
        if (StrUtil.isNotBlank(topVideoUrl)) {
            urls.add(topVideoUrl);
            return urls;
        }
        // results array
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

    // ========== endpoint resolution ==========

    private String resolveSubmitEndpoint(AigcProviderSubmitReqDTO reqDTO) {
        String baseUrl = StrUtil.removeSuffix(reqDTO.getProviderBaseUrl(), "/");
        if (baseUrl.endsWith("/generations")) {
            return baseUrl;
        }
        return baseUrl + "/generations";
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

    // ========== internal param filter ==========

    private boolean isInternalParam(String key) {
        return "providerModel".equals(key)
                || "referenceImages".equals(key)
                || "referenceImageIds".equals(key)
                || "referenceVideos".equals(key)
                || "referenceVideoIds".equals(key)
                || "referenceAudios".equals(key)
                || "referenceAudioIds".equals(key)
                || "inputImages".equals(key)
                || "inputImageIds".equals(key)
                || "inputImageUrls".equals(key);
    }

    // ========== status helpers ==========

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

    // ========== utility ==========

    private JSONObject parseParams(String inputParams) {
        if (StrUtil.isBlank(inputParams) || !JSONUtil.isTypeJSON(inputParams)) {
            return JSONUtil.createObj();
        }
        return JSONUtil.parseObj(inputParams);
    }

    private int timeoutMillis(AigcProviderSubmitReqDTO reqDTO) {
        return (reqDTO.getProviderTimeoutSeconds() == null ? 120 : reqDTO.getProviderTimeoutSeconds()) * 1000;
    }

    private AigcProviderSubmitRespDTO failed(String code, String message) {
        return new AigcProviderSubmitRespDTO()
                .setSuccess(false)
                .setFinished(true)
                .setErrorCode(code)
                .setErrorMessage(message);
    }

    private String safeBody(String body) {
        if (body == null) {
            return null;
        }
        return body.length() <= 512 ? body : body.substring(0, 512);
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
