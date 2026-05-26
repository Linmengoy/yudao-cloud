package cn.iocoder.yudao.module.aigc.task.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.task.controller.admin.log.vo.AigcTaskLogPageReqVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcTaskLogMapper extends BaseMapperX<AigcTaskLogDO> {

    default PageResult<AigcTaskLogDO> selectPage(AigcTaskLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcTaskLogDO>()
                .eqIfPresent(AigcTaskLogDO::getTaskId, reqVO.getTaskId())
                .likeIfPresent(AigcTaskLogDO::getTaskNo, reqVO.getTaskNo())
                .orderByDesc(AigcTaskLogDO::getId));
    }

    default List<AigcTaskLogDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<AigcTaskLogDO>()
                .eq(AigcTaskLogDO::getTaskId, taskId)
                .orderByAsc(AigcTaskLogDO::getId));
    }

}
