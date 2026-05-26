package cn.iocoder.yudao.module.aigc.gen.framework.client.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AigcProviderSubmitRespDTO {

    private String providerTaskId;
    private String providerStatus;
    private String outputText;
    private String outputData;
    private String outputUrls;
    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
    private Long durationMillis;
    private Boolean finished;
    private Boolean success;
    private String errorCode;
    private String errorMessage;

}
