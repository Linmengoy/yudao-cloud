package cn.iocoder.yudao.module.aigc.model.service.model;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.model.controller.admin.model.vo.AigcModelPageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.model.vo.AigcModelSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;

import java.util.Collection;
import java.util.List;

public interface AigcModelService {

    Long createModel(AigcModelSaveReqVO reqVO);

    void updateModel(AigcModelSaveReqVO reqVO);

    void deleteModel(Long id);

    AigcModelDO getModel(Long id);

    AigcModelDO validateModelExists(Long id);

    AigcModelDO validateModelExistsAndEnable(Long id);

    AigcModelDO validateTenantModel(Long id, String capability);

    PageResult<AigcModelDO> getModelPage(AigcModelPageReqVO reqVO);

    List<AigcModelDO> getModelList(Collection<Long> ids);

    void updateModelStatus(Long id, Integer status);

    void updateModelVisible(Long id, Boolean publicVisible);

    void updateModelDefault(Long id, Boolean defaultModel);

    List<AigcModelDO> listModelsByType(Integer type);

    List<AigcModelDO> listTenantAvailableModels(Integer type);

    AigcModelDO getTenantVisibleModel(Long id);

    void validateModelCapability(Long modelId, String capability);

    boolean hasModelCapability(Long modelId, String capability);

    List<String> getModelCapabilities(Long modelId);

}
