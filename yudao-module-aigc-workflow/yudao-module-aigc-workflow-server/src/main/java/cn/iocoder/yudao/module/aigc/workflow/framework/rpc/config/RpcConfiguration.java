package cn.iocoder.yudao.module.aigc.workflow.framework.rpc.config;

import cn.iocoder.yudao.module.aigc.billing.api.AigcBillingApi;
import cn.iocoder.yudao.module.aigc.asset.api.AigcAssetApi;
import cn.iocoder.yudao.module.aigc.gen.api.AigcGenerateApi;
import cn.iocoder.yudao.module.aigc.model.api.AigcModelApi;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "aigcWorkflowRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {AigcGenerateApi.class, AigcModelApi.class, AigcBillingApi.class, AigcAssetApi.class, MemberUserApi.class, FileApi.class})
public class RpcConfiguration {
}
