package cn.iocoder.yudao.module.aigc.gen.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName(value = "aigc_gen_attempt", autoResultMap = true)
@KeySequence("aigc_gen_attempt_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcGenerateAttemptDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long recordId;
    private Long taskId;
    private Integer attemptNo;
    private Integer batchNo;
    private String strategy;
    private Long modelId;
    private String modelCode;
    private Long channelId;
    private String providerModel;
    private Long providerId;
    private String providerCode;
    private String providerTaskId;
    private String providerStatus;
    private String status;
    private BigDecimal saleAmount;
    private BigDecimal costAmount;
    private String currencyType;
    private String billingUnit;
    private String priceSnapshot;
    private String requestSummary;
    private String responseSummary;
    private String failCode;
    private String failReason;
    private Boolean winner;
    private LocalDateTime submitTime;
    private LocalDateTime callbackTime;
    private LocalDateTime finishTime;

}
