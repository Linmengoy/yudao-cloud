package cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasSketchDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcCanvasSketchMapper extends BaseMapperX<AigcCanvasSketchDO> {

    default AigcCanvasSketchDO selectByProjectIdAndNodeId(Long projectId, String nodeId) {
        return selectOne(new LambdaQueryWrapperX<AigcCanvasSketchDO>()
                .eq(AigcCanvasSketchDO::getProjectId, projectId)
                .eq(AigcCanvasSketchDO::getNodeId, nodeId));
    }

}
