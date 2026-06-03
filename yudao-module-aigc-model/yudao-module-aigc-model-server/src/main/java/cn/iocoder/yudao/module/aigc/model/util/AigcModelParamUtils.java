package cn.iocoder.yudao.module.aigc.model.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;

import java.util.List;

public class AigcModelParamUtils {

    private AigcModelParamUtils() {
    }

    public static List<String> parseOptions(String options) {
        if (StrUtil.isBlank(options) || !JSONUtil.isTypeJSONArray(options)) {
            return List.of();
        }
        return JSONUtil.toList(options, String.class).stream()
                .map(AigcModelParamUtils::decodeOptionValue)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    public static String decodeOptionValue(String option) {
        String value = StrUtil.trim(option);
        for (int i = 0; i < 3; i++) {
            String parsedValue = JsonUtils.parseObjectQuietly(value, String.class);
            if (parsedValue == null) {
                break;
            }
            value = StrUtil.trim(parsedValue);
        }
        return value;
    }

}
