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
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class GrokImagineProviderClient implements AigcProviderClient {

    private static final String PROVIDER_CODE = "grok";
    private static final List<String> VIDEO_ALLOWED_SIZES = List.of("1024x1024", "1024x1792", "1280x720", "1792x1024");
    private static final int GROK_IMAGE_MAX_DATA_URL_BYTES = 960 * 1024;
    private static final int GROK_IMAGE_MAX_EDGE = 1024;

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
        try (HttpResponse response = AigcProviderProxyUtils.execute(HttpRequest.post(resolveSubmitEndpoint(reqDTO))
                .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                .contentType(ContentType.JSON.getValue())
                .body(buildSubmitBody(reqDTO).toString())
                .timeout(timeoutMillis(reqDTO)), reqDTO)) {
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
        try (HttpResponse response = AigcProviderProxyUtils.execute(HttpRequest.get(resolveVideoTaskEndpoint(reqDTO, false))
                .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                .timeout(timeoutMillis(reqDTO)), reqDTO)) {
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
            if (isImageToImage(reqDTO)) {
                List<String> images = inputImages(params);
                if (images.isEmpty()) {
                    throw new IllegalArgumentException("Grok 图生图缺少参考图片");
                }
                body.remove("image_url");
                body.remove("image");
                body.remove("images");
                if (images.size() == 1) {
                    body.set("image", providerImageObject(images.get(0), reqDTO));
                } else {
                    JSONArray providerImages = new JSONArray();
                    images.forEach(image -> providerImages.add(providerImageObject(image, reqDTO)));
                    body.set("images", providerImages);
                }
            }
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

    private List<String> inputImages(JSONObject params) {
        List<String> images = new ArrayList<>();
        addIfNotBlank(images, params.getStr("image_url"));
        addIfNotBlank(images, params.getStr("image"));
        JSONArray referenceImages = params.getJSONArray("referenceImages");
        if (referenceImages != null) {
            for (Object item : referenceImages) {
                addIfNotBlank(images, String.valueOf(item));
            }
        }
        JSONArray inputImages = params.getJSONArray("inputImages");
        if (inputImages != null) {
            for (Object item : inputImages) {
                JSONObject image = JSONUtil.parseObj(item);
                addIfNotBlank(images, firstNonBlank(image.getStr("url"), image.getStr("dataUrl")));
            }
        }
        JSONArray inputImageUrls = params.getJSONArray("inputImageUrls");
        if (inputImageUrls != null) {
            for (Object item : inputImageUrls) {
                addIfNotBlank(images, String.valueOf(item));
            }
        }
        return images.size() <= 3 ? images : images.subList(0, 3);
    }

    private void addIfNotBlank(List<String> images, String image) {
        if (StrUtil.isNotBlank(image) && !images.contains(image)) {
            images.add(image);
        }
    }

    private JSONObject providerImageObject(String image, AigcProviderSubmitReqDTO reqDTO) {
        return JSONUtil.createObj()
                .set("url", toProviderEditImage(image, reqDTO));
    }

    private String toProviderEditImage(String image, AigcProviderSubmitReqDTO reqDTO) {
        SourceImage source = readSourceImage(image, reqDTO);
        String dataUrl = toDataUrl(source.contentType(), source.bytes());
        if (dataUrl.getBytes(StandardCharsets.UTF_8).length <= GROK_IMAGE_MAX_DATA_URL_BYTES) {
            return dataUrl;
        }
        return compressEditImage(source);
    }

    private String toProviderImage(String image, AigcProviderSubmitReqDTO reqDTO) {
        SourceImage source = readSourceImage(image, reqDTO);
        return toDataUrl(source.contentType(), source.bytes());
    }

    private SourceImage readSourceImage(String image, AigcProviderSubmitReqDTO reqDTO) {
        if (!AigcGenerateFileSecurityUtils.isSafeRemoteUrl(image)) {
            if (StrUtil.startWithIgnoreCase(image, "data:")) {
                return readDataUrlImage(image);
            }
            throw new IllegalArgumentException("Grok 参考图片 URL 不安全");
        }
        try (HttpResponse response = AigcProviderProxyUtils.execute(HttpRequest.get(image)
                .timeout(timeoutMillis(reqDTO)), reqDTO)) {
            if (!response.isOk()) {
                throw new IllegalStateException("Grok 参考图片下载失败: HTTP_" + response.getStatus());
            }
            byte[] content = response.bodyBytes();
            if (content == null || content.length == 0) {
                throw new IllegalStateException("Grok 参考图片下载结果为空");
            }
            String contentType = StrUtil.blankToDefault(response.header(Header.CONTENT_TYPE.getValue()), "image/jpeg");
            if (contentType.contains(";")) {
                contentType = StrUtil.subBefore(contentType, ";", false);
            }
            return new SourceImage(contentType, content);
        }
    }

    private SourceImage readDataUrlImage(String image) {
        int commaIndex = image.indexOf(',');
        if (commaIndex < 0) {
            throw new IllegalArgumentException("Grok 参考图片 data URL 格式错误");
        }
        String meta = image.substring(0, commaIndex);
        String payload = image.substring(commaIndex + 1);
        String contentType = StrUtil.subBetween(meta, "data:", ";");
        if (StrUtil.isBlank(contentType)) {
            contentType = "image/png";
        }
        byte[] content = meta.contains(";base64")
                ? Base64.getDecoder().decode(payload)
                : URLDecoder.decode(payload, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
        return new SourceImage(contentType, content);
    }

    private String compressEditImage(SourceImage source) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(source.bytes()));
            if (original == null) {
                return toDataUrl(source.contentType(), source.bytes());
            }
            BufferedImage image = scaleImage(original, GROK_IMAGE_MAX_EDGE);
            float[] qualities = new float[]{0.86F, 0.78F, 0.70F, 0.62F, 0.54F, 0.46F, 0.38F};
            String best = null;
            for (int scaleStep = 0; scaleStep < 4; scaleStep++) {
                for (float quality : qualities) {
                    byte[] jpeg = writeJpeg(image, quality);
                    String dataUrl = toDataUrl("image/jpeg", jpeg);
                    best = dataUrl;
                    if (dataUrl.getBytes(StandardCharsets.UTF_8).length <= GROK_IMAGE_MAX_DATA_URL_BYTES) {
                        return dataUrl;
                    }
                }
                image = scaleImage(image, Math.max(512, Math.round(Math.max(image.getWidth(), image.getHeight()) * 0.82F)));
            }
            return best == null ? toDataUrl(source.contentType(), source.bytes()) : best;
        } catch (Exception ex) {
            return toDataUrl(source.contentType(), source.bytes());
        }
    }

    private BufferedImage scaleImage(BufferedImage source, int maxEdge) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longest = Math.max(width, height);
        if (longest <= maxEdge) {
            return toRgbImage(source, width, height);
        }
        double scale = (double) maxEdge / longest;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        return toRgbImage(source, targetWidth, targetHeight);
    }

    private BufferedImage toRgbImage(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return target;
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws Exception {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("JPEG writer not found");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
            return outputStream.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private String toDataUrl(String contentType, byte[] content) {
        return "data:" + StrUtil.blankToDefault(contentType, "image/jpeg") + ";base64," + Base64.getEncoder().encodeToString(content);
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
            String normalizedSize = size.replace("*", "x");
            if (VIDEO_ALLOWED_SIZES.contains(normalizedSize)) {
                return normalizedSize;
            }
            String ratioFromSize = sizeToRatio(normalizedSize);
            if (ratioFromSize != null) {
                return grokVideoSizeForRatio(ratioFromSize);
            }
        }
        String ratio = firstNonBlank(params.getStr("ratio"), params.getStr("aspectRatio"), params.getStr("aspect_ratio"), resolveImageRatio(image), "16:9");
        return grokVideoSizeForRatio(ratio);
    }

    private String grokVideoSizeForRatio(String ratio) {
        return switch (ratio) {
            case "1:1" -> "1024x1024";
            case "9:16", "3:4", "2:3" -> "1024x1792";
            case "4:3", "3:2", "21:9" -> "1792x1024";
            default -> "1280x720";
        };
    }

    private String sizeToRatio(String size) {
        if (StrUtil.isBlank(size)) {
            return null;
        }
        String[] parts = size.toLowerCase().split("x");
        if (parts.length != 2) {
            return null;
        }
        try {
            int width = Integer.parseInt(parts[0].trim());
            int height = Integer.parseInt(parts[1].trim());
            if (width <= 0 || height <= 0) {
                return null;
            }
            return closestRatio(width, height);
        } catch (NumberFormatException ignored) {
            return null;
        }
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
        try (HttpResponse response = AigcProviderProxyUtils.execute(HttpRequest.get(resolveVideoTaskEndpoint(reqDTO, true))
                .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                .timeout(timeoutMillis(reqDTO)), reqDTO)) {
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
        String target = isImageToImage(reqDTO) ? "/images/edits" : "/images/generations";
        if (baseUrl.endsWith("/images/generations") || baseUrl.endsWith("/images/edits")) {
            return baseUrl.substring(0, baseUrl.lastIndexOf("/images/")) + target;
        }
        return baseUrl + target;
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

    private boolean isImageToImage(AigcProviderSubmitReqDTO reqDTO) {
        return "IMAGE".equals(reqDTO.getGenerateType()) && "IMAGE_TO_IMAGE".equals(reqDTO.getGenerateMode());
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

    private record SourceImage(String contentType, byte[] bytes) {
    }
}
