package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelUsageLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcModelUsageLogMapper extends BaseMapperX<AigcModelUsageLogDO> {
}
