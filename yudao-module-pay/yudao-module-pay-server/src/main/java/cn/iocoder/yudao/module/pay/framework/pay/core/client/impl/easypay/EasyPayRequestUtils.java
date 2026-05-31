package cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.easypay;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static cn.hutool.core.date.DatePattern.NORM_DATETIME_FORMATTER;

public class EasyPayRequestUtils {

    public static String buildUrl(String serverUrl, String path) {
        return StrUtil.removeSuffix(serverUrl, "/") + "/" + StrUtil.removePrefix(path, "/");
    }

    public static String formatAmount(Integer price) {
        return BigDecimal.valueOf(price).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY).toPlainString();
    }

    public static Integer parseAmount(String amount) {
        return StrUtil.isBlank(amount) ? null : new BigDecimal(amount).multiply(BigDecimal.valueOf(100)).intValueExact();
    }

    public static String formatTime(LocalDateTime time) {
        return LocalDateTimeUtil.format(time, NORM_DATETIME_FORMATTER);
    }

    public static LocalDateTime parseTime(String time) {
        return StrUtil.isBlank(time) ? null : LocalDateTimeUtil.parse(time, NORM_DATETIME_FORMATTER);
    }

    public static Map<String, String> parseBody(String body) {
        if (StrUtil.isBlank(body)) {
            return new HashMap<>();
        }
        Map<String, Object> jsonMap = JsonUtils.parseObjectQuietly(body, new TypeReference<>() {});
        if (MapUtil.isNotEmpty(jsonMap)) {
            return toStringMap(jsonMap);
        }
        return HttpUtil.decodeParamMap(body, StandardCharsets.UTF_8);
    }

    public static Map<String, String> mergeNotifyParams(Map<String, String> params, String body) {
        Map<String, String> bodyParams = parseBody(body);
        if (MapUtil.isEmpty(params)) {
            return bodyParams;
        }
        if (MapUtil.isEmpty(bodyParams)) {
            return toStringMap(params);
        }
        Map<String, String> result = new HashMap<>(bodyParams);
        params.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }
            String bodyValue = result.get(key);
            Assert.isTrue(StrUtil.isBlank(bodyValue) || StrUtil.equals(bodyValue, value), "EasyPay 回调参数存在冲突");
            result.put(key, value);
        });
        return result;
    }

    public static Map<String, String> toStringMap(Map<String, ?> source) {
        Map<String, String> result = new HashMap<>();
        if (MapUtil.isEmpty(source)) {
            return result;
        }
        source.forEach((key, value) -> {
            if (StrUtil.isNotBlank(key) && value != null) {
                result.put(key, String.valueOf(value));
            }
        });
        return result;
    }

}
