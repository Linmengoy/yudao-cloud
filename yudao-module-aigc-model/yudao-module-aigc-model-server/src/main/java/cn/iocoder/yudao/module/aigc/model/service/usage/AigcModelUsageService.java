package cn.iocoder.yudao.module.aigc.model.service.usage;

import cn.iocoder.yudao.module.aigc.model.dto.AigcModelUsageRecordReqDTO;

public interface AigcModelUsageService {

    Long recordUsage(AigcModelUsageRecordReqDTO reqDTO);

}
