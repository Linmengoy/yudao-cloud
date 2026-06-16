package cn.iocoder.yudao.module.aigc.gen.controller.admin.providerlog.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AIGC 渠道调用日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcGenerateProviderLogPageReqVO extends PageParam {

    @Schema(description = "生成记录编号", example = "1024")
    private Long recordId;

    @Schema(description = "任务编号", example = "2048")
    private Long taskId;

    @Schema(description = "Attempt id", example = "4096")
    private Long attemptId;

    @Schema(description = "渠道编码", example = "openai")
    private String providerCode;

    @Schema(description = "Model code", example = "gpt-image")
    private String modelCode;

    @Schema(description = "调用动作", example = "submit")
    private String apiAction;

    @Schema(description = "是否成功", example = "true")
    private Boolean success;

}
