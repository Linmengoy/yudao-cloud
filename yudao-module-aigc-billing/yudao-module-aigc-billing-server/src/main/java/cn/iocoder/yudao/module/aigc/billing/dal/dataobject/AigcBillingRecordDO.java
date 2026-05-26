package cn.iocoder.yudao.module.aigc.billing.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@TableName(value = "aigc_billing_record", autoResultMap = true)
@KeySequence("aigc_billing_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcBillingRecordDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String recordNo;
    private Long walletId;
    private Long userId;
    private String bizType;
    private String bizId;
    private String recordType;
    private String title;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private BigDecimal frozenBalanceAfter;
    private Long freezeId;
    private Long taskId;
    private Long modelId;
    private Long providerId;
    private String currencyType;
    private String priceSnapshot;
    private String extraInfo;

}
