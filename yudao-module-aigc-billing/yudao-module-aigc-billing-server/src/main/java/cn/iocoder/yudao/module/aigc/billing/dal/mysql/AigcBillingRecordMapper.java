package cn.iocoder.yudao.module.aigc.billing.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcBillingRecordMapper extends BaseMapperX<AigcBillingRecordDO> {

    default PageResult<AigcBillingRecordDO> selectPage(PageParam reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcBillingRecordDO>()
                .orderByDesc(AigcBillingRecordDO::getId));
    }

    default PageResult<AigcBillingRecordDO> selectUserPage(PageParam reqVO, Long userId) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcBillingRecordDO>()
                .eq(AigcBillingRecordDO::getUserId, userId)
                .orderByDesc(AigcBillingRecordDO::getId));
    }

    default AigcBillingRecordDO selectByBiz(String bizType, String bizId) {
        return selectOne(new LambdaQueryWrapperX<AigcBillingRecordDO>()
                .eq(AigcBillingRecordDO::getBizType, bizType)
                .eq(AigcBillingRecordDO::getBizId, bizId));
    }

}
