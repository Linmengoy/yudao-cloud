package cn.iocoder.yudao.module.aigc.model.controller.admin.usage.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - AIGC 模型类型用量统计 Response VO")
@Data
public class AigcModelUsageTypeStatisticsRespVO {

    @Schema(description = "模型类型", example = "2")
    private Integer modelType;

    @Schema(description = "调用次数", example = "1024")
    private Long usageCount;

    @Schema(description = "成功次数", example = "1000")
    private Long successCount;

    @Schema(description = "失败次数", example = "24")
    private Long failedCount;

    @Schema(description = "Token 总数", example = "4096")
    private Long totalTokens;

    @Schema(description = "销售价合计", example = "128.000000")
    private BigDecimal salePrice;

    @Schema(description = "成本价合计", example = "64.000000")
    private BigDecimal costPrice;

    @Schema(description = "平均耗时，单位毫秒", example = "1200")
    private BigDecimal avgDurationMillis;

}
