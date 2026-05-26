package cn.iocoder.yudao.module.aigc.task.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AigcTaskWebConfiguration {

    @Bean
    public GroupedOpenApi aigcTaskGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("aigc-task");
    }
}
