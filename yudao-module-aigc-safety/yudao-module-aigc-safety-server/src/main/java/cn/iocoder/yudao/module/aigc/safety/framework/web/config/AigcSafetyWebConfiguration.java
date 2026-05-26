package cn.iocoder.yudao.module.aigc.safety.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AigcSafetyWebConfiguration {

    @Bean
    public GroupedOpenApi aigcSafetyGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("aigc-safety-server", "aigc/safety");
    }

}
