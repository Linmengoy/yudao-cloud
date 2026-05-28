package cn.iocoder.yudao.module.aigc.asset.framework.rpc.config;

import cn.iocoder.yudao.module.aigc.task.api.AigcTaskApi;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "aigcAssetRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {AigcTaskApi.class, FileApi.class})
public class RpcConfiguration {
}
