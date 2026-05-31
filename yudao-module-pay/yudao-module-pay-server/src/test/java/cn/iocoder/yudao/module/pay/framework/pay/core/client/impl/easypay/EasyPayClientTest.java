package cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.easypay;

import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.dto.order.PayOrderRespDTO;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EasyPayClientTest {

    @Test
    public void testParseOrderNotify_success() {
        EasyPayClientConfig config = buildConfig();
        EasyPayClient client = new EasyPayClient(1L, config);
        Map<String, String> notify = new HashMap<>();
        notify.put("merchant_no", config.getMerchantNo());
        notify.put("app_id", config.getAppId());
        notify.put("out_trade_no", "P202605300001");
        notify.put("trade_no", "EP202605300001");
        notify.put("buyer_id", "U100001");
        notify.put("status", "SUCCESS");
        notify.put("total_amount", "1.00");
        notify.put("pay_time", "2026-05-30 12:00:00");
        notify.put("sign", EasyPaySigner.sign(notify, config));

        PayOrderRespDTO respDTO = client.parseOrderNotify(notify, null, null);

        assertEquals(PayOrderStatusEnum.SUCCESS.getStatus(), respDTO.getStatus());
        assertEquals("P202605300001", respDTO.getOutTradeNo());
        assertEquals("EP202605300001", respDTO.getChannelOrderNo());
        assertEquals("U100001", respDTO.getChannelUserId());
        assertEquals(100, respDTO.getChannelPrice());
        assertNotNull(respDTO.getSuccessTime());
        assertEquals(notify, respDTO.getRawData());
    }

    @Test
    public void testParseOrderNotify_conflictParams() {
        EasyPayClientConfig config = buildConfig();
        EasyPayClient client = new EasyPayClient(1L, config);
        Map<String, String> params = new HashMap<>();
        params.put("status", "SUCCESS");

        assertThrows(RuntimeException.class, () -> client.parseOrderNotify(params, "status=WAITING", null));
    }

    @Test
    public void testParseOrderNotify_invalidSign() {
        EasyPayClientConfig config = buildConfig();
        EasyPayClient client = new EasyPayClient(1L, config);
        Map<String, String> notify = new HashMap<>();
        notify.put("merchant_no", config.getMerchantNo());
        notify.put("out_trade_no", "P202605300001");
        notify.put("status", "SUCCESS");
        notify.put("sign", "invalid");

        assertThrows(RuntimeException.class, () -> client.parseOrderNotify(notify, null, null));
    }

    @Test
    public void testParseStatus() {
        assertEquals(PayOrderStatusEnum.SUCCESS.getStatus(), EasyPayOrderStatusMapping.parse("PAID"));
        assertEquals(PayOrderStatusEnum.WAITING.getStatus(), EasyPayOrderStatusMapping.parse("PROCESSING"));
        assertEquals(PayOrderStatusEnum.CLOSED.getStatus(), EasyPayOrderStatusMapping.parse("EXPIRED"));
        assertNull(EasyPayOrderStatusMapping.parse("UNKNOWN"));
    }

    private EasyPayClientConfig buildConfig() {
        EasyPayClientConfig config = new EasyPayClientConfig();
        config.setServerUrl("https://gateway.easypay.example.com");
        config.setMerchantNo("M100001");
        config.setAppId("APP100001");
        config.setSignType(EasyPayClientConfig.SIGN_TYPE_MD5);
        config.setSecretKey("test-secret");
        config.setSandbox(true);
        return config;
    }

}
