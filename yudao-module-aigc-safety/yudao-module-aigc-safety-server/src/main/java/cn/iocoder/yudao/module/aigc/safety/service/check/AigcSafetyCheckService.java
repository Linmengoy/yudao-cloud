package cn.iocoder.yudao.module.aigc.safety.service.check;

import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckRespDTO;

public interface AigcSafetyCheckService {

    AigcSafetyPromptCheckRespDTO checkPrompt(AigcSafetyPromptCheckReqDTO reqDTO);

}
