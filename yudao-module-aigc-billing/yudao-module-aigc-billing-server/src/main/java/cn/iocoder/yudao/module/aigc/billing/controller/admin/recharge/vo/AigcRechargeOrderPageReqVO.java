package cn.iocoder.yudao.module.aigc.billing.controller.admin.recharge.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - AIGC 充值订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcRechargeOrderPageReqVO extends PageParam {

    @Schema(description = "充值订单号", example = "RC202601010001")
    private String rechargeNo;

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "Pay 订单编号", example = "2048")
    private Long payOrderId;

    @Schema(description = "Pay 订单号", example = "P202601010001")
    private String payOrderNo;

    @Schema(description = "支付渠道", example = "easypay_cashier")
    private String payChannelCode;

    @Schema(description = "充值状态", example = "WAIT_PAY")
    private String status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "支付时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] payTime;

}
