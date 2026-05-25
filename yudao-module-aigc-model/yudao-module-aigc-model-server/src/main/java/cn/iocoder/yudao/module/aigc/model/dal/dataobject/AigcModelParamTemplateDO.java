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

@TableName(value = "aigc_model_param_template", autoResultMap = true)
@KeySequence("aigc_model_param_template_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcModelParamTemplateDO extends BaseDO {

    @TableId
    private Long id;

    private Long modelId;

    private String capability;

    private String paramKey;

    private String paramName;

    private String paramType;

    private Boolean requiredStatus;

    private String defaultValue;

    private String options;

    private BigDecimal minValue;

    private BigDecimal maxValue;

    private String regexPattern;

    private Integer sort;

    private Integer status;

}
