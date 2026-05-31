package cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.easypay;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EasyPaySignerTest {

    @Test
    public void testSignAndVerify_md5() {
        EasyPayClientConfig config = buildConfig();
        Map<String, String> params = new HashMap<>();
        params.put("merchant_no", config.getMerchantNo());
        params.put("out_trade_no", "P202605300001");
        params.put("total_amount", "1.00");
        params.put("empty", "");

        String sign = EasyPaySigner.sign(params, config);
        params.put("sign", sign);

        assertEquals("merchant_no=M100001&out_trade_no=P202605300001&total_amount=1.00", EasyPaySigner.buildSignText(params));
        assertTrue(EasyPaySigner.verify(params, config));
        params.put("total_amount", "2.00");
        assertFalse(EasyPaySigner.verify(params, config));
    }

    private EasyPayClientConfig buildConfig() {
        EasyPayClientConfig config = new EasyPayClientConfig();
        config.setServerUrl("https://gateway.easypay.example.com");
        config.setMerchantNo("M100001");
        config.setSignType(EasyPayClientConfig.SIGN_TYPE_MD5);
        config.setSecretKey("test-secret");
        config.setSandbox(true);
        return config;
    }

}
