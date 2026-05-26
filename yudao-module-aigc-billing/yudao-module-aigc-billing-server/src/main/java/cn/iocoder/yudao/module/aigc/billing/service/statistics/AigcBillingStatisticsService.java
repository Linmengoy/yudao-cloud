package cn.iocoder.yudao.module.aigc.billing.service.statistics;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AigcBillingStatisticsService {

    Map<String, Object> getOverview(LocalDateTime startTime, LocalDateTime endTime);

    List<Map<String, Object>> getDailyStatistics(LocalDateTime startTime, LocalDateTime endTime);

    List<Map<String, Object>> getModelStatistics(LocalDateTime startTime, LocalDateTime endTime);

    List<Map<String, Object>> getProviderStatistics(LocalDateTime startTime, LocalDateTime endTime);

    List<Map<String, Object>> getUserRank(LocalDateTime startTime, LocalDateTime endTime, Integer limit);

}
