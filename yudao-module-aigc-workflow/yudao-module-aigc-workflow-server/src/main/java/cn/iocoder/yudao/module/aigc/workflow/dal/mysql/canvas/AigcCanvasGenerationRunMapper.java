package cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasGenerationRunDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AigcCanvasGenerationRunMapper extends BaseMapperX<AigcCanvasGenerationRunDO> {

    default AigcCanvasGenerationRunDO selectByTaskId(Long taskId) {
        return selectOne(new LambdaQueryWrapperX<AigcCanvasGenerationRunDO>()
                .eq(AigcCanvasGenerationRunDO::getTaskId, taskId));
    }

    default AigcCanvasGenerationRunDO selectByProjectNodeAndTask(Long projectId, String nodeId, Long taskId) {
        return selectOne(new LambdaQueryWrapperX<AigcCanvasGenerationRunDO>()
                .eq(AigcCanvasGenerationRunDO::getProjectId, projectId)
                .eq(AigcCanvasGenerationRunDO::getNodeId, nodeId)
                .eq(AigcCanvasGenerationRunDO::getTaskId, taskId));
    }

    default AigcCanvasGenerationRunDO selectByProjectNodeAndRun(Long projectId, String nodeId, String runId) {
        return selectOne(new LambdaQueryWrapperX<AigcCanvasGenerationRunDO>()
                .eq(AigcCanvasGenerationRunDO::getProjectId, projectId)
                .eq(AigcCanvasGenerationRunDO::getNodeId, nodeId)
                .eq(AigcCanvasGenerationRunDO::getRunId, runId));
    }

    default List<AigcCanvasGenerationRunDO> selectListByProjectAndTasks(Long projectId, Collection<Long> taskIds) {
        return selectList(new LambdaQueryWrapperX<AigcCanvasGenerationRunDO>()
                .eq(AigcCanvasGenerationRunDO::getProjectId, projectId)
                .in(AigcCanvasGenerationRunDO::getTaskId, taskIds));
    }

}
