package cn.iocoder.yudao.module.aigc.gen.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName(value = "aigc_gen_callback", autoResultMap = true)
@KeySequence("aigc_gen_callback_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcGenerateCallbackDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long recordId;
    private Long attemptId;
    private Long taskId;
    private String providerCode;
    private String providerTaskId;
    private String callbackType;
    private String callbackNo;
    private Boolean signatureValid;
    private String rawBody;
    private String parsedData;
    private String processStatus;
    private String processMessage;
    private LocalDateTime processTime;

}
