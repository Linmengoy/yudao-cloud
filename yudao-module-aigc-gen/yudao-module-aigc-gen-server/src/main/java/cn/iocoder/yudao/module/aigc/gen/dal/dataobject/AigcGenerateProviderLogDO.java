package cn.iocoder.yudao.module.aigc.gen.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_gen_provider_log", autoResultMap = true)
@KeySequence("aigc_gen_provider_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcGenerateProviderLogDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long recordId;
    private Long taskId;
    private String providerCode;
    private String modelCode;
    private String apiAction;
    private String requestId;
    private String requestSummary;
    private String responseSummary;
    private Boolean success;
    private Integer httpStatus;
    private String errorCode;
    private String errorMessage;
    private Long durationMs;

}
