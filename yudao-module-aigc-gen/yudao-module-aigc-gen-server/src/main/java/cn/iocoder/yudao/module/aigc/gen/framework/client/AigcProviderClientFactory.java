package cn.iocoder.yudao.module.aigc.gen.framework.client;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AigcProviderClientFactory {

    private final Map<String, AigcProviderClient> clients;

    @Resource(name = "mockAigcProviderClient")
    private AigcProviderClient defaultClient;

    public AigcProviderClientFactory(List<AigcProviderClient> clients) {
        this.clients = clients.stream().collect(Collectors.toMap(AigcProviderClient::getProviderCode, Function.identity(), (a, b) -> a));
    }

    public AigcProviderClient getClient(String providerCode) {
        return clients.getOrDefault(providerCode, defaultClient);
    }
}
