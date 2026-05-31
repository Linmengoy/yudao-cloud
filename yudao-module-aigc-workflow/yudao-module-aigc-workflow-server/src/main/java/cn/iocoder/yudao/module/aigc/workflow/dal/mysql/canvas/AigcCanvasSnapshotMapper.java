package cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasSnapshotDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcCanvasSnapshotMapper extends BaseMapperX<AigcCanvasSnapshotDO> {

    default AigcCanvasSnapshotDO selectLatestByProjectId(Long projectId) {
        return selectOne(new LambdaQueryWrapperX<AigcCanvasSnapshotDO>()
                .eq(AigcCanvasSnapshotDO::getProjectId, projectId)
                .orderByDesc(AigcCanvasSnapshotDO::getVersion)
                .last("LIMIT 1"));
    }

}
