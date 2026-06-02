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

    /** 充值订单号 */
    private String rechargeNo;
    /** 钱包ID */
    private Long walletId;
    /** 用户ID */
    private Long userId;
    /** 充值类型 */
    private String rechargeType;
    /** 充值金额 */
    private Integer payAmount;
    /** 充值积分 */
    private BigDecimal pointAmount;
    /** 充值奖励积分 */
    private BigDecimal giftAmount;
    /** 充值总积分 */
    private BigDecimal totalPointAmount;
    /** 支付订单ID */
    private Long payOrderId;
    /** 支付订单号 */
    private String payOrderNo;
    /** 支付渠道 */
    private String payChannelCode;
    /** 充值状态 */
    private String status;
    /** 充值时间 */
    private LocalDateTime payTime;
    /** 关闭时间 */
    private LocalDateTime closeTime;
    /** 退款时间 */
    private LocalDateTime refundTime;
    /** 操作人ID */
    private Long operatorId;
    /** 备注 */
    private String remark;

}
