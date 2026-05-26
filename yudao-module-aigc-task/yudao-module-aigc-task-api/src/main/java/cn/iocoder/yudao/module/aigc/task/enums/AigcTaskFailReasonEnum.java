package cn.iocoder.yudao.module.aigc.task.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcTaskFailReasonEnum {

    MODEL_VALIDATE_FAILED("MODEL_VALIDATE_FAILED", "模型校验失败"),
    PARAM_VALIDATE_FAILED("PARAM_VALIDATE_FAILED", "参数校验失败"),
    PRICE_CALCULATE_FAILED("PRICE_CALCULATE_FAILED", "价格计算失败"),
    BALANCE_NOT_ENOUGH("BALANCE_NOT_ENOUGH", "余额不足"),
    SAFETY_REJECTED("SAFETY_REJECTED", "审核拒绝"),
    PROVIDER_SUBMIT_FAILED("PROVIDER_SUBMIT_FAILED", "第三方提交失败"),
    PROVIDER_CALLBACK_FAILED("PROVIDER_CALLBACK_FAILED", "第三方回调失败"),
    DOWNLOAD_FAILED("DOWNLOAD_FAILED", "下载失败"),
    ASSET_CREATE_FAILED("ASSET_CREATE_FAILED", "资产创建失败"),
    BILLING_REFUND_FAILED("BILLING_REFUND_FAILED", "退款失败"),
    TIMEOUT("TIMEOUT", "任务超时"),
    UNKNOWN("UNKNOWN", "未知错误");

    private final String code;
    private final String name;

}
