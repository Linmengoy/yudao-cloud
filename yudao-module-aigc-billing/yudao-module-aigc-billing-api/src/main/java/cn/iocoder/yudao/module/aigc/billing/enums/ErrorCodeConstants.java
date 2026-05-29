package cn.iocoder.yudao.module.aigc.billing.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode WALLET_NOT_EXISTS = new ErrorCode(1_042_000_000, "钱包不存在");
    ErrorCode WALLET_DISABLED = new ErrorCode(1_042_000_001, "钱包已禁用");
    ErrorCode WALLET_BALANCE_NOT_ENOUGH = new ErrorCode(1_042_000_002, "钱包余额不足");
    ErrorCode WALLET_FROZEN_BALANCE_NOT_ENOUGH = new ErrorCode(1_042_000_003, "钱包冻结余额不足");
    ErrorCode WALLET_AMOUNT_INVALID = new ErrorCode(1_042_000_004, "钱包金额不合法");

    ErrorCode FREEZE_NOT_EXISTS = new ErrorCode(1_042_001_000, "冻结记录不存在");
    ErrorCode FREEZE_BIZ_DUPLICATE = new ErrorCode(1_042_001_001, "业务冻结记录已存在");
    ErrorCode FREEZE_STATUS_INVALID = new ErrorCode(1_042_001_002, "冻结记录状态不允许当前操作");
    ErrorCode FREEZE_AMOUNT_NOT_MATCH = new ErrorCode(1_042_001_003, "冻结金额与操作金额不匹配");
    ErrorCode FREEZE_ALREADY_CONFIRMED = new ErrorCode(1_042_001_004, "冻结记录已确认扣费，不允许重复冻结");
    ErrorCode FREEZE_ALREADY_RELEASED = new ErrorCode(1_042_001_005, "冻结记录已释放，不允许重复冻结");

    ErrorCode BILLING_RECORD_NOT_EXISTS = new ErrorCode(1_042_002_000, "计费流水不存在");
    ErrorCode BILLING_RECORD_DUPLICATE = new ErrorCode(1_042_002_001, "计费流水重复");

    ErrorCode COST_RECORD_NOT_EXISTS = new ErrorCode(1_042_003_000, "成本记录不存在");
    ErrorCode COST_RECORD_DUPLICATE = new ErrorCode(1_042_003_001, "成本记录重复");

    ErrorCode RECHARGE_ORDER_NOT_EXISTS = new ErrorCode(1_042_004_000, "充值订单不存在");
    ErrorCode RECHARGE_ORDER_STATUS_INVALID = new ErrorCode(1_042_004_001, "充值订单状态不允许当前操作");
    ErrorCode RECHARGE_ORDER_DUPLICATE = new ErrorCode(1_042_004_002, "充值订单重复");

    ErrorCode RECHARGE_PACKAGE_NOT_EXISTS = new ErrorCode(1_042_005_000, "充值套餐不存在");

}
