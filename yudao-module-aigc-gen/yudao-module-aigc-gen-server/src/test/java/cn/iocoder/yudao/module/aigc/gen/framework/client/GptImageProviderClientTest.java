package cn.iocoder.yudao.module.aigc.gen.framework.client;

import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitRespDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class GptImageProviderClientTest {

    @Test
    public void testSubmit_withoutApiKey() {
        GptImageProviderClient client = new GptImageProviderClient();

        AigcProviderSubmitRespDTO respDTO = client.submit(new AigcProviderSubmitReqDTO()
                .setProviderBaseUrl("https://copse.top/v1/images/generations")
                .setPrompt("测试图片")
                .setGenerateType("IMAGE")
                .setGenerateMode("TEXT_TO_IMAGE"));

        assertFalse(respDTO.getSuccess());
    }
}
