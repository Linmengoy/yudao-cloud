package cn.iocoder.yudao.module.aigc.model.service.tenant;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.tenant.vo.AigcModelTenantSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelTenantDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelTenantMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AigcModelTenantServiceImpl implements AigcModelTenantService {

    @Resource
    private AigcModelTenantMapper tenantMapper;

    @Resource
    private AigcModelMapper modelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTenantModel(AigcModelTenantSaveReqVO reqVO) {
        validateModelExists(reqVO.getModelId());

        AigcModelTenantDO tenantModel = BeanUtils.toBean(reqVO, AigcModelTenantDO.class);
        TenantUtils.executeIgnore(() -> tenantMapper.insert(tenantModel));

        if (Boolean.TRUE.equals(reqVO.getDefaultModel())) {
            TenantUtils.executeIgnore(() -> tenantMapper.updateDefaultModelToFalse(reqVO.getTenantId(), tenantModel.getId(), reqVO.getModelId()));
        }

        return tenantModel.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTenantModel(AigcModelTenantSaveReqVO reqVO) {
        validateTenantModelExists(reqVO.getId());

        AigcModelTenantDO updateObj = BeanUtils.toBean(reqVO, AigcModelTenantDO.class);
        TenantUtils.executeIgnore(() -> tenantMapper.updateById(updateObj));

        if (Boolean.TRUE.equals(reqVO.getDefaultModel())) {
            TenantUtils.executeIgnore(() -> tenantMapper.updateDefaultModelToFalse(reqVO.getTenantId(), reqVO.getId(), reqVO.getModelId()));
        }
    }

    @Override
    public void deleteTenantModel(Long id) {
        validateTenantModelExists(id);
        TenantUtils.executeIgnore(() -> tenantMapper.deleteById(id));
    }

    @Override
    public AigcModelTenantDO getTenantModel(Long id) {
        return TenantUtils.executeIgnore(() -> tenantMapper.selectById(id));
    }

    @Override
    public List<AigcModelTenantDO> getTenantModelList(Long tenantId) {
        return TenantUtils.executeIgnore(() -> tenantMapper.selectListByTenantId(tenantId));
    }

    @Override
    public void updateTenantModelStatus(Long id, Boolean enabled) {
        validateTenantModelExists(id);
        TenantUtils.executeIgnore(() -> tenantMapper.updateById(new AigcModelTenantDO().setId(id).setEnabled(enabled)));
    }

    @Override
    public void updateTenantModelVisible(Long id, Boolean publicVisible) {
        validateTenantModelExists(id);
        TenantUtils.executeIgnore(() -> tenantMapper.updateById(new AigcModelTenantDO().setId(id).setPublicVisible(publicVisible)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTenantModelDefault(Long id, Boolean defaultModel) {
        validateTenantModelExists(id);
        AigcModelTenantDO tenantModel = TenantUtils.executeIgnore(() -> tenantMapper.selectById(id));

        TenantUtils.executeIgnore(() -> tenantMapper.updateById(new AigcModelTenantDO().setId(id).setDefaultModel(defaultModel)));

        if (Boolean.TRUE.equals(defaultModel)) {
            TenantUtils.executeIgnore(() -> tenantMapper.updateDefaultModelToFalse(tenantModel.getTenantId(), id, tenantModel.getModelId()));
        }
    }

    @Override
    public List<AigcModelTenantDO> listEnabledTenantModels(Long tenantId) {
        return TenantUtils.executeIgnore(() -> tenantMapper.selectListByEnabledTrue(tenantId));
    }

    @Override
    public Long getDefaultModel(Long tenantId) {
        List<AigcModelTenantDO> list = TenantUtils.executeIgnore(() -> tenantMapper.selectListByTenantId(tenantId));
        return list.stream()
                .filter(t -> Boolean.TRUE.equals(t.getDefaultModel()))
                .map(AigcModelTenantDO::getModelId)
                .findFirst()
                .orElse(null);
    }

    private void validateModelExists(Long modelId) {
        if (modelMapper.selectById(modelId) == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
    }

    private void validateTenantModelExists(Long id) {
        if (TenantUtils.executeIgnore(() -> tenantMapper.selectById(id)) == null) {
            throw exception(MODEL_TENANT_NOT_EXISTS);
        }
    }

}
