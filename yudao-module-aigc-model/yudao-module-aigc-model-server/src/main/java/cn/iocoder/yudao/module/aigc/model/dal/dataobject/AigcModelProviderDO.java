package cn.iocoder.yudao.module.aigc.model.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.type.EncryptTypeHandler;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@TableName(value = "aigc_model_provider", autoResultMap = true)
@KeySequence("aigc_model_provider_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcModelProviderDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String code;

    private String name;

    private String apiBaseUrl;

    private String authType;

    @TableField(typeHandler = EncryptTypeHandler.class)
    private String apiKey;

    @TableField(typeHandler = EncryptTypeHandler.class)
    private String secretKey;

    private String extraConfig;

    private Integer timeoutSeconds;

    private String rateLimitConfig;

    private String healthStatus;

    private BigDecimal balance;

    private Integer status;

    private String remark;

}
