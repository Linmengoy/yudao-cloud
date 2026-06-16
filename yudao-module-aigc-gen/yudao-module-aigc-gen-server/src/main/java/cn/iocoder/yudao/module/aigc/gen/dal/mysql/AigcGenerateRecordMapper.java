package cn.iocoder.yudao.module.aigc.gen.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.record.vo.AigcGenerateRecordPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateRecordDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface AigcGenerateRecordMapper extends BaseMapperX<AigcGenerateRecordDO> {

    default PageResult<AigcGenerateRecordDO> selectPage(AigcGenerateRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcGenerateRecordDO>()
                .eqIfPresent(AigcGenerateRecordDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AigcGenerateRecordDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(AigcGenerateRecordDO::getGenerateNo, reqVO.getGenerateNo())
                .eqIfPresent(AigcGenerateRecordDO::getGenerateType, reqVO.getGenerateType())
                .eqIfPresent(AigcGenerateRecordDO::getGenerateMode, reqVO.getGenerateMode())
                .eqIfPresent(AigcGenerateRecordDO::getModelId, reqVO.getModelId())
                .eqIfPresent(AigcGenerateRecordDO::getProviderCode, reqVO.getProviderCode())
                .eqIfPresent(AigcGenerateRecordDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AigcGenerateRecordDO::getProviderTaskId, reqVO.getProviderTaskId())
                .likeIfPresent(AigcGenerateRecordDO::getFailReason, reqVO.getFailReason())
                .betweenIfPresent(AigcGenerateRecordDO::getCreateTime, reqVO.getCreateTime())
                .betweenIfPresent(AigcGenerateRecordDO::getSubmitTime, reqVO.getSubmitTime())
                .and(Boolean.TRUE.equals(reqVO.getHasError()), wrapper -> wrapper
                        .isNotNull(AigcGenerateRecordDO::getFailReason)
                        .or()
                        .isNotNull(AigcGenerateRecordDO::getFailMessage))
                .orderByDesc(AigcGenerateRecordDO::getId));
    }

    default AigcGenerateRecordDO selectByTaskId(Long taskId) {
        return selectOne(AigcGenerateRecordDO::getTaskId, taskId);
    }

    default AigcGenerateRecordDO selectByGenerateNo(String generateNo) {
        return selectOne(AigcGenerateRecordDO::getGenerateNo, generateNo);
    }

    default AigcGenerateRecordDO selectByClientRequestId(Long userId, String clientRequestId) {
        return selectOne(AigcGenerateRecordDO::getUserId, userId, AigcGenerateRecordDO::getClientRequestId, clientRequestId);
    }

    default AigcGenerateRecordDO selectByProviderTask(String providerCode, String providerTaskId) {
        return selectOne(AigcGenerateRecordDO::getProviderCode, providerCode, AigcGenerateRecordDO::getProviderTaskId, providerTaskId);
    }

    default int updateByIdAndStatus(AigcGenerateRecordDO updateObj, String status) {
        return update(updateObj, new LambdaUpdateWrapper<AigcGenerateRecordDO>()
                .eq(AigcGenerateRecordDO::getId, updateObj.getId())
                .eq(AigcGenerateRecordDO::getStatus, status));
    }

    default int updateByIdAndStatuses(AigcGenerateRecordDO updateObj, Collection<String> statuses) {
        return update(updateObj, new LambdaUpdateWrapper<AigcGenerateRecordDO>()
                .eq(AigcGenerateRecordDO::getId, updateObj.getId())
                .in(AigcGenerateRecordDO::getStatus, statuses));
    }

    default List<AigcGenerateRecordDO> selectTimeoutList(Collection<String> statuses, LocalDateTime beforeTime) {
        return selectList(new LambdaQueryWrapperX<AigcGenerateRecordDO>()
                .in(AigcGenerateRecordDO::getStatus, statuses)
                .lt(AigcGenerateRecordDO::getUpdateTime, beforeTime)
                .orderByAsc(AigcGenerateRecordDO::getId));
    }
}
