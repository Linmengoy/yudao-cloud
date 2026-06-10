package cn.iocoder.yudao.module.aigc.asset.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetPageReqVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCategoryCountRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetPageReqDTO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AigcAssetMapper extends BaseMapperX<AigcAssetDO> {

    List<String> MEDIA_ASSET_TYPES = List.of("IMAGE", "VIDEO");

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
        return selectPage(reqVO, buildPageWrapper(reqVO).orderByDesc(AigcAssetDO::getId));
    }

    default List<AigcAssetDO> selectList(AigcAssetPageReqVO reqVO) {
        return selectList(buildPageWrapper(reqVO).orderByDesc(AigcAssetDO::getId));
    }

    default Long selectCount(AigcAssetPageReqVO reqVO) {
        return selectCount(buildPageWrapper(reqVO));
    }

    @Select("""
            <script>
            SELECT
                COUNT(1) AS allCount,
                COALESCE(SUM(CASE WHEN asset_type = 'IMAGE' AND source_type = 'GENERATE' THEN 1 ELSE 0 END), 0) AS generatedImageCount,
                COALESCE(SUM(CASE WHEN asset_type = 'IMAGE' AND source_type = 'UPLOAD' THEN 1 ELSE 0 END), 0) AS uploadedImageCount,
                COALESCE(SUM(CASE WHEN asset_type = 'VIDEO' THEN 1 ELSE 0 END), 0) AS videoCount,
                COALESCE(SUM(CASE WHEN asset_type NOT IN ('IMAGE', 'VIDEO') THEN 1 ELSE 0 END), 0) AS otherCount
            FROM aigc_asset
            WHERE deleted = 0
              AND user_id = #{userId}
              AND status = #{status}
            <if test="auditStatus != null and auditStatus != ''">
              AND audit_status = #{auditStatus}
            </if>
            <if test="visibility != null and visibility != ''">
              AND visibility = #{visibility}
            </if>
            <if test="title != null and title != ''">
              AND title LIKE CONCAT('%', #{title}, '%')
            </if>
            </script>
            """)
    AigcAssetCategoryCountRespDTO selectCategoryCounts(AigcAssetPageReqVO reqVO);

    default LambdaQueryWrapperX<AigcAssetDO> buildPageWrapper(AigcAssetPageReqVO reqVO) {
        LambdaQueryWrapperX<AigcAssetDO> wrapper = new LambdaQueryWrapperX<AigcAssetDO>()
                .eqIfPresent(AigcAssetDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AigcAssetDO::getAssetType, reqVO.getAssetType())
                .eqIfPresent(AigcAssetDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(AigcAssetDO::getAuditStatus, reqVO.getAuditStatus())
                .eqIfPresent(AigcAssetDO::getVisibility, reqVO.getVisibility())
                .eqIfPresent(AigcAssetDO::getStatus, reqVO.getStatus())
                .likeIfPresent(AigcAssetDO::getTitle, reqVO.getTitle());
        if ("OTHER".equals(reqVO.getCategory())) {
            wrapper.notIn(AigcAssetDO::getAssetType, MEDIA_ASSET_TYPES);
        }
        return wrapper;
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
