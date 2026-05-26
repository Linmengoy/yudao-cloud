package cn.iocoder.yudao.module.aigc.asset.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetDownloadLogPageReqVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDownloadLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcAssetDownloadLogMapper extends BaseMapperX<AigcAssetDownloadLogDO> {

    default Long selectSuccessCount() {
        return selectCount(AigcAssetDownloadLogDO::getResult, "SUCCESS");
    }

    default PageResult<AigcAssetDownloadLogDO> selectPage(AigcAssetDownloadLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcAssetDownloadLogDO>()
                .eqIfPresent(AigcAssetDownloadLogDO::getAssetId, reqVO.getAssetId())
                .eqIfPresent(AigcAssetDownloadLogDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AigcAssetDownloadLogDO::getOwnerUserId, reqVO.getOwnerUserId())
                .eqIfPresent(AigcAssetDownloadLogDO::getResult, reqVO.getResult())
                .orderByDesc(AigcAssetDownloadLogDO::getId));
    }

}
