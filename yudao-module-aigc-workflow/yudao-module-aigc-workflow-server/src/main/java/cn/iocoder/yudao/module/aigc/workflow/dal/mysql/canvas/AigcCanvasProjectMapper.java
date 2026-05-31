package cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasProjectDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcCanvasProjectMapper extends BaseMapperX<AigcCanvasProjectDO> {

    default PageResult<AigcCanvasProjectDO> selectPage(AigcCanvasProjectPageReqVO reqVO, Long userId) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcCanvasProjectDO>()
                .eq(AigcCanvasProjectDO::getOwnerUserId, userId)
                .likeIfPresent(AigcCanvasProjectDO::getName, reqVO.getName())
                .eqIfPresent(AigcCanvasProjectDO::getKind, reqVO.getKind())
                .eqIfPresent(AigcCanvasProjectDO::getStatus, reqVO.getStatus())
                .orderByDesc(AigcCanvasProjectDO::getUpdateTime));
    }

    default AigcCanvasProjectDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(AigcCanvasProjectDO::getId, id);
    }

}
