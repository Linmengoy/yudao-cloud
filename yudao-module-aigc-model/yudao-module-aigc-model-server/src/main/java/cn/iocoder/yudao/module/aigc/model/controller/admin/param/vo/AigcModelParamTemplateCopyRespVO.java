package cn.iocoder.yudao.module.aigc.model.controller.admin.param.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AigcModelParamTemplateCopyRespVO {

    private Integer createdCount;

    private Integer updatedCount;

    private Integer skippedCount;

}
