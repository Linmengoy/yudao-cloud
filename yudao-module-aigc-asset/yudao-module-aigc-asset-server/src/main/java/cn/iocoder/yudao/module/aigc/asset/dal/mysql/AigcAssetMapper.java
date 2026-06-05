package cn.iocoder.yudao.module.aigc.asset.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetPageReqVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetPageReqDTO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcAssetMapper extends BaseMapperX<AigcAssetDO> {

    default AigcAssetDO selectByAssetNo(String assetNo) {
        return selectOne(AigcAssetDO::getAssetNo, assetNo);
    }

    default AigcAssetDO selectByTaskId(Long taskId) {
        return selectOne(AigcAssetDO::getTaskId, taskId);
    }

    default AigcAssetDO selectByTaskIdAndType(Long taskId, String assetType) {
        return selectOne(AigcAssetDO::getTaskId, taskId, AigcAssetDO::getAssetType, assetType);
    }

    default Long selectNormalCount() {
        return selectCount(AigcAssetDO::getStatus, "NORMAL");
    }

    default PageResult<AigcAssetDO> selectPage(AigcAssetPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcAssetDO>()
                .eqIfPresent(AigcAssetDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AigcAssetDO::getAssetType, reqVO.getAssetType())
                .eqIfPresent(AigcAssetDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(AigcAssetDO::getAuditStatus, reqVO.getAuditStatus())
                .eqIfPresent(AigcAssetDO::getVisibility, reqVO.getVisibility())
                .eqIfPresent(AigcAssetDO::getStatus, reqVO.getStatus())
                .likeIfPresent(AigcAssetDO::getTitle, reqVO.getTitle())
                .orderByDesc(AigcAssetDO::getId));
    }

    default List<AigcAssetDO> selectList(AigcAssetPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<AigcAssetDO>()
                .eqIfPresent(AigcAssetDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AigcAssetDO::getAssetType, reqVO.getAssetType())
                .eqIfPresent(AigcAssetDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(AigcAssetDO::getAuditStatus, reqVO.getAuditStatus())
                .eqIfPresent(AigcAssetDO::getVisibility, reqVO.getVisibility())
                .eqIfPresent(AigcAssetDO::getStatus, reqVO.getStatus())
                .likeIfPresent(AigcAssetDO::getTitle, reqVO.getTitle())
                .orderByDesc(AigcAssetDO::getId));
    }

    default PageResult<AigcAssetDO> selectPage(AigcAssetPageReqDTO reqDTO) {
        return selectPage(reqDTO, new LambdaQueryWrapperX<AigcAssetDO>()
                .eqIfPresent(AigcAssetDO::getUserId, reqDTO.getUserId())
                .eqIfPresent(AigcAssetDO::getAssetType, reqDTO.getAssetType())
                .eqIfPresent(AigcAssetDO::getSourceType, reqDTO.getSourceType())
                .eqIfPresent(AigcAssetDO::getAuditStatus, reqDTO.getAuditStatus())
                .eqIfPresent(AigcAssetDO::getStatus, reqDTO.getStatus())
                .orderByDesc(AigcAssetDO::getId));
    }

    default int increaseDownloadCount(Long id) {
        return update(null, new LambdaUpdateWrapper<AigcAssetDO>()
                .eq(AigcAssetDO::getId, id)
                .setSql("download_count = download_count + 1"));
    }

    default int increaseUseCount(Long id) {
        return update(null, new LambdaUpdateWrapper<AigcAssetDO>()
                .eq(AigcAssetDO::getId, id)
                .setSql("use_count = use_count + 1, last_used_time = NOW()"));
    }

}
