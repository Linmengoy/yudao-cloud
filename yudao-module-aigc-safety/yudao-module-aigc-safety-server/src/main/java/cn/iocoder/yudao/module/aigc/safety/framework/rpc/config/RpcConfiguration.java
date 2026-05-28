package cn.iocoder.yudao.module.aigc.safety.framework.rpc.config;

import cn.iocoder.yudao.module.aigc.asset.api.AigcAssetApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "aigcSafetyRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {AigcAssetApi.class})
public class RpcConfiguration {
}
