package cn.iocoder.yudao.module.aigc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Schema(description = "RPC Service - AIGC model submit candidate request DTO")
@Data
public class AigcModelSubmitCandidateReqDTO {

    @Schema(description = "Primary model id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long modelId;

    @Schema(description = "Capability", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT_TO_IMAGE")
    private String capability;

    @Schema(description = "Task type", example = "IMAGE")
    private String taskType;

    @Schema(description = "Pricing and validation params")
    private Map<String, Object> params;

    @Schema(description = "Excluded channel ids")
    private Set<Long> excludeChannelIds;

    @Schema(description = "Excluded provider ids")
    private Set<Long> excludeProviderIds;

    @Schema(description = "Preferred provider id for same-provider channel retry")
    private Long preferredProviderId;

    @Schema(description = "Maximum candidates", example = "3")
    private Integer maxCandidates;

    @Schema(description = "Strategy: CHANNEL_RETRY / PROVIDER_FALLBACK / HEDGING")
    private String strategy;

}
