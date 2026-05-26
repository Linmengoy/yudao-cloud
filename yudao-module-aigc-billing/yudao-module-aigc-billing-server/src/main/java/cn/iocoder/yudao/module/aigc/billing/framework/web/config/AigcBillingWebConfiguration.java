package cn.iocoder.yudao.module.aigc.billing.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AigcBillingWebConfiguration {

    @Bean
    public GroupedOpenApi aigcBillingGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("aigc-billing");
    }
}
