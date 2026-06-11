package cn.iocoder.yudao.module.aigc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "RPC Service - AIGC model submit candidate response DTO")
@Data
public class AigcModelSubmitCandidateRespDTO {

    @Schema(description = "Candidate list")
    private List<Candidate> candidates;

    @Data
    public static class Candidate {

        @Schema(description = "Model routed to a concrete channel")
        private AigcModelRespDTO model;

        @Schema(description = "Provider info")
        private AigcModelProviderRespDTO provider;

        @Schema(description = "Price for this candidate")
        private AigcModelPriceCalculateRespDTO price;

        @Schema(description = "Retry strategy")
        private String strategy;

        @Schema(description = "Sort priority")
        private Integer priority;

        @Schema(description = "Route weight")
        private Integer weight;

        @Schema(description = "Channel health status")
        private String healthStatus;

    }

}
