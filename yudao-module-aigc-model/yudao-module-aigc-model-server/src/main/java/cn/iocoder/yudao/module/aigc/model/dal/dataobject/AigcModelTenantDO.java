package cn.iocoder.yudao.module.aigc.model.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_model_tenant", autoResultMap = true)
@KeySequence("aigc_model_tenant_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcModelTenantDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long modelId;

    private Boolean enabled;

    private Boolean publicVisible;

    private Boolean defaultModel;

    private Integer sort;

    private Integer maxConcurrent;

    private Integer dailyLimit;

    private String remark;

}
