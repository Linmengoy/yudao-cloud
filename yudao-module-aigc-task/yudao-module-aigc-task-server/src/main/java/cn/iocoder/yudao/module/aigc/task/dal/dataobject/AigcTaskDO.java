package cn.iocoder.yudao.module.aigc.task.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName(value = "aigc_task", autoResultMap = true)
@KeySequence("aigc_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcTaskDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String taskNo;

    private String clientRequestId;

    private Long userId;

    private String taskType;

    private String capability;

    private Long modelId;

    private Long providerId;

    private String status;

    private Integer progress;

    private String requestParams;

    private String priceSnapshot;

    private Long freezeId;

    private BigDecimal salePrice;

    private BigDecimal costPrice;

    private String currencyType;

    private String externalTaskId;

    private Long outputAssetId;

    private String outputAssetType;

    private String outputText;

    private String outputData;

    private String failCode;

    private String failReason;

    private LocalDateTime submitTime;

    private LocalDateTime startTime;

    private LocalDateTime callbackTime;

    private LocalDateTime finishTime;

    private LocalDateTime expireTime;

    private Integer retryCount;

    private Integer maxRetryCount;

    private String remark;

}
