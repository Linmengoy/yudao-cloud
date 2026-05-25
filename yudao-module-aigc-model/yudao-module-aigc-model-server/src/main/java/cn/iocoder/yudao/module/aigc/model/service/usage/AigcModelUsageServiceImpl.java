package cn.iocoder.yudao.module.aigc.model.service.usage;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelUsageLogDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelUsageLogMapper;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelUsageRecordReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class AigcModelUsageServiceImpl implements AigcModelUsageService {

    @Resource
    private AigcModelUsageLogMapper usageLogMapper;

    @Override
    public Long recordUsage(AigcModelUsageRecordReqDTO reqDTO) {
        AigcModelUsageLogDO usageLog = BeanUtils.toBean(reqDTO, AigcModelUsageLogDO.class);
        usageLog.setTraceId(StrUtil.blankToDefault(reqDTO.getTraceId(), TracerUtils.getTraceId()));
        if (usageLog.getTotalTokens() == null) {
            usageLog.setTotalTokens(sumTokens(usageLog.getPromptTokens(), usageLog.getCompletionTokens(), usageLog.getInputTokens(), usageLog.getOutputTokens()));
        }
        usageLogMapper.insert(usageLog);
        return usageLog.getId();
    }

    private Long sumTokens(Long promptTokens, Long completionTokens, Long inputTokens, Long outputTokens) {
        long total = 0L;
        total += promptTokens == null ? 0L : promptTokens;
        total += completionTokens == null ? 0L : completionTokens;
        total += inputTokens == null ? 0L : inputTokens;
        total += outputTokens == null ? 0L : outputTokens;
        return total == 0L ? null : total;
    }

}
