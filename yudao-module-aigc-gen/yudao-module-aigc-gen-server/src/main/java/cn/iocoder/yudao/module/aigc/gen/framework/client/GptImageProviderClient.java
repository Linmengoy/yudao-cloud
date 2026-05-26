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

import java.util.HashMap;
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
            JSONObject body = buildRequestBody(reqDTO);
            try (HttpResponse response = HttpRequest.post(reqDTO.getProviderBaseUrl())
                    .header(Header.AUTHORIZATION, "Bearer " + reqDTO.getProviderApiKey())
                    .contentType(ContentType.JSON.getValue())
                    .body(body.toString())
                    .timeout((reqDTO.getProviderTimeoutSeconds() == null ? 60 : reqDTO.getProviderTimeoutSeconds()) * 1000)
                    .execute()) {
                if (!response.isOk()) {
                    return failed("HTTP_" + response.getStatus(), safeBody(response.body())).setDurationMillis(System.currentTimeMillis() - start);
                }
                return parseResponse(response.body()).setDurationMillis(System.currentTimeMillis() - start);
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
        body.put("model", reqDTO.getModelCode());
        body.put("prompt", reqDTO.getPrompt());
        if (StrUtil.isNotBlank(reqDTO.getInputParams()) && JSONUtil.isTypeJSON(reqDTO.getInputParams())) {
            JSONObject params = JSONUtil.parseObj(reqDTO.getInputParams());
            params.forEach(body::put);
        }
        body.putIfAbsent("n", 1);
        return JSONUtil.parseObj(body);
    }

    private AigcProviderSubmitRespDTO parseResponse(String body) {
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
}
