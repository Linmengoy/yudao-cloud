package cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasProjectDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

@Mapper
public interface AigcCanvasProjectMapper extends BaseMapperX<AigcCanvasProjectDO> {

    default PageResult<AigcCanvasProjectDO> selectPage(AigcCanvasProjectPageReqVO reqVO, Long userId, Collection<Long> sharedProjectIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcCanvasProjectDO>()
                .likeIfPresent(AigcCanvasProjectDO::getName, reqVO.getName())
                .eqIfPresent(AigcCanvasProjectDO::getStatus, reqVO.getStatus())
                .and(wrapper -> {
                    wrapper.eq(AigcCanvasProjectDO::getOwnerUserId, userId);
                    if (sharedProjectIds != null && !sharedProjectIds.isEmpty()) {
                        wrapper.or().in(AigcCanvasProjectDO::getId, sharedProjectIds);
                    }
                })
                .orderByDesc(AigcCanvasProjectDO::getUpdateTime));
    }

    default AigcCanvasProjectDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(AigcCanvasProjectDO::getId, id);
    }

    default int updateCoverAssetIfAbsent(Long id, Long coverAssetId) {
        return update(null, new LambdaUpdateWrapper<AigcCanvasProjectDO>()
                .eq(AigcCanvasProjectDO::getId, id)
                .isNull(AigcCanvasProjectDO::getCoverAssetId)
                .set(AigcCanvasProjectDO::getCoverAssetId, coverAssetId));
    }

    default int updateStatistics(Long id, Integer nodeCount, Integer assetCount) {
        return update(null, new LambdaUpdateWrapper<AigcCanvasProjectDO>()
                .eq(AigcCanvasProjectDO::getId, id)
                .set(AigcCanvasProjectDO::getNodeCount, nodeCount)
                .set(AigcCanvasProjectDO::getAssetCount, assetCount));
    }

}
