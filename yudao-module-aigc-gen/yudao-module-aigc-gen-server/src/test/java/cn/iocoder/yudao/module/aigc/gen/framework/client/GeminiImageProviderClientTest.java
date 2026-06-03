package cn.iocoder.yudao.module.aigc.gen.framework.client;

import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitRespDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class GeminiImageProviderClientTest {

    @Test
    public void testSubmit_withoutApiKey() {
        GeminiImageProviderClient client = new GeminiImageProviderClient();

        AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                .setProviderBaseUrl("https://example.com/v1beta")
                .setModelCode("gemini-2.5-flash-image")
                .setPrompt("测试图片")
                .setGenerateType("IMAGE")
                .setGenerateMode("TEXT_TO_IMAGE"));

        assertFalse(respDTO.getSuccess());
    }
}
