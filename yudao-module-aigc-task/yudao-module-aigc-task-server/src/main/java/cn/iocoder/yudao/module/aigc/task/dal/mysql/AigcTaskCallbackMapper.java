package cn.iocoder.yudao.module.aigc.task.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.task.controller.admin.callback.vo.AigcTaskCallbackPageReqVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskCallbackDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcTaskCallbackMapper extends BaseMapperX<AigcTaskCallbackDO> {

    default AigcTaskCallbackDO selectByExternalCallback(String providerCode, String externalTaskId, String callbackType) {
        return selectOne(new LambdaQueryWrapperX<AigcTaskCallbackDO>()
                .eq(AigcTaskCallbackDO::getProviderCode, providerCode)
                .eq(AigcTaskCallbackDO::getExternalTaskId, externalTaskId)
                .eq(AigcTaskCallbackDO::getCallbackType, callbackType));
    }

    default PageResult<AigcTaskCallbackDO> selectPage(AigcTaskCallbackPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcTaskCallbackDO>()
                .eqIfPresent(AigcTaskCallbackDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(AigcTaskCallbackDO::getProviderCode, reqVO.getProviderCode())
                .eqIfPresent(AigcTaskCallbackDO::getExternalTaskId, reqVO.getExternalTaskId())
                .eqIfPresent(AigcTaskCallbackDO::getCallbackStatus, reqVO.getCallbackStatus())
                .orderByDesc(AigcTaskCallbackDO::getId));
    }

}
