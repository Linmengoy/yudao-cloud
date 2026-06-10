package cn.iocoder.yudao.module.aigc.gen.framework.client.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AigcProviderSubmitReqDTO {

    private Long recordId;
    private Long taskId;
    private Long userId;
    private Long modelId;
    private String modelCode;
    private Long channelId;
    private String providerModel;
    private Long providerId;
    private String providerCode;
    private String providerTaskId;
    private String providerBaseUrl;
    private String providerApiKey;
    private String providerSecretKey;
    private String providerExtraConfig;
    private Integer providerTimeoutSeconds;
    private Boolean proxyEnabled;
    private String proxyProtocol;
    private String proxyHost;
    private Integer proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    private String generateType;
    private String generateMode;
    private String prompt;
    private String inputParams;
    private Boolean sync;

}
