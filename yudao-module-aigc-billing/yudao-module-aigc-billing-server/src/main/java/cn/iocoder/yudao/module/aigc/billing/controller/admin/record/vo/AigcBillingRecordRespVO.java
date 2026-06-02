package cn.iocoder.yudao.module.aigc.billing.controller.admin.record.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - AIGC 计费流水 Response VO")
@Data
@ExcelIgnoreUnannotated
public class AigcBillingRecordRespVO {

    @Schema(description = "流水编号")
    @ExcelProperty("流水编号")
    private Long id;

    @Schema(description = "流水单号")
    @ExcelProperty("流水单号")
    private String recordNo;

    @Schema(description = "钱包编号")
    @ExcelProperty("钱包编号")
    private Long walletId;

    @Schema(description = "用户编号")
    @ExcelProperty("用户编号")
    private Long userId;

    @Schema(description = "业务类型")
    @ExcelProperty("业务类型")
    private String bizType;

    @Schema(description = "业务编号")
    @ExcelProperty("业务编号")
    private String bizId;

    @Schema(description = "流水类型")
    @ExcelProperty("流水类型")
    private String recordType;

    @Schema(description = "流水标题")
    @ExcelProperty("流水标题")
    private String title;

    @Schema(description = "变动积分")
    @ExcelProperty("变动积分")
    private BigDecimal amount;

    @Schema(description = "变动后可用余额")
    @ExcelProperty("变动后可用余额")
    private BigDecimal balanceAfter;

    @Schema(description = "变动后冻结余额")
    @ExcelProperty("变动后冻结余额")
    private BigDecimal frozenBalanceAfter;

    @Schema(description = "冻结记录编号")
    @ExcelProperty("冻结记录编号")
    private Long freezeId;

    @Schema(description = "任务编号")
    @ExcelProperty("任务编号")
    private Long taskId;

    @Schema(description = "模型编号")
    @ExcelProperty("模型编号")
    private Long modelId;

    @Schema(description = "渠道商编号")
    @ExcelProperty("渠道商编号")
    private Long providerId;

    @Schema(description = "货币类型")
    @ExcelProperty("货币类型")
    private String currencyType;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
