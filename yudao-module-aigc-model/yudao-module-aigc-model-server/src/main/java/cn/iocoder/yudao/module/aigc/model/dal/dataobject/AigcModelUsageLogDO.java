package cn.iocoder.yudao.module.aigc.model.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@TableName(value = "aigc_model_usage_log", autoResultMap = true)
@KeySequence("aigc_model_usage_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcModelUsageLogDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String traceId;

    private Long taskId;

    private Long userId;

    private Long modelId;

    private Long providerId;

    private String capability;

    private String requestId;

    private String externalTaskId;

    private Long promptTokens;

    private Long completionTokens;

    private Long totalTokens;

    private Long cachedTokens;

    private Long reasoningTokens;

    private Long inputTokens;

    private Long outputTokens;

    private BigDecimal costPrice;

    private BigDecimal salePrice;

    private String currencyType;

    private Integer status;

    private Long durationMillis;

    private String usageJson;

    private String errorCode;

    private String errorMessage;

}
