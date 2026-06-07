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
import cn.iocoder.yudao.module.aigc.gen.framework.security.AigcGenerateFileSecurityUtils;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class GrokImagineProviderClient implements AigcProviderClient {

    private static final String PROVIDER_CODE = "grok";

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public AigcProviderSubmitRespDTO submit(AigcProviderSubmitReqDTO reqDTO) {
        if (StrUtil.isBlank(reqDTO.getProviderBaseUrl()) || StrUtil.isBlank(reqDTO.getProviderApiKey())) {
            return failed("CONFIG_INVALID", "Grok 渠道未配置 API 地址或 API Key");
        }
        long start = System.currentTimeMillis();
        try (HttpResponse response = HttpRequest.post(resolveSubmitEndpoint(reqDTO))
                .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                .contentType(ContentType.JSON.getValue())
                .body(buildSubmitBody(reqDTO).toString())
                .timeout(timeoutMillis(reqDTO))
                .execute()) {
            if (!response.isOk()) {
                return failed("HTTP_" + response.getStatus(), safeBody(response.body())).setDurationMillis(System.currentTimeMillis() - start);
            }
            AigcProviderSubmitRespDTO result = isVideo(reqDTO)
                    ? parseVideoSubmitResponse(response.body())
                    : parseImageResponse(response.body());
            return result.setDurationMillis(System.currentTimeMillis() - start);
        } catch (Exception ex) {
            return failed("REQUEST_EXCEPTION", ex.getMessage()).setDurationMillis(System.currentTimeMillis() - start);
        }
    }

    @Override
    public AigcProviderSubmitRespDTO query(String providerTaskId) {
        return failed("QUERY_CONTEXT_REQUIRED", "Grok 视频查询需要渠道配置上下文");
    }

    @Override
    public AigcProviderSubmitRespDTO query(AigcProviderSubmitReqDTO reqDTO) {
        if (StrUtil.isBlank(reqDTO.getProviderBaseUrl()) || StrUtil.isBlank(reqDTO.getProviderApiKey())) {
            return failed("CONFIG_INVALID", "Grok 渠道未配置 API 地址或 API Key");
        }
        if (StrUtil.isBlank(reqDTO.getProviderTaskId())) {
            return failed("TASK_ID_EMPTY", "Grok 视频查询缺少任务编号");
        }
        long start = System.currentTimeMillis();
        try (HttpResponse response = HttpRequest.get(resolveVideoTaskEndpoint(reqDTO, false))
                .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                .timeout(timeoutMillis(reqDTO))
                .execute()) {
            if (!response.isOk()) {
                return failed("HTTP_" + response.getStatus(), safeBody(response.body())).setDurationMillis(System.currentTimeMillis() - start);
            }
            return parseVideoQueryResponse(response.body(), reqDTO).setDurationMillis(System.currentTimeMillis() - start);
        } catch (Exception ex) {
            return failed("REQUEST_EXCEPTION", ex.getMessage()).setDurationMillis(System.currentTimeMillis() - start);
        }
    }

    @Override
    public boolean verifyCallback(AigcGenerateCallbackReqDTO reqDTO) {
        return true;
    }

    private JSONObject buildSubmitBody(AigcProviderSubmitReqDTO reqDTO) {
        JSONObject body = JSONUtil.createObj()
                .set("model", StrUtil.blankToDefault(reqDTO.getProviderModel(), reqDTO.getModelCode()))
                .set("prompt", StrUtil.blankToDefault(reqDTO.getPrompt(), ""));
        JSONObject params = parseParams(reqDTO.getInputParams());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value != null && !isInternalParam(key) && (!isVideo(reqDTO) || !isVideoParam(key))) {
                body.set(key, value);
            }
        }
        body.set("model", StrUtil.blankToDefault(reqDTO.getProviderModel(), reqDTO.getModelCode()));
        body.set("prompt", StrUtil.blankToDefault(reqDTO.getPrompt(), ""));
        if (isVideo(reqDTO)) {
            String providerImage = null;
            String image = firstNonBlank(params.getStr("image_url"), params.getStr("image"), firstInputImage(params));
            if (StrUtil.isNotBlank(image)) {
                providerImage = toProviderImage(image, reqDTO);
                body.set("images", new JSONArray().put(providerImage).put(providerImage));
            }
            body.set("seconds", resolveSeconds(params));
            body.set("size", resolveVideoSize(params, providerImage));
        } else {
            body.set("n", params.getInt("n", 1));
        }
        return body;
    }

    private boolean isInternalParam(String key) {
        return "providerModel".equals(key)
                || "referenceImages".equals(key)
                || "referenceImageIds".equals(key)
                || "inputImages".equals(key)
                || "inputImageIds".equals(key)
                || "inputImageUrls".equals(key);
    }

    private boolean isVideoParam(String key) {
        return "duration".equals(key)
                || "seconds".equals(key)
                || "resolution".equals(key)
                || "ratio".equals(key)
                || "aspectRatio".equals(key)
                || "aspect_ratio".equals(key)
                || "size".equals(key)
                || "image".equals(key)
                || "image_url".equals(key)
                || "images".equals(key);
    }

    private String firstInputImage(JSONObject params) {
        JSONArray referenceImages = params.getJSONArray("referenceImages");
        if (referenceImages != null && !referenceImages.isEmpty()) {
            return referenceImages.getStr(0);
        }
        JSONArray inputImages = params.getJSONArray("inputImages");
        if (inputImages != null && !inputImages.isEmpty()) {
            JSONObject image = JSONUtil.parseObj(inputImages.get(0));
            return firstNonBlank(image.getStr("url"), image.getStr("dataUrl"));
        }
        JSONArray inputImageUrls = params.getJSONArray("inputImageUrls");
        if (inputImageUrls != null && !inputImageUrls.isEmpty()) {
            return inputImageUrls.getStr(0);
        }
        return null;
    }

    private String toProviderImage(String image, AigcProviderSubmitReqDTO reqDTO) {
        if (StrUtil.startWithIgnoreCase(image, "data:")) {
            return image;
        }
        if (!AigcGenerateFileSecurityUtils.isSafeRemoteUrl(image)) {
            throw new IllegalArgumentException("Grok 首帧图片 URL 不安全");
        }
        try (HttpResponse response = HttpRequest.get(image)
                .timeout(timeoutMillis(reqDTO))
                .execute()) {
            if (!response.isOk()) {
                throw new IllegalStateException("Grok 首帧图片下载失败: HTTP_" + response.getStatus());
            }
            byte[] content = response.bodyBytes();
            if (content == null || content.length == 0) {
                throw new IllegalStateException("Grok 首帧图片下载结果为空");
            }
            String contentType = StrUtil.blankToDefault(response.header(Header.CONTENT_TYPE.getValue()), "image/jpeg");
            if (contentType.contains(";")) {
                contentType = StrUtil.subBefore(contentType, ";", false);
            }
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(content);
        }
    }

    private String resolveSeconds(JSONObject params) {
        String seconds = params.getStr("seconds");
        if (seconds != null && List.of("6", "10", "15").contains(seconds)) {
            return seconds;
        }
        int duration = params.getInt("duration", 6);
        if (duration <= 6) {
            return "6";
        }
        if (duration <= 10) {
            return "10";
        }
        return "15";
    }

    private String resolveVideoSize(JSONObject params, String image) {
        String size = firstNonBlank(params.getStr("size"), params.getStr("grokSize"));
        if (StrUtil.isNotBlank(size)) {
            return size.replace("*", "x");
        }
        String ratio = firstNonBlank(params.getStr("ratio"), params.getStr("aspectRatio"), params.getStr("aspect_ratio"), resolveImageRatio(image), "16:9");
        String resolution = params.getStr("resolution", "720p");
        int longSide = "1080p".equalsIgnoreCase(resolution) ? 1920 : 1280;
        return switch (ratio) {
            case "9:16" -> longSide == 1920 ? "1080x1920" : "720x1280";
            case "3:4" -> longSide == 1920 ? "1440x1920" : "960x1280";
            case "1:1" -> longSide == 1920 ? "1080x1080" : "720x720";
            case "4:3" -> longSide == 1920 ? "1920x1440" : "1280x960";
            case "21:9" -> longSide == 1920 ? "1920x823" : "1280x549";
            default -> longSide == 1920 ? "1920x1080" : "1280x720";
        };
    }

    private String resolveImageRatio(String image) {
        if (StrUtil.isBlank(image) || !StrUtil.startWithIgnoreCase(image, "data:")) {
            return null;
        }
        try {
            int commaIndex = image.indexOf(',');
            if (commaIndex < 0) {
                return null;
            }
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(image.substring(commaIndex + 1))));
            if (bufferedImage == null || bufferedImage.getWidth() <= 0 || bufferedImage.getHeight() <= 0) {
                return null;
            }
            return closestRatio(bufferedImage.getWidth(), bufferedImage.getHeight());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String closestRatio(int width, int height) {
        double ratio = (double) width / height;
        if (Math.abs(ratio - 1D) < 0.08D) {
            return "1:1";
        }
        if (ratio < 0.68D) {
            return "9:16";
        }
        if (ratio < 0.9D) {
            return "3:4";
        }
        if (ratio < 1.5D) {
            return "4:3";
        }
        if (ratio > 2D) {
            return "21:9";
        }
        return "16:9";
    }

    private AigcProviderSubmitRespDTO parseImageResponse(String body) {
        JSONObject json = JSONUtil.parseObj(body);
        JSONArray data = json.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return failed("EMPTY_DATA", "Grok 图片返回结果为空");
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
                urls.add("data:image/jpeg;base64," + b64Json);
            }
        }
        if (urls.isEmpty()) {
            return failed("EMPTY_URL", "Grok 图片未返回 URL");
        }
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(json.getStr("id", "GROKIMG" + System.currentTimeMillis()))
                .setProviderStatus("SUCCESS")
                .setOutputData(body)
                .setOutputUrls(urls.toString())
                .setFinished(true)
                .setSuccess(true);
    }

    private AigcProviderSubmitRespDTO parseVideoSubmitResponse(String body) {
        JSONObject json = JSONUtil.parseObj(body);
        String taskId = firstNonBlank(json.getStr("task_id"), json.getStr("id"));
        if (StrUtil.isBlank(taskId)) {
            return failed("TASK_ID_EMPTY", "Grok 视频提交未返回任务编号");
        }
        String status = json.getStr("status", "queued");
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(taskId)
                .setProviderStatus(status)
                .setOutputData(body)
                .setFinished(isCompleted(status))
                .setSuccess(true)
                .setOutputUrls(null);
    }

    private AigcProviderSubmitRespDTO parseVideoQueryResponse(String body, AigcProviderSubmitReqDTO reqDTO) {
        JSONObject json = JSONUtil.parseObj(body);
        String taskId = firstNonBlank(json.getStr("task_id"), json.getStr("id"), reqDTO.getProviderTaskId());
        String status = json.getStr("status", "queued");
        if (isFailed(status)) {
            return failed("PROVIDER_FAILED", firstNonBlank(json.getStr("error"), json.getStr("message"), "Grok 视频生成失败"))
                    .setProviderTaskId(taskId)
                    .setProviderStatus(status)
                    .setOutputData(body);
        }
        boolean completed = isCompleted(status);
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(taskId)
                .setProviderStatus(status)
                .setOutputData(body)
                .setOutputUrls(completed ? new JSONArray().put(downloadVideoAsDataUrl(reqDTO)).toString() : null)
                .setFinished(completed)
                .setSuccess(true);
    }

    private String downloadVideoAsDataUrl(AigcProviderSubmitReqDTO reqDTO) {
        try (HttpResponse response = HttpRequest.get(resolveVideoTaskEndpoint(reqDTO, true))
                .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                .timeout(timeoutMillis(reqDTO))
                .execute()) {
            if (!response.isOk()) {
                throw new IllegalStateException("Grok 视频下载失败: HTTP_" + response.getStatus());
            }
            byte[] content = response.bodyBytes();
            if (content == null || content.length == 0) {
                throw new IllegalStateException("Grok 视频下载结果为空");
            }
            String contentType = StrUtil.blankToDefault(response.header(Header.CONTENT_TYPE.getValue()), "video/mp4");
            if (contentType.contains(";")) {
                contentType = StrUtil.subBefore(contentType, ";", false);
            }
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(content);
        }
    }

    private String resolveSubmitEndpoint(AigcProviderSubmitReqDTO reqDTO) {
        String baseUrl = StrUtil.removeSuffix(reqDTO.getProviderBaseUrl(), "/");
        if (isVideo(reqDTO)) {
            return baseUrl.endsWith("/videos") ? baseUrl : baseUrl + "/videos";
        }
        return baseUrl.endsWith("/images/generations") ? baseUrl : baseUrl + "/images/generations";
    }

    private String resolveVideoTaskEndpoint(AigcProviderSubmitReqDTO reqDTO, boolean content) {
        return resolveContentEndpoint(reqDTO.getProviderTaskId(), reqDTO.getProviderBaseUrl()) + (content ? "/content" : "");
    }

    private String resolveContentEndpoint(String providerTaskId, String baseUrl) {
        String base = StrUtil.removeSuffix(StrUtil.blankToDefault(baseUrl, ""), "/");
        if (base.endsWith("/videos/" + providerTaskId)) {
            return base;
        }
        if (base.endsWith("/videos")) {
            return base + "/" + providerTaskId;
        }
        return base + "/videos/" + providerTaskId;
    }

    private boolean isVideo(AigcProviderSubmitReqDTO reqDTO) {
        return "VIDEO".equals(reqDTO.getGenerateType()) || "IMAGE_TO_VIDEO".equals(reqDTO.getGenerateMode()) || "TEXT_TO_VIDEO".equals(reqDTO.getGenerateMode());
    }

    private boolean isCompleted(String status) {
        return "completed".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status) || "succeeded".equalsIgnoreCase(status);
    }

    private boolean isFailed(String status) {
        return "failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status);
    }

    private JSONObject parseParams(String inputParams) {
        if (StrUtil.isBlank(inputParams) || !JSONUtil.isTypeJSON(inputParams)) {
            return JSONUtil.createObj();
        }
        return JSONUtil.parseObj(inputParams);
    }

    private int timeoutMillis(AigcProviderSubmitReqDTO reqDTO) {
        return (reqDTO.getProviderTimeoutSeconds() == null ? 60 : reqDTO.getProviderTimeoutSeconds()) * 1000;
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
