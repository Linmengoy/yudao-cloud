package cn.iocoder.yudao.module.aigc.billing;

import cn.iocoder.yudao.framework.common.enums.WebFilterOrderEnum;
import cn.iocoder.yudao.framework.mybatis.config.YudaoMybatisAutoConfiguration;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnoreAspect;
import cn.iocoder.yudao.framework.tenant.core.web.TenantContextWebFilter;
import cn.iocoder.yudao.module.aigc.billing.config.AigcBillingPayProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@MapperScan("cn.iocoder.yudao.module.aigc.billing.dal.mysql")
@Import({YudaoMybatisAutoConfiguration.class, TenantIgnoreAspect.class})
@EnableConfigurationProperties(AigcBillingPayProperties.class)
public class AigcBillingServerApplication {

    private static final Logger log = LoggerFactory.getLogger(AigcBillingServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AigcBillingServerApplication.class, args);
    }

    @Bean
    @Profile("!test")
    public FilterRegistrationBean<TenantContextWebFilter> tenantContextWebFilter() {
        FilterRegistrationBean<TenantContextWebFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantContextWebFilter());
        registration.setOrder(WebFilterOrderEnum.TENANT_CONTEXT_FILTER);
        return registration;
    }

    @Bean
    public ApplicationRunner printPayProperties(AigcBillingPayProperties payProperties) {
        return args -> log.info("支付配置 => appId: {}, appKey: {}, expireMinutes: {}",
                payProperties.getAppId(), payProperties.getAppKey(), payProperties.getExpireMinutes());
    }

}
