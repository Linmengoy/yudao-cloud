package cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.easypay;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.PrivateKey;
import java.util.Map;
import java.util.TreeMap;

public class EasyPaySigner {

    public static String sign(Map<String, String> params, EasyPayClientConfig config) {
        String plainText = buildSignText(params);
        if (StrUtil.equalsIgnoreCase(config.getSignType(), EasyPayClientConfig.SIGN_TYPE_MD5)) {
            return SecureUtil.md5(plainText + config.getResolvedPkey());
        }
        if (StrUtil.equalsIgnoreCase(config.getSignType(), EasyPayClientConfig.SIGN_TYPE_HMAC_SHA256)) {
            HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, config.getResolvedPkey().getBytes(StandardCharsets.UTF_8));
            return HexUtil.encodeHexStr(hMac.digest(plainText), false);
        }
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            PrivateKey privateKey = SecureUtil.generatePrivateKey("RSA", Base64.decode(config.getPrivateKey()));
            signature.initSign(privateKey);
            signature.update(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.encode(signature.sign());
        } catch (Exception ex) {
            throw new IllegalArgumentException("EasyPay RSA2 签名失败", ex);
        }
    }

    public static boolean verify(Map<String, String> params, EasyPayClientConfig config) {
        String sign = MapUtil.getStr(params, "sign");
        if (StrUtil.isBlank(sign)) {
            return false;
        }
        if (StrUtil.equalsIgnoreCase(config.getSignType(), EasyPayClientConfig.SIGN_TYPE_RSA2)) {
            try {
                Signature signature = Signature.getInstance("SHA256withRSA");
                signature.initVerify(SecureUtil.generatePublicKey("RSA", Base64.decode(config.getPublicKey())));
                signature.update(buildSignText(params).getBytes(StandardCharsets.UTF_8));
                return signature.verify(Base64.decode(sign));
            } catch (Exception ex) {
                return false;
            }
        }
        return StrUtil.equalsIgnoreCase(sign(params, config), sign);
    }

    public static String buildSignText(Map<String, String> params) {
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        sortedParams.remove("sign");
        sortedParams.remove("sign_type");
        StringBuilder builder = new StringBuilder();
        sortedParams.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || StrUtil.isBlank(value)) {
                return;
            }
            if (!builder.isEmpty()) {
                builder.append('&');
            }
            builder.append(key).append('=').append(value);
        });
        return StrUtil.str(builder, CharsetUtil.CHARSET_UTF_8);
    }

}
