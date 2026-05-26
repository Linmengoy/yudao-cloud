package cn.iocoder.yudao.module.aigc.billing.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcQuotaFreezeDO;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingFreezeStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AigcQuotaFreezeMapper extends BaseMapperX<AigcQuotaFreezeDO> {

    default AigcQuotaFreezeDO selectByBiz(String bizType, String bizId) {
        return selectOne(AigcQuotaFreezeDO::getBizType, bizType, AigcQuotaFreezeDO::getBizId, bizId);
    }

    default PageResult<AigcQuotaFreezeDO> selectPage(PageParam reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcQuotaFreezeDO>()
                .orderByDesc(AigcQuotaFreezeDO::getId));
    }

    default PageResult<AigcQuotaFreezeDO> selectUserPage(PageParam reqVO, Long userId) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcQuotaFreezeDO>()
                .eq(AigcQuotaFreezeDO::getUserId, userId)
                .orderByDesc(AigcQuotaFreezeDO::getId));
    }

    default int updateConfirmed(Long id, BigDecimal amount, Long taskId, String taskNo, LocalDateTime confirmTime) {
        return update(null, new LambdaUpdateWrapper<AigcQuotaFreezeDO>()
                .set(AigcQuotaFreezeDO::getStatus, AigcBillingFreezeStatusEnum.CONFIRMED.getCode())
                .set(AigcQuotaFreezeDO::getConfirmedAmount, amount)
                .set(AigcQuotaFreezeDO::getConfirmTime, confirmTime)
                .set(taskId != null, AigcQuotaFreezeDO::getTaskId, taskId)
                .set(taskNo != null, AigcQuotaFreezeDO::getTaskNo, taskNo)
                .eq(AigcQuotaFreezeDO::getId, id)
                .eq(AigcQuotaFreezeDO::getStatus, AigcBillingFreezeStatusEnum.FROZEN.getCode()));
    }

    default int updateReleased(Long id, BigDecimal amount, Long taskId, String taskNo, String reason, LocalDateTime releaseTime) {
        return update(null, new LambdaUpdateWrapper<AigcQuotaFreezeDO>()
                .set(AigcQuotaFreezeDO::getStatus, AigcBillingFreezeStatusEnum.RELEASED.getCode())
                .set(AigcQuotaFreezeDO::getReleasedAmount, amount)
                .set(AigcQuotaFreezeDO::getReleaseTime, releaseTime)
                .set(AigcQuotaFreezeDO::getReason, reason)
                .set(taskId != null, AigcQuotaFreezeDO::getTaskId, taskId)
                .set(taskNo != null, AigcQuotaFreezeDO::getTaskNo, taskNo)
                .eq(AigcQuotaFreezeDO::getId, id)
                .eq(AigcQuotaFreezeDO::getStatus, AigcBillingFreezeStatusEnum.FROZEN.getCode()));
    }

    default List<AigcQuotaFreezeDO> selectTimeoutFrozenList(LocalDateTime now, Integer limit) {
        return selectList(new LambdaQueryWrapperX<AigcQuotaFreezeDO>()
                .eq(AigcQuotaFreezeDO::getStatus, AigcBillingFreezeStatusEnum.FROZEN.getCode())
                .lt(AigcQuotaFreezeDO::getExpireTime, now)
                .orderByAsc(AigcQuotaFreezeDO::getId)
                .last("LIMIT " + limit));
    }

    default List<AigcQuotaFreezeDO> selectConfirmedWithoutRecord(Long limit) {
        return selectList(new LambdaQueryWrapperX<AigcQuotaFreezeDO>()
                .eq(AigcQuotaFreezeDO::getStatus, AigcBillingFreezeStatusEnum.CONFIRMED.getCode())
                .isNotNull(AigcQuotaFreezeDO::getTaskId)
                .orderByAsc(AigcQuotaFreezeDO::getId)
                .last("LIMIT " + limit));
    }

}
