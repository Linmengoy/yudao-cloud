package cn.iocoder.yudao.module.member.framework.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MemberEmailCodeProperties.class)
public class MemberAuthConfiguration {
}
