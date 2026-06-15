package cn.iocoder.yudao.module.aigc.asset.service.template;

import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcPromptTemplateImportReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcPromptTemplateImportRespVO;

public interface AigcPromptTemplateImportService {

    AigcPromptTemplateImportRespVO importAwesomeGptImageCases(AigcPromptTemplateImportReqVO reqVO);

}
