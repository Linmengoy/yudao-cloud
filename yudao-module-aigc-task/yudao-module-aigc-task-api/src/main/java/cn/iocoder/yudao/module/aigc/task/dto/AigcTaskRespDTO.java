package cn.iocoder.yudao.module.aigc.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - AIGC 任务 Response DTO")
@Data
@Accessors(chain = true)
public class AigcTaskRespDTO {

    @Schema(description = "任务编号", example = "1024")
    private Long id;

    @Schema(description = "平台任务编号", example = "TASK202605260001")
    private String taskNo;

    @Schema(description = "客户端请求编号", example = "REQ202605260001")
    private String clientRequestId;

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "任务类型", example = "TEXT_GENERATE")
    private String taskType;

    @Schema(description = "模型能力", example = "TEXT_GENERATE")
    private String capability;

    @Schema(description = "模型编号", example = "1024")
    private Long modelId;

    @Schema(description = "渠道商编号", example = "2048")
    private Long providerId;

    @Schema(description = "任务状态", example = "SUCCESS")
    private String status;

    @Schema(description = "进度", example = "100")
    private Integer progress;

    @Schema(description = "冻结记录编号", example = "4096")
    private Long freezeId;

    @Schema(description = "销售价", example = "10.00")
    private BigDecimal salePrice;

    @Schema(description = "成本价", example = "2.00")
    private BigDecimal costPrice;

    @Schema(description = "货币类型", example = "POINT")
    private String currencyType;

    @Schema(description = "第三方任务编号")
    private String externalTaskId;

    @Schema(description = "输出资产编号")
    private Long outputAssetId;

    @Schema(description = "输出资产类型")
    private String outputAssetType;

    @Schema(description = "文本输出")
    private String outputText;

    @Schema(description = "结构化输出 JSON")
    private String outputData;

    @Schema(description = "失败错误码")
    private String failCode;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

}
