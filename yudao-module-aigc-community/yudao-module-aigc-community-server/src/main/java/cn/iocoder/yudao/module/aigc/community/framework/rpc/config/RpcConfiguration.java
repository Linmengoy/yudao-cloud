package cn.iocoder.yudao.module.aigc.community.framework.rpc.config;

import cn.iocoder.yudao.module.aigc.asset.api.AigcAssetApi;
import cn.iocoder.yudao.module.aigc.workflow.api.AigcWorkflowApi;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(value = "aigcCommunityRpcConfiguration", proxyBeanMethods = false)
@EnableFeignClients(clients = {AigcAssetApi.class, AigcWorkflowApi.class, MemberUserApi.class})
public class RpcConfiguration {
}
