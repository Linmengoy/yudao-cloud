package cn.iocoder.yudao.module.aigc.asset.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetFileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AigcAssetFileMapper extends BaseMapperX<AigcAssetFileDO> {

    default AigcAssetFileDO selectByAssetIdAndRole(Long assetId, String fileRole) {
        return selectOne(AigcAssetFileDO::getAssetId, assetId, AigcAssetFileDO::getFileRole, fileRole);
    }

    default List<AigcAssetFileDO> selectListByAssetIds(Collection<Long> assetIds) {
        return selectList(new LambdaQueryWrapperX<AigcAssetFileDO>()
                .inIfPresent(AigcAssetFileDO::getAssetId, assetIds));
    }

    default List<AigcAssetFileDO> selectListByAssetId(Long assetId) {
        return selectList(AigcAssetFileDO::getAssetId, assetId);
    }

}
