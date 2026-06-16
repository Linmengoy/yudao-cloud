package cn.iocoder.yudao.module.aigc.asset.service.template;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplateModelRespVO;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplatePageReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplateRespVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcPromptTemplateDO;

import java.util.List;

public interface AigcPromptTemplateService {

    AigcPromptTemplateDO validateTemplateAvailable(Long id);

    AigcPromptTemplateRespVO getTemplate(Long id);

    PageResult<AigcPromptTemplateRespVO> getTemplatePage(AigcPromptTemplatePageReqVO reqVO);

    List<String> getCategoryList();

    List<AigcPromptTemplateModelRespVO> getModelList();

    void increaseViewCount(Long id);

    void increaseCopyCount(Long id);

    void increaseUseCount(Long id);

}
