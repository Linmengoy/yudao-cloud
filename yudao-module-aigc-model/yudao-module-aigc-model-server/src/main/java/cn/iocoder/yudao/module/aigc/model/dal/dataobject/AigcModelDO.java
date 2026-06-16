package cn.iocoder.yudao.module.aigc.model.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_model", autoResultMap = true)
@KeySequence("aigc_model_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcModelDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long providerId;

    private String code;

    private String name;

    private String nameEn;

    private String model;

    private Integer type;

    private Boolean publicVisible;

    private Boolean defaultModel;

    private Integer sort;

    private Integer maxConcurrent;

    private Integer timeoutSeconds;

    private Integer queuePriority;

    private Integer status;

    private String remark;

}
