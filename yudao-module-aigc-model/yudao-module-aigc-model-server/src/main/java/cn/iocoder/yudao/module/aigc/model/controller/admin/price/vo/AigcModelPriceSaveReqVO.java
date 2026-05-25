package cn.iocoder.yudao.module.aigc.model.controller.admin.price.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - AIGC 模型价格规则新增/修改 Request VO")
@Data
public class AigcModelPriceSaveReqVO {

    @Schema(description = "价格规则编号", example = "1024")
    private Long id;

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    @Schema(description = "模型能力", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT_TO_IMAGE")
    @NotBlank(message = "能力不能为空")
    private String capability;

    @Schema(description = "计费单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "PER_TASK")
    @NotBlank(message = "计费单位不能为空")
    private String billingUnit;

    @Schema(description = "成本价", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.10")
    @NotNull(message = "成本价不能为空")
    private BigDecimal costPrice;

    @Schema(description = "销售价", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.20")
    @NotNull(message = "销售价不能为空")
    private BigDecimal salePrice;

    @Schema(description = "币种", example = "CNY")
    private String currencyType;

    @Schema(description = "价格配置")
    private String priceConfig;

    @Schema(description = "生效开始时间")
    private LocalDateTime effectiveStartTime;

    @Schema(description = "生效结束时间")
    private LocalDateTime effectiveEndTime;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
