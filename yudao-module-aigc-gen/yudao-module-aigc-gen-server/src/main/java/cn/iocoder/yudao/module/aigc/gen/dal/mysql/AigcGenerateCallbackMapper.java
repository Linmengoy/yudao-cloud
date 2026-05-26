package cn.iocoder.yudao.module.aigc.gen.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.callback.vo.AigcGenerateCallbackPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateCallbackDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcGenerateCallbackMapper extends BaseMapperX<AigcGenerateCallbackDO> {

    default PageResult<AigcGenerateCallbackDO> selectPage(AigcGenerateCallbackPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcGenerateCallbackDO>()
                .eqIfPresent(AigcGenerateCallbackDO::getRecordId, reqVO.getRecordId())
                .eqIfPresent(AigcGenerateCallbackDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(AigcGenerateCallbackDO::getProviderCode, reqVO.getProviderCode())
                .eqIfPresent(AigcGenerateCallbackDO::getProviderTaskId, reqVO.getProviderTaskId())
                .eqIfPresent(AigcGenerateCallbackDO::getProcessStatus, reqVO.getProcessStatus())
                .orderByDesc(AigcGenerateCallbackDO::getId));
    }

    default AigcGenerateCallbackDO selectByCallbackNo(String providerCode, String callbackNo) {
        return selectOne(AigcGenerateCallbackDO::getProviderCode, providerCode, AigcGenerateCallbackDO::getCallbackNo, callbackNo);
    }
}
