package cn.iocoder.yudao.module.aigc.billing.service.statistics;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcCostRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcBillingRecordMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcCostRecordMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcWalletMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AigcBillingStatisticsServiceImpl implements AigcBillingStatisticsService {

    @Resource
    private AigcWalletMapper walletMapper;
    @Resource
    private AigcBillingRecordMapper billingRecordMapper;
    @Resource
    private AigcCostRecordMapper costRecordMapper;

    @Override
    public Map<String, Object> getOverview(LocalDateTime startTime, LocalDateTime endTime) {
        List<AigcWalletDO> wallets = walletMapper.selectList();
        List<AigcCostRecordDO> costs = costRecordMapper.selectList(new LambdaQueryWrapperX<AigcCostRecordDO>()
                .betweenIfPresent(AigcCostRecordDO::getCreateTime, startTime, endTime));
        
        BigDecimal totalBalance = wallets.stream()
                .map(AigcWalletDO::getBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFrozen = wallets.stream()
                .map(AigcWalletDO::getFrozenBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalConsume = wallets.stream()
                .map(AigcWalletDO::getTotalConsume)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRecharge = wallets.stream()
                .map(AigcWalletDO::getTotalRecharge)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCost = costs.stream()
                .map(AigcCostRecordDO::getCostAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSale = costs.stream()
                .map(AigcCostRecordDO::getSaleAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = costs.stream()
                .map(AigcCostRecordDO::getGrossProfit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return Map.of(
                "walletCount", wallets.size(),
                "totalBalance", totalBalance,
                "totalFrozenBalance", totalFrozen,
                "totalRecharge", totalRecharge,
                "totalConsume", totalConsume,
                "totalCost", totalCost,
                "totalSale", totalSale,
                "totalGrossProfit", totalProfit
        );
    }

    @Override
    public List<Map<String, Object>> getDailyStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return costRecordMapper.selectList(new LambdaQueryWrapperX<AigcCostRecordDO>()
                        .betweenIfPresent(AigcCostRecordDO::getCreateTime, startTime, endTime)).stream()
                .filter(record -> record.getCreateTime() != null)
                .collect(Collectors.groupingBy(record -> record.getCreateTime().toLocalDate()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    BigDecimal saleAmount = entry.getValue().stream()
                            .map(AigcCostRecordDO::getSaleAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal costAmount = entry.getValue().stream()
                            .map(AigcCostRecordDO::getCostAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal profit = entry.getValue().stream()
                            .map(AigcCostRecordDO::getGrossProfit)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return Map.<String, Object>of(
                            "date", entry.getKey(),
                            "saleAmount", saleAmount,
                            "costAmount", costAmount,
                            "grossProfit", profit
                    );
                })
                .toList();
    }

    @Override
    public List<Map<String, Object>> getModelStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return groupCostBy("model", startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> getProviderStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return groupCostBy("provider", startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> getUserRank(LocalDateTime startTime, LocalDateTime endTime, Integer limit) {
        int actualLimit = limit != null && limit > 0 ? limit : 20;
        return billingRecordMapper.selectList(new LambdaQueryWrapperX<AigcBillingRecordDO>()
                        .betweenIfPresent(AigcBillingRecordDO::getCreateTime, startTime, endTime)).stream()
                .filter(record -> record.getUserId() != null)
                .collect(Collectors.groupingBy(AigcBillingRecordDO::getUserId))
                .entrySet().stream()
                .map(entry -> {
                    BigDecimal consume = entry.getValue().stream()
                            .map(AigcBillingRecordDO::getAmount)
                            .filter(Objects::nonNull)
                            .filter(amount -> amount.signum() < 0)
                            .map(BigDecimal::abs)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return Map.<String, Object>of("userId", entry.getKey(), "consumeAmount", consume);
                })
                .sorted((left, right) -> ((BigDecimal) right.get("consumeAmount")).compareTo((BigDecimal) left.get("consumeAmount")))
                .limit(actualLimit)
                .toList();
    }

    private List<Map<String, Object>> groupCostBy(String type, LocalDateTime startTime, LocalDateTime endTime) {
        return costRecordMapper.selectList(new LambdaQueryWrapperX<AigcCostRecordDO>()
                        .betweenIfPresent(AigcCostRecordDO::getCreateTime, startTime, endTime)).stream()
                .collect(Collectors.groupingBy(record -> "model".equals(type) ? record.getModelId() : record.getProviderId()))
                .entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .map(entry -> {
                    BigDecimal saleAmount = entry.getValue().stream()
                            .map(AigcCostRecordDO::getSaleAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal costAmount = entry.getValue().stream()
                            .map(AigcCostRecordDO::getCostAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal profit = entry.getValue().stream()
                            .map(AigcCostRecordDO::getGrossProfit)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return Map.<String, Object>of(
                            type + "Id", entry.getKey(),
                            "saleAmount", saleAmount,
                            "costAmount", costAmount,
                            "grossProfit", profit
                    );
                })
                .sorted(Comparator.comparing(entry -> (Long) entry.get(type + "Id")))
                .toList();
    }

}
