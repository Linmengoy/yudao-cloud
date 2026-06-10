package cn.iocoder.yudao.module.aigc.model.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@TableName(value = "aigc_model_channel", autoResultMap = true)
@KeySequence("aigc_model_channel_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcModelChannelDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    /**
     * 业务模型编号。
     */
    private Long modelId;

    private Long providerId;

    /**
     * 渠道商真实模型标识。
     */
    private String providerModel;

    private String name;

    private BigDecimal costPrice;

    private String currencyType;

    private Integer weight;

    private Integer priority;

    private Integer maxConcurrent;

    private Integer timeoutSeconds;

    private String rateLimitConfig;

    private String healthStatus;

    private Integer status;

    private String remark;

}
