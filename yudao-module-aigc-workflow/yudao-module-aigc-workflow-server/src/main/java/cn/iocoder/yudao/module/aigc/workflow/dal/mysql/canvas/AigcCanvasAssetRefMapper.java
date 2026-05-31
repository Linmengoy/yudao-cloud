package cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasAssetRefDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcCanvasAssetRefMapper extends BaseMapperX<AigcCanvasAssetRefDO> {

    default AigcCanvasAssetRefDO selectByNodeAndAsset(Long projectId, String nodeId, Long assetId, String usageType) {
        return selectOne(new LambdaQueryWrapperX<AigcCanvasAssetRefDO>()
                .eq(AigcCanvasAssetRefDO::getProjectId, projectId)
                .eq(AigcCanvasAssetRefDO::getNodeId, nodeId)
                .eq(AigcCanvasAssetRefDO::getAssetId, assetId)
                .eq(AigcCanvasAssetRefDO::getUsageType, usageType));
    }

}
