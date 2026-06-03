package cn.iocoder.yudao.module.aigc.billing.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.recharge.vo.AigcRechargeOrderPageReqVO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargeOrderDO;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRechargeStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AigcRechargeOrderMapper extends BaseMapperX<AigcRechargeOrderDO> {

    default AigcRechargeOrderDO selectByRechargeNo(String rechargeNo) {
        return selectOne(AigcRechargeOrderDO::getRechargeNo, rechargeNo);
    }

    default AigcRechargeOrderDO selectByPayOrderId(Long payOrderId) {
        return selectOne(AigcRechargeOrderDO::getPayOrderId, payOrderId);
    }

    default AigcRechargeOrderDO selectLatestWaitPayByUserAndPackage(Long userId, Long packageId, LocalDateTime expireTime) {
        return selectOne(new LambdaQueryWrapperX<AigcRechargeOrderDO>()
                .eq(AigcRechargeOrderDO::getUserId, userId)
                .eq(AigcRechargeOrderDO::getPackageId, packageId)
                .eq(AigcRechargeOrderDO::getStatus, AigcBillingRechargeStatusEnum.WAIT_PAY.getCode())
                .gt(AigcRechargeOrderDO::getCreateTime, expireTime)
                .orderByDesc(AigcRechargeOrderDO::getId)
                .last("LIMIT 1"));
    }

    default PageResult<AigcRechargeOrderDO> selectPage(AigcRechargeOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcRechargeOrderDO>()
                .likeIfPresent(AigcRechargeOrderDO::getRechargeNo, reqVO.getRechargeNo())
                .eqIfPresent(AigcRechargeOrderDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AigcRechargeOrderDO::getPayOrderId, reqVO.getPayOrderId())
                .likeIfPresent(AigcRechargeOrderDO::getPayOrderNo, reqVO.getPayOrderNo())
                .eqIfPresent(AigcRechargeOrderDO::getPayChannelCode, reqVO.getPayChannelCode())
                .eqIfPresent(AigcRechargeOrderDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AigcRechargeOrderDO::getCreateTime, reqVO.getCreateTime())
                .betweenIfPresent(AigcRechargeOrderDO::getPayTime, reqVO.getPayTime())
                .orderByDesc(AigcRechargeOrderDO::getId));
    }

    default PageResult<AigcRechargeOrderDO> selectUserPage(PageParam reqVO, Long userId) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcRechargeOrderDO>()
                .eq(AigcRechargeOrderDO::getUserId, userId)
                .orderByDesc(AigcRechargeOrderDO::getId));
    }

    default int updatePaid(Long id, Long payOrderId, String payOrderNo, String payChannelCode, LocalDateTime payTime) {
        return update(null, new LambdaUpdateWrapper<AigcRechargeOrderDO>()
                .set(AigcRechargeOrderDO::getStatus, AigcBillingRechargeStatusEnum.PAID.getCode())
                .set(AigcRechargeOrderDO::getPayOrderId, payOrderId)
                .set(AigcRechargeOrderDO::getPayOrderNo, payOrderNo)
                .set(AigcRechargeOrderDO::getPayChannelCode, payChannelCode)
                .set(AigcRechargeOrderDO::getPayTime, payTime)
                .eq(AigcRechargeOrderDO::getId, id)
                .eq(AigcRechargeOrderDO::getStatus, AigcBillingRechargeStatusEnum.WAIT_PAY.getCode()));
    }

    default int updatePayOrder(Long id, Long payOrderId, String payOrderNo) {
        return update(null, new LambdaUpdateWrapper<AigcRechargeOrderDO>()
                .set(AigcRechargeOrderDO::getPayOrderId, payOrderId)
                .set(AigcRechargeOrderDO::getPayOrderNo, payOrderNo)
                .eq(AigcRechargeOrderDO::getId, id)
                .eq(AigcRechargeOrderDO::getStatus, AigcBillingRechargeStatusEnum.WAIT_PAY.getCode()));
    }

    default int updateClosed(Long id, LocalDateTime closeTime) {
        return update(null, new LambdaUpdateWrapper<AigcRechargeOrderDO>()
                .set(AigcRechargeOrderDO::getStatus, AigcBillingRechargeStatusEnum.CLOSED.getCode())
                .set(AigcRechargeOrderDO::getCloseTime, closeTime)
                .eq(AigcRechargeOrderDO::getId, id)
                .eq(AigcRechargeOrderDO::getStatus, AigcBillingRechargeStatusEnum.WAIT_PAY.getCode()));
    }

    default List<AigcRechargeOrderDO> selectExpiredWaitPayList(LocalDateTime expireTime, Integer limit) {
        return selectList(new LambdaQueryWrapperX<AigcRechargeOrderDO>()
                .eq(AigcRechargeOrderDO::getStatus, AigcBillingRechargeStatusEnum.WAIT_PAY.getCode())
                .lt(AigcRechargeOrderDO::getCreateTime, expireTime)
                .orderByAsc(AigcRechargeOrderDO::getId)
                .last("LIMIT " + limit));
    }

    default List<AigcRechargeOrderDO> selectWaitPayWithPayOrderList(Integer limit) {
        return selectList(new LambdaQueryWrapperX<AigcRechargeOrderDO>()
                .eq(AigcRechargeOrderDO::getStatus, AigcBillingRechargeStatusEnum.WAIT_PAY.getCode())
                .isNotNull(AigcRechargeOrderDO::getPayOrderId)
                .orderByAsc(AigcRechargeOrderDO::getId)
                .last("LIMIT " + limit));
    }

    @Select("""
            SELECT ro.*
            FROM aigc_recharge_order ro
            WHERE ro.status = #{status}
              AND ro.deleted = 0
              AND NOT EXISTS (
                  SELECT 1
                  FROM aigc_billing_record br
                  WHERE br.biz_type = #{bizType}
                    AND br.biz_id = ro.recharge_no
                    AND br.deleted = 0
              )
            ORDER BY ro.id ASC
            LIMIT #{limit}
            """)
    List<AigcRechargeOrderDO> selectPaidWithoutRecordList(@Param("status") String status,
                                                          @Param("bizType") String bizType,
                                                          @Param("limit") Integer limit);

}
