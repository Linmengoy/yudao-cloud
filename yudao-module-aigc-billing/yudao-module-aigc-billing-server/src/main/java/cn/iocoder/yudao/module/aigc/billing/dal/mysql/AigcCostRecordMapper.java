package cn.iocoder.yudao.module.aigc.billing.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcCostRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcCostRecordMapper extends BaseMapperX<AigcCostRecordDO> {

    default AigcCostRecordDO selectByTaskId(Long taskId) {
        return selectOne(AigcCostRecordDO::getTaskId, taskId);
    }

    default PageResult<AigcCostRecordDO> selectPage(PageParam reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcCostRecordDO>()
                .orderByDesc(AigcCostRecordDO::getId));
    }

}
