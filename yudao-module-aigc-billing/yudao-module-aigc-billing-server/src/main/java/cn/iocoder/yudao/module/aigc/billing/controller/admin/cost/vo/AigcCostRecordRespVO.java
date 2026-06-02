package cn.iocoder.yudao.module.aigc.billing.controller.admin.cost.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - AIGC 成本记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class AigcCostRecordRespVO {

    @Schema(description = "成本记录编号")
    @ExcelProperty("成本记录编号")
    private Long id;

    @Schema(description = "成本单号")
    @ExcelProperty("成本单号")
    private String costNo;

    @Schema(description = "任务编号")
    @ExcelProperty("任务编号")
    private Long taskId;

    @Schema(description = "任务单号")
    @ExcelProperty("任务单号")
    private String taskNo;

    @Schema(description = "用户编号")
    @ExcelProperty("用户编号")
    private Long userId;

    @Schema(description = "模型编号")
    @ExcelProperty("模型编号")
    private Long modelId;

    @Schema(description = "渠道商编号")
    @ExcelProperty("渠道商编号")
    private Long providerId;

    @Schema(description = "能力")
    @ExcelProperty("能力")
    private String capability;

    @Schema(description = "计费单位")
    @ExcelProperty("计费单位")
    private String billingUnit;

    @Schema(description = "用量")
    @ExcelProperty("用量")
    private BigDecimal usageAmount;

    @Schema(description = "成本金额")
    @ExcelProperty("成本金额")
    private BigDecimal costAmount;

    @Schema(description = "销售金额")
    @ExcelProperty("销售金额")
    private BigDecimal saleAmount;

    @Schema(description = "毛利")
    @ExcelProperty("毛利")
    private BigDecimal grossProfit;

    @Schema(description = "毛利率")
    @ExcelProperty("毛利率")
    private BigDecimal grossProfitRate;

    @Schema(description = "货币类型")
    @ExcelProperty("货币类型")
    private String currencyType;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
