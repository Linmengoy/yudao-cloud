package cn.iocoder.yudao.module.aigc.gen.service.media;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.aigc.gen.framework.security.AigcGenerateFileSecurityUtils;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AigcMediaArchiveService {

    private static final String INPUT_DIRECTORY = "aigc/input";
    private static final String OUTPUT_DIRECTORY = "aigc/output";
    private static final int REMOTE_DOWNLOAD_TIMEOUT_MILLIS = 30_000;

    @Resource
    private FileApi fileApi;

    public String archiveInputParams(String inputParams) {
        if (StrUtil.isBlank(inputParams) || !JSONUtil.isTypeJSONObject(inputParams)) {
            return inputParams;
        }
        JSONObject params = JSONUtil.parseObj(inputParams);
        Map<String, String> cache = new HashMap<>();
        archiveNamedMediaValue(params, "image", INPUT_DIRECTORY, cache);
        archiveNamedMediaValue(params, "image_url", INPUT_DIRECTORY, cache);
        archiveNamedMediaArray(params, "referenceImages", INPUT_DIRECTORY, cache);
        archiveNamedMediaArray(params, "inputImageUrls", INPUT_DIRECTORY, cache);
        archiveNamedMediaArray(params, "image_urls", INPUT_DIRECTORY, cache);
        archiveInputImageObjects(params, cache);
        return params.toString();
    }

    public String archiveOutputData(String outputData) {
        if (StrUtil.isBlank(outputData) || !JSONUtil.isTypeJSON(outputData)) {
            return outputData;
        }
        Object json = JSONUtil.parse(outputData);
        Map<String, String> cache = new HashMap<>();
        archiveOutputJsonValue(null, json, cache);
        return JSONUtil.toJsonStr(json);
    }

    private void archiveInputImageObjects(JSONObject params, Map<String, String> cache) {
        JSONArray inputImages = params.getJSONArray("inputImages");
        if (inputImages == null || inputImages.isEmpty()) {
            return;
        }
        JSONArray archivedImages = new JSONArray();
        for (Object item : inputImages) {
            JSONObject image = JSONUtil.parseObj(item);
            String source = StrUtil.blankToDefault(image.getStr("url"), image.getStr("dataUrl"));
            String archivedUrl = archiveMediaSource(source, INPUT_DIRECTORY, cache);
            if (StrUtil.isNotBlank(archivedUrl)) {
                image.set("url", archivedUrl);
            }
            image.remove("dataUrl");
            archivedImages.add(image);
        }
        params.set("inputImages", archivedImages);
    }

    private void archiveNamedMediaValue(JSONObject object, String key, String directory, Map<String, String> cache) {
        String value = object.getStr(key);
        String archivedUrl = archiveMediaSource(value, directory, cache);
        if (StrUtil.isNotBlank(archivedUrl)) {
            object.set(key, archivedUrl);
        }
    }

    private void archiveNamedMediaArray(JSONObject object, String key, String directory, Map<String, String> cache) {
        JSONArray array = object.getJSONArray(key);
        if (array == null || array.isEmpty()) {
            return;
        }
        JSONArray archivedArray = new JSONArray();
        for (Object item : array) {
            if (item instanceof String source) {
                String archivedUrl = archiveMediaSource(source, directory, cache);
                archivedArray.add(StrUtil.blankToDefault(archivedUrl, source));
            } else {
                archivedArray.add(item);
            }
        }
        object.set(key, archivedArray);
    }

    private void archiveOutputJsonValue(String key, Object value, Map<String, String> cache) {
        if (value instanceof JSONObject object) {
            for (String childKey : object.keySet().toArray(new String[0])) {
                Object child = object.get(childKey);
                if (child instanceof String source && isLikelyMediaOutputKey(childKey)) {
                    String archivedUrl = archiveMediaSource(source, OUTPUT_DIRECTORY, cache);
                    if (StrUtil.isNotBlank(archivedUrl)) {
                        object.set(childKey, archivedUrl);
                    }
                } else {
                    archiveOutputJsonValue(childKey, child, cache);
                }
            }
            return;
        }
        if (value instanceof JSONArray array) {
            for (int i = 0; i < array.size(); i++) {
                Object child = array.get(i);
                if (child instanceof String source && isLikelyMediaOutputKey(key)) {
                    String archivedUrl = archiveMediaSource(source, OUTPUT_DIRECTORY, cache);
                    if (StrUtil.isNotBlank(archivedUrl)) {
                        array.set(i, archivedUrl);
                    }
                } else {
                    archiveOutputJsonValue(key, child, cache);
                }
            }
        }
    }

    private boolean isLikelyMediaOutputKey(String key) {
        String normalized = StrUtil.nullToEmpty(key).toLowerCase();
        return normalized.contains("url") || normalized.contains("image") || normalized.contains("video")
                || normalized.contains("audio");
    }

    private String archiveMediaSource(String source, String directory, Map<String, String> cache) {
        if (StrUtil.isBlank(source)) {
            return null;
        }
        String trimmed = source.trim();
        if (cache.containsKey(trimmed)) {
            return cache.get(trimmed);
        }
        ArchivedMedia media = null;
        if (StrUtil.startWithIgnoreCase(trimmed, "data:")) {
            media = decodeDataUrl(trimmed);
        } else if (AigcGenerateFileSecurityUtils.isSafeRemoteUrl(trimmed)) {
            media = downloadRemoteMedia(trimmed);
        }
        if (media == null || media.content().length == 0) {
            return null;
        }
        String archivedUrl = fileApi.createFile(media.content(), archiveFileName(media.mimeType()), directory,
                media.mimeType());
        cache.put(trimmed, archivedUrl);
        return archivedUrl;
    }

    private ArchivedMedia decodeDataUrl(String dataUrl) {
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex <= 5 || !dataUrl.substring(0, commaIndex).contains(";base64")) {
            return null;
        }
        String mimeType = dataUrl.substring("data:".length(), dataUrl.indexOf(";base64"));
        if (!isSupportedMediaType(mimeType)) {
            return null;
        }
        try {
            return new ArchivedMedia(Base64.getDecoder().decode(dataUrl.substring(commaIndex + 1)), mimeType);
        } catch (IllegalArgumentException ex) {
            log.warn("[decodeDataUrl][invalid data url]");
            return null;
        }
    }

    private ArchivedMedia downloadRemoteMedia(String url) {
        try (HttpResponse response = HttpRequest.get(url)
                .header(Header.USER_AGENT, "manman-aigc-media-archiver/1.0")
                .header(Header.ACCEPT, "image/*,video/*,audio/*,*/*;q=0.5")
                .timeout(REMOTE_DOWNLOAD_TIMEOUT_MILLIS)
                .execute()) {
            if (!response.isOk()) {
                log.warn("[downloadRemoteMedia][url({}) failed status({})]", url, response.getStatus());
                return null;
            }
            String mimeType = StrUtil.subBefore(response.header(Header.CONTENT_TYPE.getValue()), ";", false);
            if (!isSupportedMediaType(mimeType)) {
                mimeType = mimeTypeFromUrl(url);
            }
            if (!isSupportedMediaType(mimeType)) {
                log.warn("[downloadRemoteMedia][url({}) unsupported content type({})]", url, mimeType);
                return null;
            }
            return new ArchivedMedia(response.bodyBytes(), mimeType);
        } catch (Exception ex) {
            log.warn("[downloadRemoteMedia][url({}) failed]", url, ex);
            return null;
        }
    }

    private boolean isSupportedMediaType(String mimeType) {
        return StrUtil.startWithAnyIgnoreCase(StrUtil.nullToEmpty(mimeType), "image/", "video/", "audio/");
    }

    private String archiveFileName(String mimeType) {
        return "aigc-media-" + IdUtil.getSnowflakeNextIdStr() + "." + fileExtFromMimeType(mimeType);
    }

    private String fileExtFromMimeType(String mimeType) {
        if (StrUtil.equalsIgnoreCase(mimeType, "image/jpeg")) {
            return "jpg";
        }
        if (StrUtil.startWithIgnoreCase(mimeType, "image/")) {
            return StrUtil.subAfter(mimeType, "image/", true);
        }
        if (StrUtil.startWithIgnoreCase(mimeType, "video/")) {
            return StrUtil.subAfter(mimeType, "video/", true);
        }
        if (StrUtil.startWithIgnoreCase(mimeType, "audio/")) {
            return StrUtil.subAfter(mimeType, "audio/", true);
        }
        return "bin";
    }

    private String mimeTypeFromUrl(String url) {
        String path;
        try {
            path = URI.create(url).getPath();
        } catch (IllegalArgumentException ex) {
            path = url;
        }
        String ext = StrUtil.subAfter(StrUtil.nullToEmpty(path), ".", true).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            default -> null;
        };
    }

    private record ArchivedMedia(byte[] content, String mimeType) {
    }

}
