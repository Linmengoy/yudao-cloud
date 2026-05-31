package cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasOperationLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcCanvasOperationLogMapper extends BaseMapperX<AigcCanvasOperationLogDO> {

    default AigcCanvasOperationLogDO selectByClientOperation(Long projectId, String clientId, String opId) {
        return selectOne(new LambdaQueryWrapperX<AigcCanvasOperationLogDO>()
                .eq(AigcCanvasOperationLogDO::getProjectId, projectId)
                .eq(AigcCanvasOperationLogDO::getClientId, clientId)
                .eq(AigcCanvasOperationLogDO::getOpId, opId));
    }

    default List<AigcCanvasOperationLogDO> selectListAfterVersion(Long projectId, Long afterVersion) {
        return selectList(new LambdaQueryWrapperX<AigcCanvasOperationLogDO>()
                .eq(AigcCanvasOperationLogDO::getProjectId, projectId)
                .gt(AigcCanvasOperationLogDO::getNextVersion, afterVersion)
                .orderByAsc(AigcCanvasOperationLogDO::getNextVersion));
    }

    default AigcCanvasOperationLogDO selectMinByProjectId(Long projectId) {
        return selectOne(new LambdaQueryWrapperX<AigcCanvasOperationLogDO>()
                .eq(AigcCanvasOperationLogDO::getProjectId, projectId)
                .orderByAsc(AigcCanvasOperationLogDO::getNextVersion)
                .last("LIMIT 1"));
    }

    default List<AigcCanvasOperationLogDO> selectListByProjectIdAndType(Long projectId, String operationType) {
        return selectList(new LambdaQueryWrapperX<AigcCanvasOperationLogDO>()
                .eq(AigcCanvasOperationLogDO::getProjectId, projectId)
                .eq(AigcCanvasOperationLogDO::getOperationType, operationType)
                .orderByAsc(AigcCanvasOperationLogDO::getNextVersion));
    }

}
