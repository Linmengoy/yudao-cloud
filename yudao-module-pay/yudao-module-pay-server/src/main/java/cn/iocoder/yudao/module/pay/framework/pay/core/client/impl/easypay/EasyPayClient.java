package cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.easypay;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.pay.enums.PayChannelEnum;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.dto.order.PayOrderRespDTO;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.dto.order.PayOrderUnifiedReqDTO;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.dto.refund.PayRefundRespDTO;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.dto.refund.PayRefundUnifiedReqDTO;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.dto.transfer.PayTransferRespDTO;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.dto.transfer.PayTransferUnifiedReqDTO;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.AbstractPayClient;
import cn.iocoder.yudao.module.pay.framework.pay.core.enums.PayOrderDisplayModeEnum;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class EasyPayClient extends AbstractPayClient<EasyPayClientConfig> {

    public EasyPayClient(Long channelId, EasyPayClientConfig config) {
        super(channelId, PayChannelEnum.EASYPAY_CASHIER.getCode(), config);
    }

    @Override
    protected void doInit() {
    }

    @Override
    protected PayOrderRespDTO doUnifiedOrder(PayOrderUnifiedReqDTO reqDTO) {
        Map<String, String> request = buildUnifiedOrderRequest(reqDTO);
        Map<String, String> response = post(config.getUnifiedOrderPath(), request);
        if (!isSuccessResponse(response)) {
            return PayOrderRespDTO.closedOf(response.get("code"), ObjectUtil.defaultIfNull(response.get("message"), response.get("msg")),
                    reqDTO.getOutTradeNo(), response);
        }
        Integer status = EasyPayOrderStatusMapping.parse(firstNotBlank(response, "status", "trade_status", "order_status"));
        if (PayOrderStatusEnum.SUCCESS.getStatus().equals(status)) {
            return PayOrderRespDTO.successOf(firstNotBlank(response, "trade_no", "channel_order_no", "transaction_id"),
                    firstNotBlank(response, "buyer_id", "payer_id", "channel_user_id"), parseChannelPrice(response), parseSuccessTime(response),
                    reqDTO.getOutTradeNo(), response);
        }
        return PayOrderRespDTO.waitingOf(resolveDisplayMode(response), resolveDisplayContent(response), reqDTO.getOutTradeNo(), response);
    }

    @Override
    protected PayOrderRespDTO doParseOrderNotify(Map<String, String> params, String body, Map<String, String> headers) {
        Map<String, String> notify = EasyPayRequestUtils.mergeNotifyParams(params, body);
        Assert.isTrue(EasyPaySigner.verify(notify, config), "EasyPay 回调签名校验失败");
        Assert.equals(config.getMerchantNo(), firstNotBlank(notify, "merchant_no", "merchantNo", "mch_id"), "EasyPay 回调商户号不匹配");
        if (StrUtil.isNotBlank(config.getAppId()) && StrUtil.isNotBlank(firstNotBlank(notify, "app_id", "appId"))) {
            Assert.equals(config.getAppId(), firstNotBlank(notify, "app_id", "appId"), "EasyPay 回调应用编号不匹配");
        }
        Integer status = EasyPayOrderStatusMapping.parse(firstNotBlank(notify, "status", "trade_status", "order_status"));
        Assert.notNull(status, "EasyPay 回调支付状态不正确");
        return PayOrderRespDTO.of(status, firstNotBlank(notify, "trade_no", "channel_order_no", "transaction_id"),
                firstNotBlank(notify, "buyer_id", "payer_id", "channel_user_id"), parseChannelPrice(notify), parseSuccessTime(notify),
                firstNotBlank(notify, "out_trade_no", "outTradeNo", "merchant_order_no"), notify);
    }

    @Override
    protected PayOrderRespDTO doGetOrder(String outTradeNo) {
        Map<String, String> request = new HashMap<>();
        putBaseParams(request);
        request.put("out_trade_no", outTradeNo);
        sign(request);
        Map<String, String> response = post(config.getQueryOrderPath(), request);
        if (!isSuccessResponse(response)) {
            return PayOrderRespDTO.closedOf(response.get("code"), ObjectUtil.defaultIfNull(response.get("message"), response.get("msg")),
                    outTradeNo, response);
        }
        Integer status = EasyPayOrderStatusMapping.parse(firstNotBlank(response, "status", "trade_status", "order_status"));
        Assert.notNull(status, "EasyPay 查单支付状态不正确");
        return PayOrderRespDTO.of(status, firstNotBlank(response, "trade_no", "channel_order_no", "transaction_id"),
                firstNotBlank(response, "buyer_id", "payer_id", "channel_user_id"), parseChannelPrice(response), parseSuccessTime(response), outTradeNo, response);
    }

    @Override
    protected PayRefundRespDTO doUnifiedRefund(PayRefundUnifiedReqDTO reqDTO) {
        throw new UnsupportedOperationException("EasyPay 第一阶段暂不支持退款");
    }

    @Override
    protected PayRefundRespDTO doParseRefundNotify(Map<String, String> params, String body, Map<String, String> headers) {
        throw new UnsupportedOperationException("EasyPay 第一阶段暂不支持退款回调");
    }

    @Override
    protected PayRefundRespDTO doGetRefund(String outTradeNo, String outRefundNo) {
        throw new UnsupportedOperationException("EasyPay 第一阶段暂不支持退款查询");
    }

    @Override
    protected PayTransferRespDTO doUnifiedTransfer(PayTransferUnifiedReqDTO reqDTO) {
        throw new UnsupportedOperationException("EasyPay 暂不支持转账");
    }

    @Override
    protected PayTransferRespDTO doParseTransferNotify(Map<String, String> params, String body, Map<String, String> headers) {
        throw new UnsupportedOperationException("EasyPay 暂不支持转账回调");
    }

    @Override
    protected PayTransferRespDTO doGetTransfer(String outTradeNo) {
        throw new UnsupportedOperationException("EasyPay 暂不支持转账查询");
    }

    private Map<String, String> buildUnifiedOrderRequest(PayOrderUnifiedReqDTO reqDTO) {
        Map<String, String> request = new HashMap<>();
        putBaseParams(request);
        request.put("out_trade_no", reqDTO.getOutTradeNo());
        request.put("total_amount", EasyPayRequestUtils.formatAmount(reqDTO.getPrice()));
        request.put("subject", reqDTO.getSubject());
        request.put("body", reqDTO.getBody());
        request.put("notify_url", reqDTO.getNotifyUrl());
        request.put("return_url", ObjectUtil.defaultIfBlank(reqDTO.getReturnUrl(), config.getReturnUrl()));
        request.put("expire_time", EasyPayRequestUtils.formatTime(reqDTO.getExpireTime()));
        sign(request);
        return request;
    }

    private void putBaseParams(Map<String, String> request) {
        request.put("merchant_no", config.getMerchantNo());
        request.put("app_id", config.getAppId());
        request.put("sign_type", config.getSignType());
    }

    private void sign(Map<String, String> request) {
        request.put("sign", EasyPaySigner.sign(request, config));
    }

    private Map<String, String> post(String path, Map<String, String> request) {
        try (HttpResponse response = HttpRequest.post(EasyPayRequestUtils.buildUrl(config.getServerUrl(), path))
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JsonUtils.toJsonString(request))
                .timeout(config.getTimeoutSeconds() * 1000)
                .execute()) {
            return EasyPayRequestUtils.parseBody(response.body());
        }
    }

    private boolean isSuccessResponse(Map<String, String> response) {
        String code = firstNotBlank(response, "code", "result_code", "status_code");
        return StrUtil.isBlank(code) || StrUtil.equalsAnyIgnoreCase(code, "0", "SUCCESS", "OK", "200");
    }

    private String resolveDisplayMode(Map<String, String> response) {
        String displayMode = firstNotBlank(response, "display_mode", "displayMode");
        if (StrUtil.isNotBlank(displayMode)) {
            return displayMode;
        }
        if (StrUtil.isNotBlank(firstNotBlank(response, "form_html", "form"))) {
            return PayOrderDisplayModeEnum.FORM.getMode();
        }
        if (StrUtil.isNotBlank(firstNotBlank(response, "qr_code_url", "qrcode_url"))) {
            return PayOrderDisplayModeEnum.QR_CODE_URL.getMode();
        }
        if (StrUtil.isNotBlank(firstNotBlank(response, "qr_code", "qrcode"))) {
            return PayOrderDisplayModeEnum.QR_CODE.getMode();
        }
        return PayOrderDisplayModeEnum.URL.getMode();
    }

    private String resolveDisplayContent(Map<String, String> response) {
        return firstNotBlank(response, "display_content", "displayContent", "pay_url", "cashier_url", "url",
                "form_html", "form", "qr_code_url", "qrcode_url", "qr_code", "qrcode");
    }

    private LocalDateTime parseSuccessTime(Map<String, String> response) {
        return EasyPayRequestUtils.parseTime(firstNotBlank(response, "success_time", "pay_time", "paid_time", "gmt_payment"));
    }

    private Integer parseChannelPrice(Map<String, String> response) {
        return EasyPayRequestUtils.parseAmount(firstNotBlank(response, "total_amount", "amount", "pay_amount", "payAmount"));
    }

    private String firstNotBlank(Map<String, String> map, String... keys) {
        for (String key : keys) {
            String value = MapUtil.getStr(map, key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

}
