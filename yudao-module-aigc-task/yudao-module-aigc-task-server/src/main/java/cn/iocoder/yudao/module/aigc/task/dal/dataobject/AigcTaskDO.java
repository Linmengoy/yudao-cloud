package cn.iocoder.yudao.module.aigc.task.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AIGC 任务 DO
 *
 * @author 芋道源码
 */
@TableName(value = "aigc_task", autoResultMap = true)
@KeySequence("aigc_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcTaskDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 任务编号
     */
    private String taskNo;

    /**
     * 客户端请求ID
     */
    private String clientRequestId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 能力类型
     */
    private String capability;

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 服务商ID
     */
    private Long providerId;

    /**
     * 渠道实现ID
     */
    private Long channelId;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 任务进度
     */
    private Integer progress;

    /**
     * 预计耗时，单位毫秒
     */
    private Long estimatedDurationMillis;

    /**
     * 请求参数（JSON格式）
     */
    private String requestParams;

    /**
     * 价格快照（JSON格式）
     */
    private String priceSnapshot;

    /**
     * 冻结记录ID
     */
    private Long freezeId;

    /**
     * 售价
     */
    private BigDecimal salePrice;

    /**
     * 成本价
     */
    private BigDecimal costPrice;

    /**
     * 货币类型
     */
    private String currencyType;

    /**
     * 外部任务ID（服务商侧的任务ID）
     */
    private String externalTaskId;

    /**
     * 输出资产ID
     */
    private Long outputAssetId;

    /**
     * 输出资产类型
     */
    private String outputAssetType;

    /**
     * 输出摘要
     */
    private String outputSummary;

    /**
     * 输出文本
     */
    @TableField(exist = false)
    private String outputText;

    /**
     * 输出数据（JSON格式）
     */
    @TableField(exist = false)
    private String outputData;

    /**
     * 失败码
     */
    private String failCode;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 回调时间
     */
    private LocalDateTime callbackTime;

    /**
     * 完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;

    /**
     * 备注
     */
    private String remark;

}
