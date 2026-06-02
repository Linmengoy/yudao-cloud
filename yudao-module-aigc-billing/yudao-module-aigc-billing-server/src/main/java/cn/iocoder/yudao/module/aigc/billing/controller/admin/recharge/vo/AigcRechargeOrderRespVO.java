package cn.iocoder.yudao.module.aigc.billing.controller.admin.recharge.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - AIGC 充值订单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class AigcRechargeOrderRespVO {

    @Schema(description = "充值订单编号")
    @ExcelProperty("充值订单编号")
    private Long id;

    @Schema(description = "充值订单号")
    @ExcelProperty("充值订单号")
    private String rechargeNo;

    @Schema(description = "钱包编号")
    @ExcelProperty("钱包编号")
    private Long walletId;

    @Schema(description = "用户编号")
    @ExcelProperty("用户编号")
    private Long userId;

    @Schema(description = "充值类型")
    @ExcelProperty("充值类型")
    private String rechargeType;

    @Schema(description = "支付金额")
    @ExcelProperty("支付金额")
    private Integer payAmount;

    @Schema(description = "充值积分")
    @ExcelProperty("充值积分")
    private BigDecimal pointAmount;

    @Schema(description = "赠送积分")
    @ExcelProperty("赠送积分")
    private BigDecimal giftAmount;

    @Schema(description = "总到账积分")
    @ExcelProperty("总到账积分")
    private BigDecimal totalPointAmount;

    @Schema(description = "Pay 支付订单编号")
    @ExcelProperty("Pay 支付订单编号")
    private Long payOrderId;

    @Schema(description = "Pay 支付订单号")
    @ExcelProperty("Pay 支付订单号")
    private String payOrderNo;

    @Schema(description = "支付渠道")
    @ExcelProperty("支付渠道")
    private String payChannelCode;

    @Schema(description = "充值状态")
    @ExcelProperty("充值状态")
    private String status;

    @Schema(description = "支付时间")
    @ExcelProperty("支付时间")
    private LocalDateTime payTime;

    @Schema(description = "关闭时间")
    @ExcelProperty("关闭时间")
    private LocalDateTime closeTime;

    @Schema(description = "退款时间")
    @ExcelProperty("退款时间")
    private LocalDateTime refundTime;

    @Schema(description = "操作人编号")
    @ExcelProperty("操作人编号")
    private Long operatorId;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
