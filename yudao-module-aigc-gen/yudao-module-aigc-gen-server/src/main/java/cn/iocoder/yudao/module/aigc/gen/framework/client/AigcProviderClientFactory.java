package cn.iocoder.yudao.module.aigc.gen.framework.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AigcProviderClientFactory {

    private final Map<String, AigcProviderClient> clients;
    private final Map<String, AigcProviderClient> clientTypes;

    @Resource(name = "mockAigcProviderClient")
    private AigcProviderClient defaultClient;

    public AigcProviderClientFactory(List<AigcProviderClient> clients) {
        this.clients = clients.stream().collect(Collectors.toMap(AigcProviderClient::getProviderCode, Function.identity(), (a, b) -> a));
        this.clientTypes = clients.stream().collect(Collectors.toMap(AigcProviderClient::getClientType, Function.identity(), (a, b) -> a));
    }

    public AigcProviderClient getClient(String providerCode) {
        if ("provider_hkcopp".equals(providerCode)) {
            return clients.getOrDefault("openapi-generations", defaultClient);
        }
        return clients.getOrDefault(providerCode, defaultClient);
    }

    public AigcProviderClient getClient(AigcProviderSubmitReqDTO reqDTO) {
        String modelClientType = resolveClientTypeByModel(reqDTO);
        if (StrUtil.isNotBlank(modelClientType)) {
            AigcProviderClient client = clientTypes.get(modelClientType);
            if (client != null) {
                return client;
            }
        }
        String clientType = resolveClientType(reqDTO == null ? null : reqDTO.getProviderExtraConfig());
        if (StrUtil.isNotBlank(clientType)) {
            AigcProviderClient client = clientTypes.get(clientType);
            if (client != null) {
                return client;
            }
        }
        return getClient(reqDTO == null ? null : reqDTO.getProviderCode());
    }

    private String resolveClientTypeByModel(AigcProviderSubmitReqDTO reqDTO) {
        if (reqDTO == null) {
            return null;
        }
        String model = StrUtil.blankToDefault(reqDTO.getProviderModel(), reqDTO.getModelCode());
        if (StrUtil.isBlank(model)) {
            return null;
        }
        String normalized = model.trim().toLowerCase();
        if (normalized.startsWith("gpt-image-")) {
            return "gpt-image-2";
        }
        if (normalized.startsWith("midjourney")) {
            return MidjourneyProviderClient.CLIENT_TYPE;
        }
        if (normalized.startsWith("doubao-seedream-") || normalized.startsWith("seedream-")) {
            return VolcImagesGenerationsProviderClient.CLIENT_TYPE;
        }
        return null;
    }

    private String resolveClientType(String extraConfig) {
        if (StrUtil.isBlank(extraConfig) || !JSONUtil.isTypeJSON(extraConfig)) {
            return null;
        }
        JSONObject extra = JSONUtil.parseObj(extraConfig);
        return extra.getStr("clientType");
    }
}
