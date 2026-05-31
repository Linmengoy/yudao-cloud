package cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcCanvasMemberMapper extends BaseMapperX<AigcCanvasMemberDO> {

    default AigcCanvasMemberDO selectByProjectIdAndUserId(Long projectId, Long userId) {
        return selectOne(AigcCanvasMemberDO::getProjectId, projectId, AigcCanvasMemberDO::getUserId, userId);
    }

    default List<AigcCanvasMemberDO> selectListByProjectId(Long projectId) {
        return selectList(AigcCanvasMemberDO::getProjectId, projectId);
    }

}
