package cn.iocoder.yudao.module.aigc.billing.framework.rpc.config;

import cn.iocoder.yudao.module.pay.api.notify.PayNotifyApi;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "aigcBillingRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {PayOrderApi.class,PayNotifyApi.class})
public class RpcConfiguration {
}
