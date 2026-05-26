package cn.iocoder.yudao.module.aigc.task.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcTaskStatusEnum {

    CREATED("CREATED", "任务已创建"),
    PRICE_CALCULATED("PRICE_CALCULATED", "已记录价格快照"),
    FROZEN("FROZEN", "已冻结积分"),
    QUEUED("QUEUED", "已进入队列"),
    RUNNING("RUNNING", "生成服务处理中"),
    SUBMITTED("SUBMITTED", "已提交第三方任务"),
    CALLBACK_WAITING("CALLBACK_WAITING", "等待第三方回调"),
    DOWNLOADING("DOWNLOADING", "下载第三方文件结果中"),
    ASSET_CREATING("ASSET_CREATING", "创建资产中"),
    AUDITING("AUDITING", "审核中"),
    SUCCESS("SUCCESS", "任务成功"),
    FAILED("FAILED", "任务失败"),
    CANCELLED("CANCELLED", "任务取消"),
    REFUNDING("REFUNDING", "退款处理中"),
    REFUNDED("REFUNDED", "已退款或已释放冻结");

    private final String code;
    private final String name;

}
