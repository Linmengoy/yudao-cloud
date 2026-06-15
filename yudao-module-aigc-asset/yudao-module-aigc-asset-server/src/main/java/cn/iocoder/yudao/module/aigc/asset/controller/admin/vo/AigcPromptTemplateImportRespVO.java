package cn.iocoder.yudao.module.aigc.asset.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - AIGC 提示词模板导入 Response VO")
@Data
@Accessors(chain = true)
public class AigcPromptTemplateImportRespVO {

    @Schema(description = "读取案例数")
    private Integer totalCount;

    @Schema(description = "新增数量")
    private Integer createCount;

    @Schema(description = "更新数量")
    private Integer updateCount;

    @Schema(description = "跳过数量")
    private Integer skipCount;

}
