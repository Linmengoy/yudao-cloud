package cn.iocoder.yudao.module.aigc.model.service.param;

import cn.iocoder.yudao.module.aigc.model.controller.admin.param.vo.AigcModelParamTemplateSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.param.vo.AigcModelParamTemplateCopyReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.param.vo.AigcModelParamTemplateCopyRespVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelParamTemplateDO;

import java.util.List;

public interface AigcModelParamService {

    Long createParamTemplate(AigcModelParamTemplateSaveReqVO reqVO);

    AigcModelParamTemplateCopyRespVO copyParamTemplates(AigcModelParamTemplateCopyReqVO reqVO);

    void updateParamTemplate(AigcModelParamTemplateSaveReqVO reqVO);

    void deleteParamTemplate(Long id);

    AigcModelParamTemplateDO getParamTemplate(Long id);

    List<AigcModelParamTemplateDO> getParamTemplateList(Long modelId, String capability);

    void validateParams(Long modelId, String capability, java.util.Map<String, Object> params);

}
