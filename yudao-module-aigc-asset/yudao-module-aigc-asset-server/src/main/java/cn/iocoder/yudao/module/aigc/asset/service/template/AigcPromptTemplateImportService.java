package cn.iocoder.yudao.module.aigc.asset.service.template;

import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcPromptTemplateImportReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcPromptTemplateImportRespVO;
import org.springframework.web.multipart.MultipartFile;

public interface AigcPromptTemplateImportService {

    AigcPromptTemplateImportRespVO importAwesomeGptImageCases(AigcPromptTemplateImportReqVO reqVO);

    AigcPromptTemplateImportRespVO importAwesomeGptImageCaseFiles(MultipartFile casesJson, MultipartFile[] images,
                                                                  String storageDirectory);

}
