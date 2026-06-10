package cn.iocoder.yudao.module.aigc.gen.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateAttemptDO;
import cn.iocoder.yudao.module.aigc.gen.enums.AigcGenerateAttemptStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AigcGenerateAttemptMapper extends BaseMapperX<AigcGenerateAttemptDO> {

    default List<AigcGenerateAttemptDO> selectListByRecordId(Long recordId) {
        return selectList(new LambdaQueryWrapperX<AigcGenerateAttemptDO>()
                .eq(AigcGenerateAttemptDO::getRecordId, recordId)
                .orderByAsc(AigcGenerateAttemptDO::getAttemptNo));
    }

    default List<AigcGenerateAttemptDO> selectActiveListByRecordId(Long recordId) {
        return selectList(new LambdaQueryWrapperX<AigcGenerateAttemptDO>()
                .eq(AigcGenerateAttemptDO::getRecordId, recordId)
                .in(AigcGenerateAttemptDO::getStatus, List.of(AigcGenerateAttemptStatusEnum.SUBMITTING.getCode(),
                        AigcGenerateAttemptStatusEnum.SUBMITTED.getCode(), AigcGenerateAttemptStatusEnum.CALLBACK_WAITING.getCode()))
                .orderByDesc(AigcGenerateAttemptDO::getId));
    }

    default AigcGenerateAttemptDO selectByProviderTask(String providerCode, String providerTaskId) {
        return selectOne(AigcGenerateAttemptDO::getProviderCode, providerCode,
                AigcGenerateAttemptDO::getProviderTaskId, providerTaskId);
    }

    default AigcGenerateAttemptDO selectWinnerByRecordId(Long recordId) {
        return selectOne(AigcGenerateAttemptDO::getRecordId, recordId, AigcGenerateAttemptDO::getWinner, true);
    }

    default Long selectAttemptCount(Long recordId) {
        return selectCount(new LambdaQueryWrapperX<AigcGenerateAttemptDO>()
                .eq(AigcGenerateAttemptDO::getRecordId, recordId));
    }

    default int updateByIdAndStatus(AigcGenerateAttemptDO updateObj, String status) {
        return update(updateObj, new LambdaUpdateWrapper<AigcGenerateAttemptDO>()
                .eq(AigcGenerateAttemptDO::getId, updateObj.getId())
                .eq(AigcGenerateAttemptDO::getStatus, status));
    }

    default int updateStatusByIds(Collection<Long> ids, String status, String failCode, String failReason) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return update(new AigcGenerateAttemptDO().setStatus(status).setFailCode(failCode).setFailReason(failReason),
                new LambdaUpdateWrapper<AigcGenerateAttemptDO>().in(AigcGenerateAttemptDO::getId, ids));
    }

}
