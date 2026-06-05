package cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasMemberDO;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface AigcCanvasMemberMapper extends BaseMapperX<AigcCanvasMemberDO> {

    default AigcCanvasMemberDO selectByProjectIdAndUserId(Long projectId, Long userId) {
        return selectOne(AigcCanvasMemberDO::getProjectId, projectId, AigcCanvasMemberDO::getUserId, userId);
    }

    default List<AigcCanvasMemberDO> selectListByUserId(Long userId) {
        return selectList(AigcCanvasMemberDO::getUserId, userId);
    }

    default List<AigcCanvasMemberDO> selectListByProjectIdsAndUserId(Collection<Long> projectIds, Long userId) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<AigcCanvasMemberDO>()
        .in(AigcCanvasMemberDO::getProjectId, projectIds)
        .eq(AigcCanvasMemberDO::getUserId, userId));
    }

    default List<AigcCanvasMemberDO> selectListByProjectId(Long projectId) {
        return selectList(AigcCanvasMemberDO::getProjectId, projectId);
    }

    default AigcCanvasMemberDO selectByProjectIdAndId(Long projectId, Long id) {
        return selectOne(AigcCanvasMemberDO::getProjectId, projectId, AigcCanvasMemberDO::getId, id);
    }

}
