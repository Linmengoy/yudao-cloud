package cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectRecycleBinPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasProjectRecycleBinDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AigcCanvasProjectRecycleBinMapper extends BaseMapperX<AigcCanvasProjectRecycleBinDO> {

    default PageResult<AigcCanvasProjectRecycleBinDO> selectPage(AigcCanvasProjectRecycleBinPageReqVO reqVO, Long userId) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcCanvasProjectRecycleBinDO>()
                .eq(AigcCanvasProjectRecycleBinDO::getOwnerUserId, userId)
                .likeIfPresent(AigcCanvasProjectRecycleBinDO::getProjectName, reqVO.getName())
                .orderByDesc(AigcCanvasProjectRecycleBinDO::getDeletedTime));
    }

    default AigcCanvasProjectRecycleBinDO selectByProjectId(Long projectId) {
        return selectOne(AigcCanvasProjectRecycleBinDO::getProjectId, projectId);
    }

    @Update("UPDATE aigc_canvas_project SET deleted = 0 WHERE id = #{projectId} AND deleted = 1")
    int restoreProject(@Param("projectId") Long projectId);

    @Delete("DELETE FROM aigc_canvas_project_recycle_bin WHERE id = #{id}")
    int deletePhysicallyById(@Param("id") Long id);

}
