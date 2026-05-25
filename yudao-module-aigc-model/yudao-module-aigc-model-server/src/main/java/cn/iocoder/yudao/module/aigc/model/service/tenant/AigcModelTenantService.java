package cn.iocoder.yudao.module.aigc.model.service.tenant;

import cn.iocoder.yudao.module.aigc.model.controller.admin.tenant.vo.AigcModelTenantSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelTenantDO;

import java.util.List;

public interface AigcModelTenantService {

    Long createTenantModel(AigcModelTenantSaveReqVO reqVO);

    void updateTenantModel(AigcModelTenantSaveReqVO reqVO);

    void deleteTenantModel(Long id);

    AigcModelTenantDO getTenantModel(Long id);

    List<AigcModelTenantDO> getTenantModelList(Long tenantId);

    void updateTenantModelStatus(Long id, Boolean enabled);

    void updateTenantModelVisible(Long id, Boolean publicVisible);

    void updateTenantModelDefault(Long id, Boolean defaultModel);

    List<AigcModelTenantDO> listEnabledTenantModels(Long tenantId);

    Long getDefaultModel(Long tenantId);

}