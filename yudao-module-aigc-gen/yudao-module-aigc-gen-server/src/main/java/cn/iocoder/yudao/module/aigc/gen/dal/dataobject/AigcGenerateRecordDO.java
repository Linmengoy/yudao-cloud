package cn.iocoder.yudao.module.aigc.gen.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName(value = "aigc_gen_record", autoResultMap = true)
@KeySequence("aigc_gen_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcGenerateRecordDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long taskId;
    private Long userId;
    private String generateNo;
    private String clientRequestId;
    private String generateType;
    private String generateMode;
    private Long modelId;
    private String modelCode;
    private Long channelId;
    private String providerModel;
    private Long providerId;
    private String providerCode;
    private String providerTaskId;
    private String providerStatus;
    private String status;
    private String prompt;
    private String inputParams;
    private String outputText;
    private String outputData;
    private String outputUrls;
    private String assetIds;
    private Long freezeId;
    private BigDecimal priceAmount;
    private BigDecimal costAmount;
    private LocalDateTime submitTime;
    private LocalDateTime callbackTime;
    private LocalDateTime finishTime;
    private String failReason;
    private String failMessage;

}
