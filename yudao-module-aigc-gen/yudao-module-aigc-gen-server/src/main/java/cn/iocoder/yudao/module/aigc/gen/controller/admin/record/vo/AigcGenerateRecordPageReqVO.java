package cn.iocoder.yudao.module.aigc.gen.controller.admin.record.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AIGC 生成记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcGenerateRecordPageReqVO extends PageParam {

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "任务编号", example = "2048")
    private Long taskId;

    @Schema(description = "生成流水号", example = "GEN202605260001")
    private String generateNo;

    @Schema(description = "生成类型", example = "TEXT")
    private String generateType;

    @Schema(description = "生成模式", example = "TEXT_TO_IMAGE")
    private String generateMode;

    @Schema(description = "模型编号", example = "1024")
    private Long modelId;

    @Schema(description = "渠道编码", example = "openai")
    private String providerCode;

    @Schema(description = "状态", example = "SUCCESS")
    private String status;

}
