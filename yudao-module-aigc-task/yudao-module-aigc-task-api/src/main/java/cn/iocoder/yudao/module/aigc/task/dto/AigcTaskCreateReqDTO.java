package cn.iocoder.yudao.module.aigc.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "RPC 服务 - AIGC 任务创建 Request DTO")
@Data
public class AigcTaskCreateReqDTO {

    @Schema(description = "平台任务编号", example = "TASK202605260001")
    private String taskNo;

    @Schema(description = "客户端请求编号", example = "REQ202605260001")
    private String clientRequestId;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "任务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT_GENERATE")
    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @Schema(description = "模型能力", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT_GENERATE")
    @NotBlank(message = "模型能力不能为空")
    private String capability;

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "模型编号不能为空")
    private Long modelId;

    @Schema(description = "渠道商编号", example = "2048")
    private Long providerId;

    @Schema(description = "请求参数 JSON")
    private String requestParams;

    @Schema(description = "价格快照 JSON")
    private String priceSnapshot;

    @Schema(description = "冻结记录编号", example = "4096")
    private Long freezeId;

    @Schema(description = "销售价", example = "10.00")
    private BigDecimal salePrice;

    @Schema(description = "成本价", example = "2.00")
    private BigDecimal costPrice;

    @Schema(description = "货币类型", example = "POINT")
    private String currencyType;

}
