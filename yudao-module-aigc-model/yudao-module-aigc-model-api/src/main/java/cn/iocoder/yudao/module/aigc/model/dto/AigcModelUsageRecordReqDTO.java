package cn.iocoder.yudao.module.aigc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "RPC 服务 - AIGC 模型调用计量 Request DTO")
@Data
public class AigcModelUsageRecordReqDTO {

    @Schema(description = "链路追踪编号", example = "TID:xxxx")
    private String traceId;

    @Schema(description = "任务编号", example = "1024")
    private Long taskId;

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long modelId;

    @Schema(description = "渠道商编号", example = "1024")
    private Long providerId;

    @Schema(description = "模型能力", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT_GENERATE")
    private String capability;

    @Schema(description = "请求编号", example = "req_123")
    private String requestId;

    @Schema(description = "外部任务编号", example = "task_123")
    private String externalTaskId;

    @Schema(description = "提示词 Token 数", example = "100")
    private Long promptTokens;

    @Schema(description = "输出 Token 数", example = "200")
    private Long completionTokens;

    @Schema(description = "总 Token 数", example = "300")
    private Long totalTokens;

    @Schema(description = "缓存 Token 数", example = "50")
    private Long cachedTokens;

    @Schema(description = "推理 Token 数", example = "20")
    private Long reasoningTokens;

    @Schema(description = "输入 Token 数", example = "100")
    private Long inputTokens;

    @Schema(description = "输出 Token 数", example = "200")
    private Long outputTokens;

    @Schema(description = "成本价", example = "0.10")
    private BigDecimal costPrice;

    @Schema(description = "销售价", example = "0.20")
    private BigDecimal salePrice;

    @Schema(description = "币种", example = "POINT")
    private String currencyType;

    @Schema(description = "调用状态", example = "0")
    private Integer status;

    @Schema(description = "调用耗时，单位：毫秒", example = "1000")
    private Long durationMillis;

    @Schema(description = "渠道原始 usage JSON")
    private String usageJson;

    @Schema(description = "错误码")
    private String errorCode;

    @Schema(description = "错误信息")
    private String errorMessage;

}
