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
import java.time.LocalDateTime;

@TableName(value = "aigc_model_price", autoResultMap = true)
@KeySequence("aigc_model_price_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcModelPriceDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long modelId;

    private String capability;

    private String billingUnit;

    private BigDecimal costPrice;

    private BigDecimal salePrice;

    private String currencyType;

    private String priceConfig;

    private LocalDateTime effectiveStartTime;

    private LocalDateTime effectiveEndTime;

    private Integer status;

}
