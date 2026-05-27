package cn.iocoder.yudao.module.aigc.workflow.service.definition;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowDefinitionPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowDefinitionSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowDefinitionDO;

public interface AigcWorkflowDefinitionService {

    Long createDefinition(AigcWorkflowDefinitionSaveReqVO reqVO, Long userId);

    void updateDefinition(AigcWorkflowDefinitionSaveReqVO reqVO);

    void deleteDefinition(Long id);
    void publishDefinition(Long id);
    void offlineDefinition(Long id);
    AigcWorkflowDefinitionDO getDefinition(Long id);
    AigcWorkflowDefinitionDO validateDefinitionExists(Long id);
    PageResult<AigcWorkflowDefinitionDO> getDefinitionPage(AigcWorkflowDefinitionPageReqVO reqVO);

}
