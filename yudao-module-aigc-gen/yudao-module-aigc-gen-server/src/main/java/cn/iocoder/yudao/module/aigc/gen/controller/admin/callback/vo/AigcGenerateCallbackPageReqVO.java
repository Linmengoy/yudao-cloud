package cn.iocoder.yudao.module.aigc.gen.controller.admin.callback.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AIGC 生成回调分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcGenerateCallbackPageReqVO extends PageParam {

    @Schema(description = "生成记录编号", example = "1024")
    private Long recordId;

    @Schema(description = "任务编号", example = "2048")
    private Long taskId;

    @Schema(description = "渠道编码", example = "openai")
    private String providerCode;

    @Schema(description = "第三方任务编号")
    private String providerTaskId;

    @Schema(description = "处理状态", example = "SUCCESS")
    private String processStatus;

}
