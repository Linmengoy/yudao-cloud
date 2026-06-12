package cn.iocoder.yudao.module.aigc.model.controller.admin.param.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class AigcModelParamTemplateCopyReqVO {

    @NotNull(message = "源模型不能为空")
    private Long sourceModelId;

    @NotEmpty(message = "目标模型不能为空")
    private List<Long> targetModelIds;

    private List<String> capabilities;

    private Boolean overwrite;

}
