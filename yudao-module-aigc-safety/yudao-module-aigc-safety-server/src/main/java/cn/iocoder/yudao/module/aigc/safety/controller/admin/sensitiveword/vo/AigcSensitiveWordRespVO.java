package cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AIGC 敏感词 Response VO")
@Data
public class AigcSensitiveWordRespVO {

    @Schema(description = "敏感词编号", example = "1024")
    private Long id;

    @Schema(description = "敏感词")
    private String word;

    @Schema(description = "审核场景", example = "PROMPT")
    private String scene;

    @Schema(description = "风险等级", example = "3")
    private Integer level;

    @Schema(description = "匹配方式", example = "CONTAINS")
    private String matchType;

    @Schema(description = "状态", example = "ENABLE")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
