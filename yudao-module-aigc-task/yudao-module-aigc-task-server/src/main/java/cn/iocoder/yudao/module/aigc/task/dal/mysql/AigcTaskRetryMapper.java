package cn.iocoder.yudao.module.aigc.task.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.task.controller.admin.retry.vo.AigcTaskRetryPageReqVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskRetryDO;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskRetryStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AigcTaskRetryMapper extends BaseMapperX<AigcTaskRetryDO> {

    default PageResult<AigcTaskRetryDO> selectPage(AigcTaskRetryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcTaskRetryDO>()
                .eqIfPresent(AigcTaskRetryDO::getTaskId, reqVO.getTaskId())
                .likeIfPresent(AigcTaskRetryDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(AigcTaskRetryDO::getRetryStatus, reqVO.getRetryStatus())
                .orderByDesc(AigcTaskRetryDO::getId));
    }

    default List<AigcTaskRetryDO> selectWaitingRetries(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<AigcTaskRetryDO>()
                .eq(AigcTaskRetryDO::getRetryStatus, AigcTaskRetryStatusEnum.WAITING.getCode())
                .le(AigcTaskRetryDO::getNextRetryTime, now)
                .orderByAsc(AigcTaskRetryDO::getNextRetryTime));
    }

}
