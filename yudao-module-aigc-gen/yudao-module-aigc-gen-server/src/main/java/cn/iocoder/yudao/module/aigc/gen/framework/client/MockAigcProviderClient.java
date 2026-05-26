package cn.iocoder.yudao.module.aigc.gen.framework.client;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitRespDTO;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MockAigcProviderClient implements AigcProviderClient {

    @Override
    public String getProviderCode() {
        return "mock";
    }

    @Override
    public AigcProviderSubmitRespDTO submit(AigcProviderSubmitReqDTO reqDTO) {
        boolean sync = Boolean.TRUE.equals(reqDTO.getSync()) || "TEXT".equals(reqDTO.getGenerateType()) || "CODE".equals(reqDTO.getGenerateType());
        String providerTaskId = "MOCK" + IdUtil.getSnowflakeNextIdStr();
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(providerTaskId)
                .setProviderStatus(sync ? "SUCCESS" : "SUBMITTED")
                .setOutputText(sync ? buildOutputText(reqDTO) : null)
                .setOutputData(sync ? "{\"providerTaskId\":\"" + providerTaskId + "\"}" : null)
                .setOutputUrls(sync ? null : "[\"https://example.com/aigc/" + providerTaskId + "\"]")
                .setPromptTokens(reqDTO.getPrompt() == null ? 0L : (long) reqDTO.getPrompt().length())
                .setCompletionTokens(sync ? 64L : 0L)
                .setTotalTokens((reqDTO.getPrompt() == null ? 0L : (long) reqDTO.getPrompt().length()) + (sync ? 64L : 0L))
                .setDurationMillis(100L)
                .setFinished(sync)
                .setSuccess(true);
    }

    @Override
    public AigcProviderSubmitRespDTO query(String providerTaskId) {
        return new AigcProviderSubmitRespDTO()
                .setProviderTaskId(providerTaskId)
                .setProviderStatus("SUCCESS")
                .setOutputData("{\"providerTaskId\":\"" + providerTaskId + "\"}")
                .setOutputUrls("[\"https://example.com/aigc/" + providerTaskId + "\"]")
                .setDurationMillis(100L)
                .setFinished(true)
                .setSuccess(true);
    }

    @Override
    public boolean verifyCallback(AigcGenerateCallbackReqDTO reqDTO) {
        return reqDTO.getSignature() == null || Objects.equals(reqDTO.getSignature(), "mock") || Objects.equals(reqDTO.getSignature(), reqDTO.getProviderTaskId());
    }

    private String buildOutputText(AigcProviderSubmitReqDTO reqDTO) {
        return "模拟生成结果：" + (reqDTO.getPrompt() == null ? reqDTO.getGenerateMode() : reqDTO.getPrompt());
    }
}
