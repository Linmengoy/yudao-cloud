package cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.easypay;

import cn.iocoder.yudao.framework.common.util.object.ObjectUtils;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;

public class EasyPayOrderStatusMapping {

    public static Integer parse(String status) {
        if (ObjectUtils.equalsAny(status, "SUCCESS", "PAID", "TRADE_SUCCESS")) {
            return PayOrderStatusEnum.SUCCESS.getStatus();
        }
        if (ObjectUtils.equalsAny(status, "WAITING", "UNPAID", "PROCESSING")) {
            return PayOrderStatusEnum.WAITING.getStatus();
        }
        if (ObjectUtils.equalsAny(status, "CLOSED", "CANCELLED", "EXPIRED", "FAILED")) {
            return PayOrderStatusEnum.CLOSED.getStatus();
        }
        return null;
    }

}
