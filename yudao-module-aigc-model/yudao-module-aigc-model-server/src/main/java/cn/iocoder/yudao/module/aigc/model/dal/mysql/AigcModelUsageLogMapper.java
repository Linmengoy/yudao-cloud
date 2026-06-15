package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.model.controller.admin.usage.vo.AigcModelUsagePageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.usage.vo.AigcModelUsageTypeStatisticsRespVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelUsageLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AigcModelUsageLogMapper extends BaseMapperX<AigcModelUsageLogDO> {

    default PageResult<AigcModelUsageLogDO> selectPage(AigcModelUsagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcModelUsageLogDO>()
                .eqIfPresent(AigcModelUsageLogDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(AigcModelUsageLogDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AigcModelUsageLogDO::getModelId, reqVO.getModelId())
                .eqIfPresent(AigcModelUsageLogDO::getProviderId, reqVO.getProviderId())
                .eqIfPresent(AigcModelUsageLogDO::getCapability, reqVO.getCapability())
                .eqIfPresent(AigcModelUsageLogDO::getStatus, reqVO.getStatus())
                .orderByDesc(AigcModelUsageLogDO::getId));
    }

    @Select("""
            <script>
            SELECT
                COALESCE(model.type, 0) AS modelType,
                COUNT(1) AS usageCount,
                SUM(CASE WHEN usage_log.status = 0 THEN 1 ELSE 0 END) AS successCount,
                SUM(CASE WHEN usage_log.status = 1 THEN 1 ELSE 0 END) AS failedCount,
                COALESCE(SUM(usage_log.total_tokens), 0) AS totalTokens,
                COALESCE(SUM(usage_log.sale_price), 0) AS salePrice,
                COALESCE(SUM(usage_log.cost_price), 0) AS costPrice,
                COALESCE(ROUND(AVG(usage_log.duration_millis), 0), 0) AS avgDurationMillis
            FROM aigc_model_usage_log usage_log
                     LEFT JOIN aigc_model model ON model.id = usage_log.model_id AND model.deleted = 0
            WHERE usage_log.deleted = 0
              <if test="reqVO.taskId != null">
                  AND usage_log.task_id = #{reqVO.taskId}
              </if>
              <if test="reqVO.userId != null">
                  AND usage_log.user_id = #{reqVO.userId}
              </if>
              <if test="reqVO.modelId != null">
                  AND usage_log.model_id = #{reqVO.modelId}
              </if>
              <if test="reqVO.providerId != null">
                  AND usage_log.provider_id = #{reqVO.providerId}
              </if>
              <if test="reqVO.capability != null and reqVO.capability != ''">
                  AND usage_log.capability = #{reqVO.capability}
              </if>
              <if test="reqVO.status != null">
                  AND usage_log.status = #{reqVO.status}
              </if>
            GROUP BY COALESCE(model.type, 0)
            ORDER BY usageCount DESC, modelType ASC
            </script>
            """)
    List<AigcModelUsageTypeStatisticsRespVO> selectTypeStatistics(@Param("reqVO") AigcModelUsagePageReqVO reqVO);

}
