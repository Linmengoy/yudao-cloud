package cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AIGC 敏感词分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcSensitiveWordPageReqVO extends PageParam {

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

    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;

}
