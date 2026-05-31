package cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.easypay;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.PayClientConfig;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EasyPayClientConfig implements PayClientConfig {

    public static final String SIGN_TYPE_MD5 = "MD5";
    public static final String SIGN_TYPE_RSA2 = "RSA2";
    public static final String SIGN_TYPE_HMAC_SHA256 = "HMAC_SHA256";
    public static final String NOTIFY_CONTENT_TYPE_JSON = "JSON";
    public static final String NOTIFY_CONTENT_TYPE_FORM = "FORM";

    @NotBlank(message = "EasyPay 网关地址不能为空")
    private String serverUrl;
    @NotBlank(message = "EasyPay 商户号不能为空")
    private String merchantNo;
    private String appId;
    @NotBlank(message = "EasyPay 签名类型不能为空")
    private String signType;
    private String privateKey;
    private String publicKey;
    private String secretKey;
    private String returnUrl;
    private String notifyContentType = NOTIFY_CONTENT_TYPE_JSON;
    @NotNull(message = "EasyPay 沙箱标识不能为空")
    private Boolean sandbox;
    private Integer timeoutSeconds = 10;
    private String unifiedOrderPath = "/pay/unified-order";
    private String queryOrderPath = "/pay/query-order";
    private String successResponse = "success";

    @Override
    public void validate(Validator validator) {
        ValidationUtils.validate(validator, this);
        if (StrUtil.equalsIgnoreCase(signType, SIGN_TYPE_RSA2)) {
            ValidationUtils.validate(validator, new RsaConfig(privateKey, publicKey));
        } else if (StrUtil.equalsAnyIgnoreCase(signType, SIGN_TYPE_MD5, SIGN_TYPE_HMAC_SHA256)) {
            ValidationUtils.validate(validator, new SecretConfig(secretKey));
        } else {
            throw new IllegalArgumentException("EasyPay 签名类型不支持：" + signType);
        }
    }

    @Data
    private static class RsaConfig {
        @NotBlank(message = "EasyPay 商户私钥不能为空")
        private final String privateKey;
        @NotBlank(message = "EasyPay 平台公钥不能为空")
        private final String publicKey;
    }

    @Data
    private static class SecretConfig {
        @NotBlank(message = "EasyPay 对称密钥不能为空")
        private final String secretKey;
    }

}
