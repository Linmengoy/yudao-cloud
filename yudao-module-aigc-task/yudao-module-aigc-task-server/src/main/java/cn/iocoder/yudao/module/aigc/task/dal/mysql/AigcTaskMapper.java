package cn.iocoder.yudao.module.aigc.task.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.task.controller.admin.task.vo.AigcTaskPageReqVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface AigcTaskMapper extends BaseMapperX<AigcTaskDO> {

    default AigcTaskDO selectByTaskNo(String taskNo) {
        return selectOne(AigcTaskDO::getTaskNo, taskNo);
    }

    default AigcTaskDO selectByClientRequestId(Long userId, String clientRequestId) {
        return selectOne(new LambdaQueryWrapperX<AigcTaskDO>()
                .eq(AigcTaskDO::getUserId, userId)
                .eq(AigcTaskDO::getClientRequestId, clientRequestId));
    }

    default PageResult<AigcTaskDO> selectPage(AigcTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcTaskDO>()
                .eqIfPresent(AigcTaskDO::getUserId, reqVO.getUserId())
                .likeIfPresent(AigcTaskDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(AigcTaskDO::getTaskType, reqVO.getTaskType())
                .eqIfPresent(AigcTaskDO::getModelId, reqVO.getModelId())
                .eqIfPresent(AigcTaskDO::getStatus, reqVO.getStatus())
                .orderByDesc(AigcTaskDO::getId));
    }

    default PageResult<AigcTaskDO> selectPageByUserId(cn.iocoder.yudao.framework.common.pojo.PageParam reqVO, Long userId) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcTaskDO>()
                .eq(AigcTaskDO::getUserId, userId)
                .orderByDesc(AigcTaskDO::getId));
    }

    default int updateByIdAndStatus(AigcTaskDO updateObj, String fromStatus) {
        return update(updateObj, new LambdaUpdateWrapper<AigcTaskDO>()
                .eq(AigcTaskDO::getId, updateObj.getId())
                .eq(AigcTaskDO::getStatus, fromStatus));
    }

    default int incrementRetryCount(Long id) {
        return update(null, new LambdaUpdateWrapper<AigcTaskDO>()
                .setSql("retry_count = retry_count + 1")
                .eq(AigcTaskDO::getId, id));
    }

    default List<AigcTaskDO> selectTimeoutTasks(Collection<String> statuses, LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<AigcTaskDO>()
                .in(AigcTaskDO::getStatus, statuses)
                .isNotNull(AigcTaskDO::getExpireTime)
                .lt(AigcTaskDO::getExpireTime, now)
                .orderByAsc(AigcTaskDO::getExpireTime));
    }

}
