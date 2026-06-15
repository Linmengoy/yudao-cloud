package cn.iocoder.yudao.module.aigc.model.service.usage;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.usage.vo.AigcModelUsagePageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.usage.vo.AigcModelUsageTypeStatisticsRespVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelUsageLogDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelUsageLogMapper;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelUsageRecordReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public AigcModelUsageLogDO getUsageLog(Long id) {
        return usageLogMapper.selectById(id);
    }

    @Override
    public PageResult<AigcModelUsageLogDO> getUsageLogPage(AigcModelUsagePageReqVO reqVO) {
        return usageLogMapper.selectPage(reqVO);
    }

    @Override
    public List<AigcModelUsageTypeStatisticsRespVO> getUsageTypeStatistics(AigcModelUsagePageReqVO reqVO) {
        int topN = reqVO.getTopN() == null ? 10 : Math.max(1, Math.min(reqVO.getTopN(), 50));
        List<AigcModelUsageTypeStatisticsRespVO> statistics = usageLogMapper.selectTypeStatistics(reqVO);
        Map<String, List<AigcModelUsageTypeStatisticsRespVO>> statisticsByDimension = statistics.stream()
                .collect(Collectors.groupingBy(AigcModelUsageTypeStatisticsRespVO::getDimensionType));
        List<AigcModelUsageTypeStatisticsRespVO> result = new ArrayList<>();
        result.addAll(statisticsByDimension.getOrDefault("MODEL_TYPE", List.of()));
        result.addAll(statisticsByDimension.getOrDefault("CAPABILITY", List.of()));
        result.addAll(statisticsByDimension.getOrDefault("MODEL_TOP", List.of()).stream()
                .sorted(Comparator.comparing(AigcModelUsageTypeStatisticsRespVO::getUsageCount,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(topN)
                .toList());
        result.addAll(statisticsByDimension.getOrDefault("FAILURE_RATE", List.of()).stream()
                .sorted(Comparator.comparing(AigcModelUsageTypeStatisticsRespVO::getFailureRate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AigcModelUsageTypeStatisticsRespVO::getUsageCount,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(topN)
                .toList());
        return result;
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
