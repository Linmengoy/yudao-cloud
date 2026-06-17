package cn.iocoder.yudao.module.aigc.gen.framework.rpc.config;

import cn.iocoder.yudao.module.aigc.asset.api.AigcAssetApi;
import cn.iocoder.yudao.module.aigc.billing.api.AigcBillingApi;
import cn.iocoder.yudao.module.aigc.model.api.AigcModelApi;
import cn.iocoder.yudao.module.aigc.safety.api.AigcSafetyApi;
import cn.iocoder.yudao.module.aigc.task.api.AigcTaskApi;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "aigcGenRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {AigcModelApi.class, AigcBillingApi.class, AigcTaskApi.class, AigcAssetApi.class, AigcSafetyApi.class, ConfigApi.class, FileApi.class})
public class RpcConfiguration {
}
