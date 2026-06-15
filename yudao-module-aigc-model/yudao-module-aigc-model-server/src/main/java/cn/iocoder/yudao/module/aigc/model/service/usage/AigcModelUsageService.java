package cn.iocoder.yudao.module.aigc.model.service.usage;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.model.controller.admin.usage.vo.AigcModelUsagePageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.usage.vo.AigcModelUsageTypeStatisticsRespVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelUsageLogDO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelUsageRecordReqDTO;

import java.util.List;

public interface AigcModelUsageService {

    Long recordUsage(AigcModelUsageRecordReqDTO reqDTO);

    AigcModelUsageLogDO getUsageLog(Long id);

    PageResult<AigcModelUsageLogDO> getUsageLogPage(AigcModelUsagePageReqVO reqVO);

    List<AigcModelUsageTypeStatisticsRespVO> getUsageTypeStatistics(AigcModelUsagePageReqVO reqVO);

}
