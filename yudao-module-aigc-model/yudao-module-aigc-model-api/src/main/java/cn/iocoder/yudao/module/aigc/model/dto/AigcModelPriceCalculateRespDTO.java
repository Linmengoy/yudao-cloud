package cn.iocoder.yudao.module.aigc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "RPC 服务 - AIGC 模型价格计算 Response DTO")
@Data
public class AigcModelPriceCalculateRespDTO {

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long modelId;

    @Schema(description = "模型能力", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT_TO_IMAGE")
    private String capability;

    @Schema(description = "币种", example = "CNY")
    private String currencyType;

    @Schema(description = "成本价", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.10")
    private BigDecimal costPrice;

    @Schema(description = "销售价", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.20")
    private BigDecimal salePrice;

    @Schema(description = "计费单位", example = "PER_TASK")
    private String billingUnit;

    @Schema(description = "价格来源", example = "PLATFORM")
    private String priceSource;

    @Schema(description = "价格规则编号", example = "1024")
    private Long priceRuleId;

    @Schema(description = "价格明细")
    private Map<String, Object> priceDetail;

}
