package cn.iocoder.yudao.module.aigc.model.controller.admin.channel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - AIGC 模型渠道实现新增/修改 Request VO")
@Data
public class AigcModelChannelSaveReqVO {

    @Schema(description = "渠道实现编号", example = "1024")
    private Long id;

    @Schema(description = "业务模型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    @Schema(description = "渠道商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "渠道商ID不能为空")
    private Long providerId;

    @Schema(description = "渠道商真实模型标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "image2")
    @NotBlank(message = "上游模型标识不能为空")
    private String providerModel;

    @Schema(description = "渠道实现名称", example = "OpenAI image2")
    private String name;

    @Schema(description = "成本价", example = "0.10")
    private BigDecimal costPrice;

    @Schema(description = "币种", example = "POINT")
    private String currencyType;

    @Schema(description = "权重", example = "100")
    private Integer weight;

    @Schema(description = "优先级", example = "100")
    private Integer priority;

    @Schema(description = "最大并发", example = "10")
    private Integer maxConcurrent;

    @Schema(description = "超时时间，单位：秒", example = "60")
    private Integer timeoutSeconds;

    @Schema(description = "限流配置")
    private String rateLimitConfig;

    @Schema(description = "健康状态", example = "HEALTHY")
    private String healthStatus;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
