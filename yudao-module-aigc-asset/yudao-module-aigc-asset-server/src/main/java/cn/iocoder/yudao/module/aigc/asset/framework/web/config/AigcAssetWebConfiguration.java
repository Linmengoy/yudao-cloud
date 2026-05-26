package cn.iocoder.yudao.module.aigc.asset.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AigcAssetWebConfiguration {

    @Bean
    public GroupedOpenApi aigcAssetGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("aigc-asset");
    }
}
