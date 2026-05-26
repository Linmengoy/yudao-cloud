package cn.iocoder.yudao.module.aigc.gen.framework.client;

import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitRespDTO;

public interface AigcProviderClient {

    String getProviderCode();

    AigcProviderSubmitRespDTO submit(AigcProviderSubmitReqDTO reqDTO);

    AigcProviderSubmitRespDTO query(String providerTaskId);

    boolean verifyCallback(AigcGenerateCallbackReqDTO reqDTO);

}
