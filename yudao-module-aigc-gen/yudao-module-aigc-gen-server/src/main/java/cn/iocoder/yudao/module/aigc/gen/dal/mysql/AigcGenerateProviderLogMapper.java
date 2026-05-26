package cn.iocoder.yudao.module.aigc.gen.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.providerlog.vo.AigcGenerateProviderLogPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateProviderLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcGenerateProviderLogMapper extends BaseMapperX<AigcGenerateProviderLogDO> {

    default PageResult<AigcGenerateProviderLogDO> selectPage(AigcGenerateProviderLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcGenerateProviderLogDO>()
                .eqIfPresent(AigcGenerateProviderLogDO::getRecordId, reqVO.getRecordId())
                .eqIfPresent(AigcGenerateProviderLogDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(AigcGenerateProviderLogDO::getProviderCode, reqVO.getProviderCode())
                .eqIfPresent(AigcGenerateProviderLogDO::getApiAction, reqVO.getApiAction())
                .eqIfPresent(AigcGenerateProviderLogDO::getSuccess, reqVO.getSuccess())
                .orderByDesc(AigcGenerateProviderLogDO::getId));
    }
}
