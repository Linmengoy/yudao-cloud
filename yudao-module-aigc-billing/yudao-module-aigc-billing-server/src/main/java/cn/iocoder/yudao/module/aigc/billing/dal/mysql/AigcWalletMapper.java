package cn.iocoder.yudao.module.aigc.billing.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;

@Mapper
public interface AigcWalletMapper extends BaseMapperX<AigcWalletDO> {

    default AigcWalletDO selectByUserId(Long userId) {
        return selectOne(AigcWalletDO::getUserId, userId);
    }

    default PageResult<AigcWalletDO> selectPage(PageParam reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcWalletDO>()
                .orderByDesc(AigcWalletDO::getId));
    }

    default int freezeBalance(Long id, BigDecimal amount) {
        return update(null, new LambdaUpdateWrapper<AigcWalletDO>()
                .setSql("balance = balance - " + amount)
                .setSql("frozen_balance = frozen_balance + " + amount)
                .setSql("last_trans_time = NOW()")
                .eq(AigcWalletDO::getId, id)
                .ge(AigcWalletDO::getBalance, amount));
    }

    default int confirmFrozen(Long id, BigDecimal amount) {
        return update(null, new LambdaUpdateWrapper<AigcWalletDO>()
                .setSql("frozen_balance = frozen_balance - " + amount)
                .setSql("total_consume = total_consume + " + amount)
                .setSql("last_trans_time = NOW()")
                .eq(AigcWalletDO::getId, id)
                .ge(AigcWalletDO::getFrozenBalance, amount));
    }

    default int releaseFrozen(Long id, BigDecimal amount) {
        return update(null, new LambdaUpdateWrapper<AigcWalletDO>()
                .setSql("balance = balance + " + amount)
                .setSql("frozen_balance = frozen_balance - " + amount)
                .setSql("last_trans_time = NOW()")
                .eq(AigcWalletDO::getId, id)
                .ge(AigcWalletDO::getFrozenBalance, amount));
    }

    default int recharge(Long id, BigDecimal amount) {
        return update(null, new LambdaUpdateWrapper<AigcWalletDO>()
                .setSql("balance = balance + " + amount)
                .setSql("total_recharge = total_recharge + " + amount)
                .setSql("last_trans_time = NOW()")
                .eq(AigcWalletDO::getId, id));
    }

    default int gift(Long id, BigDecimal amount) {
        return update(null, new LambdaUpdateWrapper<AigcWalletDO>()
                .setSql("balance = balance + " + amount)
                .setSql("total_gift = total_gift + " + amount)
                .setSql("last_trans_time = NOW()")
                .eq(AigcWalletDO::getId, id));
    }

    default int adjust(Long id, BigDecimal amount) {
        LambdaUpdateWrapper<AigcWalletDO> wrapper = new LambdaUpdateWrapper<AigcWalletDO>()
                .setSql("balance = balance + " + amount)
                .setSql("last_trans_time = NOW()")
                .eq(AigcWalletDO::getId, id);
        if (amount.signum() < 0) {
            wrapper.ge(AigcWalletDO::getBalance, amount.abs());
        }
        return update(null, wrapper);
    }

    default int refund(Long id, BigDecimal amount) {
        return update(null, new LambdaUpdateWrapper<AigcWalletDO>()
                .setSql("balance = balance + " + amount)
                .setSql("total_refund = total_refund + " + amount)
                .setSql("last_trans_time = NOW()")
                .eq(AigcWalletDO::getId, id));
    }

    default int compensate(Long id, BigDecimal amount) {
        return update(null, new LambdaUpdateWrapper<AigcWalletDO>()
                .setSql("balance = balance + " + amount)
                .setSql("total_gift = total_gift + " + amount)
                .setSql("last_trans_time = NOW()")
                .eq(AigcWalletDO::getId, id));
    }

}
