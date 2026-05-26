package cn.iocoder.yudao.module.aigc.billing.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("aigc_recharge_order")
@KeySequence("aigc_recharge_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcRechargeOrderDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String rechargeNo;
    private Long walletId;
    private Long userId;
    private String rechargeType;
    private Integer payAmount;
    private BigDecimal pointAmount;
    private BigDecimal giftAmount;
    private BigDecimal totalPointAmount;
    private Long payOrderId;
    private String payOrderNo;
    private String payChannelCode;
    private String status;
    private LocalDateTime payTime;
    private LocalDateTime closeTime;
    private LocalDateTime refundTime;
    private Long operatorId;
    private String remark;

}
