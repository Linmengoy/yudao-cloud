package cn.iocoder.yudao.module.aigc.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "RPC 服务 - AIGC 毛利响应 DTO")
@Data
public class AigcGrossProfitRespDTO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long taskId;

    @Schema(description = "平台成本", requiredMode = Schema.RequiredMode.REQUIRED, example = "5.000000")
    private BigDecimal costAmount;

    @Schema(description = "用户销售价", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.000000")
    private BigDecimal saleAmount;

    @Schema(description = "毛利", requiredMode = Schema.RequiredMode.REQUIRED, example = "5.000000")
    private BigDecimal grossProfit;

    @Schema(description = "毛利率", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.500000")
    private BigDecimal grossProfitRate;

    @Schema(description = "Currency type", example = "POINT")
    private String currencyType;

}
