package cn.iocoder.yudao.module.aigc.billing.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcCostRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcCostRecordMapper extends BaseMapperX<AigcCostRecordDO> {

    default AigcCostRecordDO selectByTaskId(Long taskId) {
        return selectOne(new LambdaQueryWrapperX<AigcCostRecordDO>()
                .eq(AigcCostRecordDO::getTaskId, taskId)
                .orderByAsc(AigcCostRecordDO::getId)
                .last("LIMIT 1"));
    }

    default List<AigcCostRecordDO> selectListByTaskId(Long taskId) {
        return selectList(AigcCostRecordDO::getTaskId, taskId);
    }

    default AigcCostRecordDO selectByAttemptId(Long attemptId) {
        return selectOne(AigcCostRecordDO::getAttemptId, attemptId);
    }

    default PageResult<AigcCostRecordDO> selectPage(PageParam reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcCostRecordDO>()
                .orderByDesc(AigcCostRecordDO::getId));
    }

}
