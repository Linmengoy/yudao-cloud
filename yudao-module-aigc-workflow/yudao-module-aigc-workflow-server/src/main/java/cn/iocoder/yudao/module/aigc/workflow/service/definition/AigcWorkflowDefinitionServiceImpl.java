package cn.iocoder.yudao.module.aigc.workflow.service.definition;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowDefinitionPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowDefinitionSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowDefinitionDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowDefinitionMapper;
import cn.iocoder.yudao.module.aigc.workflow.enums.AigcWorkflowStatusEnum;
import cn.iocoder.yudao.module.aigc.workflow.enums.AigcWorkflowVisibilityEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_DEFINITION_CODE_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_DEFINITION_NOT_EXISTS;

@Service
@Validated
public class AigcWorkflowDefinitionServiceImpl implements AigcWorkflowDefinitionService {

    @Resource
    private AigcWorkflowDefinitionMapper definitionMapper;

    @Override
    public Long createDefinition(AigcWorkflowDefinitionSaveReqVO reqVO, Long userId) {
        validateCodeUnique(reqVO.getCode(), null);
        AigcWorkflowDefinitionDO definition = BeanUtils.toBean(reqVO, AigcWorkflowDefinitionDO.class)
                .setStatus(AigcWorkflowStatusEnum.DRAFT.getCode())
                .setVisibility(StrUtil.blankToDefault(reqVO.getVisibility(), AigcWorkflowVisibilityEnum.PRIVATE.getCode()))
                .setCreatorUserId(userId);
        definitionMapper.insert(definition);
        return definition.getId();
    }

    @Override
    public void updateDefinition(AigcWorkflowDefinitionSaveReqVO reqVO) {
        validateDefinitionExists(reqVO.getId());
        validateCodeUnique(reqVO.getCode(), reqVO.getId());
        definitionMapper.updateById(BeanUtils.toBean(reqVO, AigcWorkflowDefinitionDO.class));
    }

    @Override
    public void deleteDefinition(Long id) {
        validateDefinitionExists(id);
        definitionMapper.deleteById(id);
    }

    @Override
    public void publishDefinition(Long id) {
        validateDefinitionExists(id);
        definitionMapper.updateById(new AigcWorkflowDefinitionDO().setId(id).setStatus(AigcWorkflowStatusEnum.PUBLISHED.getCode()));
    }

    @Override
    public void offlineDefinition(Long id) {
        validateDefinitionExists(id);
        definitionMapper.updateById(new AigcWorkflowDefinitionDO().setId(id).setStatus(AigcWorkflowStatusEnum.OFFLINE.getCode()));
    }

    @Override
    public AigcWorkflowDefinitionDO getDefinition(Long id) {
        return definitionMapper.selectById(id);
    }

    @Override
    public AigcWorkflowDefinitionDO validateDefinitionExists(Long id) {
        AigcWorkflowDefinitionDO definition = definitionMapper.selectById(id);
        if (definition == null) {
            throw exception(WORKFLOW_DEFINITION_NOT_EXISTS);
        }
        return definition;
    }

    @Override
    public PageResult<AigcWorkflowDefinitionDO> getDefinitionPage(AigcWorkflowDefinitionPageReqVO reqVO) {
        return definitionMapper.selectPage(reqVO);
    }

    private void validateCodeUnique(String code, Long id) {
        AigcWorkflowDefinitionDO definition = definitionMapper.selectByCode(code);
        if (definition == null) {
            return;
        }
        if (id == null || !definition.getId().equals(id)) {
            throw exception(WORKFLOW_DEFINITION_CODE_EXISTS);
        }
    }

}
