package cn.iocoder.yudao.module.aigc.task.framework.rpc.config;

import cn.iocoder.yudao.module.aigc.billing.api.AigcBillingApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "aigcTaskRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {AigcBillingApi.class})
public class RpcConfiguration {
}
