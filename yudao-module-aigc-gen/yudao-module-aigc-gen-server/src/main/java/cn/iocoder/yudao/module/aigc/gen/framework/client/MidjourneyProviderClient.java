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

import java.util.ArrayList;
import java.util.List;

@Component
public class MidjourneyProviderClient implements AigcProviderClient {

    public static final String CLIENT_TYPE = "MIDJOURNEY";
    private static final String PROVIDER_CODE = "midjourney";
    private static final int UPSCALE_COUNT = 4;

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
            return failed("CONFIG_INVALID", "Midjourney 渠道未配置 API 地址或 API Key");
        }
        long start = System.currentTimeMillis();
        try {
            JSONObject params = parseParams(reqDTO.getInputParams());
            JSONObject body = isVideo(reqDTO)
                    ? buildVideoSubmitBody(reqDTO, params)
                    : buildImagineSubmitBody(reqDTO, params);
            JSONObject response = postJson(reqDTO, resolveSubmitEndpoint(reqDTO), body);
            String taskId = firstNonBlank(response.getStr("result"), response.getStr("id"), response.getStr("taskId"));
            if (StrUtil.isBlank(taskId)) {
                return failed("MISSING_PROVIDER_TASK_ID", "Midjourney 提交响应缺少任务编号")
                        .setProviderStatus(response.getStr("description"))
                        .setOutputData(response.toString())
                        .setDurationMillis(System.currentTimeMillis() - start);
            }
            return new AigcProviderSubmitRespDTO()
                    .setProviderTaskId(taskId)
                    .setProviderStatus("SUBMITTED")
                    .setOutputData(buildOutputData(isVideo(reqDTO), response, null).toString())
                    .setFinished(false)
                    .setSuccess(true)
                    .setDurationMillis(System.currentTimeMillis() - start);
        } catch (MidjourneyException ex) {
            return failed(ex.code, ex.getMessage()).setDurationMillis(System.currentTimeMillis() - start);
        } catch (Exception ex) {
            return failed("REQUEST_EXCEPTION", ex.getMessage()).setDurationMillis(System.currentTimeMillis() - start);
        }
    }

    @Override
    public AigcProviderSubmitRespDTO query(String providerTaskId) {
        return failed("QUERY_CONTEXT_REQUIRED", "Midjourney 查询需要渠道配置上下文");
    }

    @Override
    public AigcProviderSubmitRespDTO query(AigcProviderSubmitReqDTO reqDTO) {
        if (StrUtil.isBlank(reqDTO.getProviderBaseUrl()) || StrUtil.isBlank(reqDTO.getProviderApiKey())) {
            return failed("CONFIG_INVALID", "Midjourney 渠道未配置 API 地址或 API Key");
        }
        if (StrUtil.isBlank(reqDTO.getProviderTaskId())) {
            return failed("TASK_ID_EMPTY", "Midjourney 查询缺少任务编号");
        }
        long start = System.currentTimeMillis();
        try {
            return isVideo(reqDTO)
                    ? queryVideo(reqDTO, start)
                    : queryImagine(reqDTO, start);
        } catch (MidjourneyException ex) {
            return failed(ex.code, ex.getMessage()).setDurationMillis(System.currentTimeMillis() - start);
        } catch (Exception ex) {
            return failed("REQUEST_EXCEPTION", ex.getMessage()).setDurationMillis(System.currentTimeMillis() - start);
        }
    }

    @Override
    public boolean verifyCallback(AigcGenerateCallbackReqDTO reqDTO) {
        return true;
    }

    private AigcProviderSubmitRespDTO queryImagine(AigcProviderSubmitReqDTO reqDTO, long start) {
        MidjourneyState state = MidjourneyState.parse(reqDTO.getProviderTaskId());
        JSONObject grid = getTask(reqDTO, state.gridTaskId());
        if (isFailedTask(grid)) {
            return failed("PROVIDER_FAILED", failReason(grid))
                    .setProviderTaskId(state.encode())
                    .setProviderStatus(grid.getStr("status"))
                    .setOutputData(buildOutputData(false, grid, state).toString())
                    .setDurationMillis(System.currentTimeMillis() - start);
        }
        if (!isSuccessTask(grid)) {
            return pending(state.encode(), grid.getStr("status"), buildOutputData(false, grid, state), start);
        }
        if (!state.hasAllUpscaleTaskIds()) {
            state = submitMissingUpscales(reqDTO, state, grid);
            return pending(state.encode(), "UPSCALING", buildOutputData(false, grid, state), start);
        }

        JSONArray outputUrls = new JSONArray();
        JSONArray upscaleTasks = new JSONArray();
        for (String upscaleTaskId : state.upscaleTaskIds()) {
            JSONObject upscale = getTask(reqDTO, upscaleTaskId);
            upscaleTasks.add(upscale);
            if (isFailedTask(upscale)) {
                return failed("UPSCALE_FAILED", failReason(upscale))
                        .setProviderTaskId(state.encode())
                        .setProviderStatus(upscale.getStr("status"))
                        .setOutputData(buildOutputData(false, grid, state).set("upscaleTasks", upscaleTasks).toString())
                        .setDurationMillis(System.currentTimeMillis() - start);
            }
            if (!isSuccessTask(upscale)) {
                return pending(state.encode(), "UPSCALING", buildOutputData(false, grid, state).set("upscaleTasks", upscaleTasks), start);
            }
            String imageUrl = firstNonBlank(upscale.getStr("imageUrl"), upscale.getStr("image_url"));
            if (StrUtil.isBlank(imageUrl)) {
                return failed("UPSCALE_RESULT_EMPTY", "Midjourney 放大任务成功但未返回图片地址")
                        .setProviderTaskId(state.encode())
                        .setProviderStatus(upscale.getStr("status"))
                        .setOutputData(buildOutputData(false, grid, state).set("upscaleTasks", upscaleTasks).toString())
                        .setDurationMillis(System.currentTimeMillis() - start);
            }
            outputUrls.add(imageUrl);
        }
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(state.encode())
                .setProviderStatus("SUCCESS")
                .setOutputData(buildOutputData(false, grid, state).set("upscaleTasks", upscaleTasks).toString())
                .setOutputUrls(outputUrls.toString())
                .setFinished(true)
                .setSuccess(true)
                .setDurationMillis(System.currentTimeMillis() - start);
    }

    private AigcProviderSubmitRespDTO queryVideo(AigcProviderSubmitReqDTO reqDTO, long start) {
        JSONObject task = getTask(reqDTO, reqDTO.getProviderTaskId());
        if (isFailedTask(task)) {
            return failed("PROVIDER_FAILED", failReason(task))
                    .setProviderTaskId(reqDTO.getProviderTaskId())
                    .setProviderStatus(task.getStr("status"))
                    .setOutputData(buildOutputData(true, task, null).toString())
                    .setDurationMillis(System.currentTimeMillis() - start);
        }
        if (!isSuccessTask(task)) {
            return pending(reqDTO.getProviderTaskId(), task.getStr("status"), buildOutputData(true, task, null), start);
        }
        JSONArray outputUrls = extractVideoUrls(task);
        if (outputUrls.isEmpty()) {
            return failed("VIDEO_RESULT_EMPTY", "Midjourney 视频任务成功但未返回视频地址")
                    .setProviderTaskId(reqDTO.getProviderTaskId())
                    .setProviderStatus(task.getStr("status"))
                    .setOutputData(buildOutputData(true, task, null).toString())
                    .setDurationMillis(System.currentTimeMillis() - start);
        }
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(reqDTO.getProviderTaskId())
                .setProviderStatus("SUCCESS")
                .setOutputData(buildOutputData(true, task, null).toString())
                .setOutputUrls(outputUrls.toString())
                .setFinished(true)
                .setSuccess(true)
                .setDurationMillis(System.currentTimeMillis() - start);
    }

    private MidjourneyState submitMissingUpscales(AigcProviderSubmitReqDTO reqDTO, MidjourneyState state, JSONObject grid) {
        List<String> upscaleTaskIds = new ArrayList<>(state.upscaleTaskIds());
        JSONArray buttons = grid.getJSONArray("buttons");
        for (int index = upscaleTaskIds.size(); index < UPSCALE_COUNT; index++) {
            String customId = findUpscaleCustomId(buttons, index + 1);
            if (StrUtil.isBlank(customId)) {
                throw new MidjourneyException("UPSCALE_BUTTON_MISSING", "Midjourney 结果缺少 U" + (index + 1) + " 放大按钮");
            }
            JSONObject response = postJson(reqDTO, resolveActionEndpoint(reqDTO), JSONUtil.createObj()
                    .set("taskId", state.gridTaskId())
                    .set("customId", customId));
            String taskId = firstNonBlank(response.getStr("result"), response.getStr("id"), response.getStr("taskId"));
            if (StrUtil.isBlank(taskId)) {
                throw new MidjourneyException("UPSCALE_TASK_ID_MISSING", "Midjourney U" + (index + 1) + " 提交响应缺少任务编号");
            }
            upscaleTaskIds.add(taskId);
        }
        return new MidjourneyState(state.gridTaskId(), upscaleTaskIds);
    }

    private JSONObject buildImagineSubmitBody(AigcProviderSubmitReqDTO reqDTO, JSONObject params) {
        JSONObject body = JSONUtil.createObj()
                .set("prompt", buildImaginePrompt(reqDTO, params))
                .set("botType", StrUtil.blankToDefault(params.getStr("botType"), "MID_JOURNEY"));
        JSONArray base64Array = params.getJSONArray("base64Array");
        if (base64Array != null && !base64Array.isEmpty()) {
            body.set("base64Array", base64Array);
        }
        copyIfPresent(body, params, "notifyHook", "state", "accountFilter");
        return body;
    }

    private JSONObject buildVideoSubmitBody(AigcProviderSubmitReqDTO reqDTO, JSONObject params) {
        String image = firstReferenceImage(params);
        String prompt = StrUtil.blankToDefault(reqDTO.getPrompt(), "").trim();
        JSONObject body = JSONUtil.createObj()
                .set("prompt", StrUtil.isBlank(image) ? prompt : (image + (StrUtil.isBlank(prompt) ? "" : " " + prompt)))
                .set("video_type", StrUtil.blankToDefault(params.getStr("video_type"), "vid_1.1_i2v_480"))
                .set("motion", StrUtil.blankToDefault(params.getStr("motion"), "low"))
                .set("animate_mode", StrUtil.blankToDefault(firstNonBlank(params.getStr("animate_mode"), params.getStr("animate_mod")), "manual"));
        copyIfPresent(body, params, "base64", "action", "taskId", "index", "noStorage", "notifyHook", "state");
        return body;
    }

    private String buildImaginePrompt(AigcProviderSubmitReqDTO reqDTO, JSONObject params) {
        String prompt = StrUtil.blankToDefault(reqDTO.getPrompt(), "").trim();
        StringBuilder builder = new StringBuilder(prompt);
        appendParam(builder, "--ar", firstNonBlank(params.getStr("aspect_ratio"), params.getStr("ar")));
        appendParam(builder, "--chaos", firstNonBlank(params.getStr("chaos"), params.getStr("c")));
        appendParam(builder, "--quality", firstNonBlank(params.getStr("quality"), params.getStr("q")));
        appendParam(builder, "--seed", params.getStr("seed"));
        appendParam(builder, "--stylize", firstNonBlank(params.getStr("stylize"), params.getStr("s")));
        appendParam(builder, "--weird", firstNonBlank(params.getStr("weird"), params.getStr("w")));
        appendParam(builder, "--no", params.getStr("no"));
        appendFlag(builder, "--raw", params.getBool("raw"));
        appendFlag(builder, "--tile", params.getBool("tile"));
        return builder.toString().trim();
    }

    private void appendParam(StringBuilder builder, String name, String value) {
        if (StrUtil.isBlank(value) || promptContainsParam(builder, name)) {
            return;
        }
        builder.append(' ').append(name).append(' ').append(value.trim());
    }

    private void appendFlag(StringBuilder builder, String name, Boolean enabled) {
        if (!Boolean.TRUE.equals(enabled) || promptContainsParam(builder, name)) {
            return;
        }
        builder.append(' ').append(name);
    }

    private boolean promptContainsParam(StringBuilder builder, String name) {
        String prompt = builder.toString();
        return prompt.contains(name + " ") || prompt.endsWith(name);
    }

    private String findUpscaleCustomId(JSONArray buttons, int index) {
        if (buttons == null || buttons.isEmpty()) {
            return null;
        }
        String target = "U" + index;
        for (Object item : buttons) {
            JSONObject button = JSONUtil.parseObj(item);
            String label = firstNonBlank(button.getStr("label"), button.getStr("emoji"), button.getStr("text"));
            String customId = firstNonBlank(button.getStr("customId"), button.getStr("custom_id"));
            if (target.equalsIgnoreCase(label) || StrUtil.containsIgnoreCase(customId, "upsample::" + index)) {
                return customId;
            }
        }
        return null;
    }

    private JSONArray extractVideoUrls(JSONObject task) {
        JSONArray urls = firstNonEmptyArray(task, "video_urls", "videoUrls");
        if (urls != null) {
            return urls;
        }
        JSONArray result = new JSONArray();
        String url = firstNonBlank(task.getStr("video_url"), task.getStr("videoUrl"));
        if (StrUtil.isNotBlank(url)) {
            result.add(url);
        }
        return result;
    }

    private JSONObject getTask(AigcProviderSubmitReqDTO reqDTO, String taskId) {
        try (HttpResponse response = AigcProviderProxyUtils.execute(HttpRequest.get(resolveTaskEndpoint(reqDTO, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                .timeout(timeoutMillis(reqDTO)), reqDTO)) {
            if (!response.isOk()) {
                throw new MidjourneyException("HTTP_" + response.getStatus(), safeBody(response.body()));
            }
            return JSONUtil.parseObj(response.body());
        }
    }

    private JSONObject postJson(AigcProviderSubmitReqDTO reqDTO, String endpoint, JSONObject body) {
        try (HttpResponse response = AigcProviderProxyUtils.execute(HttpRequest.post(endpoint)
                .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                .contentType(ContentType.JSON.getValue())
                .body(body.toString())
                .timeout(timeoutMillis(reqDTO)), reqDTO)) {
            if (!response.isOk()) {
                throw new MidjourneyException("HTTP_" + response.getStatus(), safeBody(response.body()));
            }
            JSONObject json = JSONUtil.parseObj(response.body());
            Integer code = json.getInt("code");
            if (code != null && code != 0 && code != 1 && code != 22) {
                throw new MidjourneyException("PROVIDER_CODE_" + code, StrUtil.blankToDefault(json.getStr("description"), json.toString()));
            }
            return json;
        }
    }

    private AigcProviderSubmitRespDTO pending(String providerTaskId, String providerStatus, JSONObject outputData, long start) {
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(providerTaskId)
                .setProviderStatus(StrUtil.blankToDefault(providerStatus, "SUBMITTED"))
                .setOutputData(outputData.toString())
                .setFinished(false)
                .setSuccess(true)
                .setDurationMillis(System.currentTimeMillis() - start);
    }

    private JSONObject buildOutputData(boolean video, JSONObject task, MidjourneyState state) {
        JSONObject data = JSONUtil.createObj()
                .set("provider", PROVIDER_CODE)
                .set("type", video ? "video" : "image")
                .set("task", task);
        if (state != null) {
            data.set("gridTaskId", state.gridTaskId())
                    .set("upscaleTaskIds", state.upscaleTaskIds());
        }
        return data;
    }

    private String resolveSubmitEndpoint(AigcProviderSubmitReqDTO reqDTO) {
        String base = modeBaseUrl(reqDTO);
        return base + (isVideo(reqDTO) ? "/submit/video" : "/submit/imagine");
    }

    private String resolveActionEndpoint(AigcProviderSubmitReqDTO reqDTO) {
        return baseUrl(reqDTO) + "/mj/submit/action";
    }

    private String resolveTaskEndpoint(AigcProviderSubmitReqDTO reqDTO, String taskId) {
        return baseUrl(reqDTO) + "/mj/task/" + taskId + "/fetch";
    }

    private String modeBaseUrl(AigcProviderSubmitReqDTO reqDTO) {
        String base = baseUrl(reqDTO);
        String model = StrUtil.blankToDefault(reqDTO.getProviderModel(), reqDTO.getModelCode());
        String lower = model == null ? "" : model.toLowerCase();
        if (lower.contains("video-turbo")) {
            return base + "/mj-relax/mj";
        }
        if (lower.contains("turbo")) {
            return base + "/mj-turbo/mj";
        }
        if (lower.contains("relax")) {
            return base + "/mj-relax/mj";
        }
        if (lower.contains("fast") && isVideo(reqDTO)) {
            return base + "/mj-fast/mj";
        }
        return base + "/mj";
    }

    private String baseUrl(AigcProviderSubmitReqDTO reqDTO) {
        String base = StrUtil.removeSuffix(reqDTO.getProviderBaseUrl(), "/");
        if (base.endsWith("/v1")) {
            return StrUtil.removeSuffix(base, "/v1");
        }
        if (base.endsWith("/mj")) {
            return StrUtil.removeSuffix(base, "/mj");
        }
        return base;
    }

    private boolean isVideo(AigcProviderSubmitReqDTO reqDTO) {
        return "VIDEO".equals(reqDTO.getGenerateType()) || "IMAGE_TO_VIDEO".equals(reqDTO.getGenerateMode())
                || "TEXT_TO_VIDEO".equals(reqDTO.getGenerateMode());
    }

    private boolean isSuccessTask(JSONObject task) {
        String status = task.getStr("status");
        return "SUCCESS".equalsIgnoreCase(status) || (StrUtil.isBlank(status)
                && (StrUtil.isNotBlank(task.getStr("imageUrl")) || !extractVideoUrls(task).isEmpty()));
    }

    private boolean isFailedTask(JSONObject task) {
        String status = task.getStr("status");
        return "FAILURE".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status) || "CANCEL".equalsIgnoreCase(status)
                || "CANCELLED".equalsIgnoreCase(status);
    }

    private String failReason(JSONObject task) {
        return firstNonBlank(task.getStr("failReason"), task.getStr("description"), task.toString());
    }

    private JSONObject parseParams(String inputParams) {
        if (StrUtil.isBlank(inputParams) || !JSONUtil.isTypeJSON(inputParams)) {
            return JSONUtil.createObj();
        }
        return JSONUtil.parseObj(inputParams);
    }

    private String firstReferenceImage(JSONObject params) {
        String image = firstNonBlank(params.getStr("image_url"), params.getStr("image"));
        if (StrUtil.isNotBlank(image)) {
            return image;
        }
        JSONArray referenceImages = firstNonEmptyArray(params, "referenceImages", "inputImageUrls", "image_urls");
        return referenceImages == null || referenceImages.isEmpty() ? null : referenceImages.getStr(0);
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

    private void copyIfPresent(JSONObject body, JSONObject params, String... keys) {
        for (String key : keys) {
            if (params.containsKey(key) && params.get(key) != null) {
                body.set(key, params.get(key));
            }
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private record MidjourneyState(String gridTaskId, List<String> upscaleTaskIds) {

        static MidjourneyState parse(String providerTaskId) {
            if (StrUtil.isBlank(providerTaskId)) {
                throw new MidjourneyException("TASK_ID_EMPTY", "Midjourney 查询缺少任务编号");
            }
            if (!providerTaskId.startsWith("mj:")) {
                return new MidjourneyState(providerTaskId, List.of());
            }
            String[] parts = providerTaskId.split("\\|");
            String gridTaskId = parts[0].substring("mj:".length());
            List<String> upscaleTaskIds = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                String taskId = StrUtil.subAfter(parts[i], "u" + i + ":", false);
                if (StrUtil.isNotBlank(taskId)) {
                    upscaleTaskIds.add(taskId);
                }
            }
            return new MidjourneyState(gridTaskId, upscaleTaskIds);
        }

        boolean hasAllUpscaleTaskIds() {
            return upscaleTaskIds.size() >= UPSCALE_COUNT;
        }

        String encode() {
            if (upscaleTaskIds.isEmpty()) {
                return gridTaskId;
            }
            StringBuilder builder = new StringBuilder("mj:").append(gridTaskId);
            for (int i = 0; i < upscaleTaskIds.size(); i++) {
                builder.append("|u").append(i + 1).append(":").append(upscaleTaskIds.get(i));
            }
            return builder.toString();
        }

    }

    private static class MidjourneyException extends RuntimeException {

        private final String code;

        MidjourneyException(String code, String message) {
            super(message);
            this.code = code;
        }

    }

}
