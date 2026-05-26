package cn.iocoder.yudao.module.aigc.gen.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AigcGenerateStatusEnum implements ArrayValuable<String> {

    CREATED("CREATED", "已创建"),
    SUBMITTING("SUBMITTING", "提交中"),
    SUBMITTED("SUBMITTED", "已提交"),
    CALLBACK_WAITING("CALLBACK_WAITING", "等待回调"),
    SYNCING("SYNCING", "同步中"),
    DOWNLOADING("DOWNLOADING", "下载中"),
    ASSET_CREATING("ASSET_CREATING", "资产创建中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消");

    public static final String[] ARRAYS = Arrays.stream(values()).map(AigcGenerateStatusEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
