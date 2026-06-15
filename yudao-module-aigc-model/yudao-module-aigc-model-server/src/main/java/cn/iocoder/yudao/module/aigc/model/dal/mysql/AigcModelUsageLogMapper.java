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
        LambdaQueryWrapperX<AigcModelUsageLogDO> queryWrapper = new LambdaQueryWrapperX<AigcModelUsageLogDO>()
                .eqIfPresent(AigcModelUsageLogDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(AigcModelUsageLogDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AigcModelUsageLogDO::getModelId, reqVO.getModelId())
                .eqIfPresent(AigcModelUsageLogDO::getProviderId, reqVO.getProviderId())
                .eqIfPresent(AigcModelUsageLogDO::getCapability, reqVO.getCapability())
                .eqIfPresent(AigcModelUsageLogDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AigcModelUsageLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AigcModelUsageLogDO::getId);
        if (reqVO.getModelType() != null) {
            queryWrapper.inSql(AigcModelUsageLogDO::getModelId,
                    "SELECT id FROM aigc_model WHERE deleted = 0 AND type = " + reqVO.getModelType());
        }
        return selectPage(reqVO, queryWrapper);
    }

    @Select("""
            <script>
            SELECT * FROM (
                SELECT 'MODEL_TYPE' AS dimensionType,
                       NULL AS modelId,
                       NULL AS modelName,
                       COALESCE(model.type, 0) AS modelType,
                       NULL AS capability,
                       COUNT(1) AS usageCount,
                       SUM(CASE WHEN usage_log.status = 0 THEN 1 ELSE 0 END) AS successCount,
                       SUM(CASE WHEN usage_log.status = 1 THEN 1 ELSE 0 END) AS failedCount,
                       ROUND(SUM(CASE WHEN usage_log.status = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(1), 2) AS failureRate,
                       COALESCE(SUM(usage_log.total_tokens), 0) AS totalTokens,
                       COALESCE(SUM(usage_log.sale_price), 0) AS salePrice,
                       COALESCE(SUM(usage_log.cost_price), 0) AS costPrice,
                       COALESCE(ROUND(AVG(usage_log.duration_millis), 0), 0) AS avgDurationMillis,
                       1 AS dimensionOrder
                FROM aigc_model_usage_log usage_log
                         LEFT JOIN aigc_model model ON model.id = usage_log.model_id AND model.deleted = 0
                WHERE usage_log.deleted = 0
                  <if test="reqVO.taskId != null">AND usage_log.task_id = #{reqVO.taskId}</if>
                  <if test="reqVO.userId != null">AND usage_log.user_id = #{reqVO.userId}</if>
                  <if test="reqVO.modelId != null">AND usage_log.model_id = #{reqVO.modelId}</if>
                  <if test="reqVO.modelType != null">AND model.type = #{reqVO.modelType}</if>
                  <if test="reqVO.providerId != null">AND usage_log.provider_id = #{reqVO.providerId}</if>
                  <if test="reqVO.capability != null and reqVO.capability != ''">AND usage_log.capability = #{reqVO.capability}</if>
                  <if test="reqVO.status != null">AND usage_log.status = #{reqVO.status}</if>
                  <if test="reqVO.createTime != null and reqVO.createTime.length == 2">
                      AND usage_log.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}
                  </if>
                GROUP BY COALESCE(model.type, 0)
                UNION ALL
                SELECT 'CAPABILITY' AS dimensionType,
                       NULL AS modelId,
                       NULL AS modelName,
                       NULL AS modelType,
                       COALESCE(usage_log.capability, 'UNKNOWN') AS capability,
                       COUNT(1) AS usageCount,
                       SUM(CASE WHEN usage_log.status = 0 THEN 1 ELSE 0 END) AS successCount,
                       SUM(CASE WHEN usage_log.status = 1 THEN 1 ELSE 0 END) AS failedCount,
                       ROUND(SUM(CASE WHEN usage_log.status = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(1), 2) AS failureRate,
                       COALESCE(SUM(usage_log.total_tokens), 0) AS totalTokens,
                       COALESCE(SUM(usage_log.sale_price), 0) AS salePrice,
                       COALESCE(SUM(usage_log.cost_price), 0) AS costPrice,
                       COALESCE(ROUND(AVG(usage_log.duration_millis), 0), 0) AS avgDurationMillis,
                       2 AS dimensionOrder
                FROM aigc_model_usage_log usage_log
                         LEFT JOIN aigc_model model ON model.id = usage_log.model_id AND model.deleted = 0
                WHERE usage_log.deleted = 0
                  <if test="reqVO.taskId != null">AND usage_log.task_id = #{reqVO.taskId}</if>
                  <if test="reqVO.userId != null">AND usage_log.user_id = #{reqVO.userId}</if>
                  <if test="reqVO.modelId != null">AND usage_log.model_id = #{reqVO.modelId}</if>
                  <if test="reqVO.modelType != null">AND model.type = #{reqVO.modelType}</if>
                  <if test="reqVO.providerId != null">AND usage_log.provider_id = #{reqVO.providerId}</if>
                  <if test="reqVO.capability != null and reqVO.capability != ''">AND usage_log.capability = #{reqVO.capability}</if>
                  <if test="reqVO.status != null">AND usage_log.status = #{reqVO.status}</if>
                  <if test="reqVO.createTime != null and reqVO.createTime.length == 2">
                      AND usage_log.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}
                  </if>
                GROUP BY COALESCE(usage_log.capability, 'UNKNOWN')
                UNION ALL
                SELECT 'MODEL_TOP' AS dimensionType,
                       usage_log.model_id AS modelId,
                       COALESCE(model.name, CONCAT('Unknown Model/', usage_log.model_id)) AS modelName,
                       COALESCE(model.type, 0) AS modelType,
                       NULL AS capability,
                       COUNT(1) AS usageCount,
                       SUM(CASE WHEN usage_log.status = 0 THEN 1 ELSE 0 END) AS successCount,
                       SUM(CASE WHEN usage_log.status = 1 THEN 1 ELSE 0 END) AS failedCount,
                       ROUND(SUM(CASE WHEN usage_log.status = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(1), 2) AS failureRate,
                       COALESCE(SUM(usage_log.total_tokens), 0) AS totalTokens,
                       COALESCE(SUM(usage_log.sale_price), 0) AS salePrice,
                       COALESCE(SUM(usage_log.cost_price), 0) AS costPrice,
                       COALESCE(ROUND(AVG(usage_log.duration_millis), 0), 0) AS avgDurationMillis,
                       3 AS dimensionOrder
                FROM aigc_model_usage_log usage_log
                         LEFT JOIN aigc_model model ON model.id = usage_log.model_id AND model.deleted = 0
                WHERE usage_log.deleted = 0
                  <if test="reqVO.taskId != null">AND usage_log.task_id = #{reqVO.taskId}</if>
                  <if test="reqVO.userId != null">AND usage_log.user_id = #{reqVO.userId}</if>
                  <if test="reqVO.modelId != null">AND usage_log.model_id = #{reqVO.modelId}</if>
                  <if test="reqVO.modelType != null">AND model.type = #{reqVO.modelType}</if>
                  <if test="reqVO.providerId != null">AND usage_log.provider_id = #{reqVO.providerId}</if>
                  <if test="reqVO.capability != null and reqVO.capability != ''">AND usage_log.capability = #{reqVO.capability}</if>
                  <if test="reqVO.status != null">AND usage_log.status = #{reqVO.status}</if>
                  <if test="reqVO.createTime != null and reqVO.createTime.length == 2">
                      AND usage_log.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}
                  </if>
                GROUP BY usage_log.model_id, model.name, COALESCE(model.type, 0)
                UNION ALL
                SELECT 'FAILURE_RATE' AS dimensionType,
                       usage_log.model_id AS modelId,
                       COALESCE(model.name, CONCAT('Unknown Model/', usage_log.model_id)) AS modelName,
                       COALESCE(model.type, 0) AS modelType,
                       NULL AS capability,
                       COUNT(1) AS usageCount,
                       SUM(CASE WHEN usage_log.status = 0 THEN 1 ELSE 0 END) AS successCount,
                       SUM(CASE WHEN usage_log.status = 1 THEN 1 ELSE 0 END) AS failedCount,
                       ROUND(SUM(CASE WHEN usage_log.status = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(1), 2) AS failureRate,
                       COALESCE(SUM(usage_log.total_tokens), 0) AS totalTokens,
                       COALESCE(SUM(usage_log.sale_price), 0) AS salePrice,
                       COALESCE(SUM(usage_log.cost_price), 0) AS costPrice,
                       COALESCE(ROUND(AVG(usage_log.duration_millis), 0), 0) AS avgDurationMillis,
                       4 AS dimensionOrder
                FROM aigc_model_usage_log usage_log
                         LEFT JOIN aigc_model model ON model.id = usage_log.model_id AND model.deleted = 0
                WHERE usage_log.deleted = 0
                  <if test="reqVO.taskId != null">AND usage_log.task_id = #{reqVO.taskId}</if>
                  <if test="reqVO.userId != null">AND usage_log.user_id = #{reqVO.userId}</if>
                  <if test="reqVO.modelId != null">AND usage_log.model_id = #{reqVO.modelId}</if>
                  <if test="reqVO.modelType != null">AND model.type = #{reqVO.modelType}</if>
                  <if test="reqVO.providerId != null">AND usage_log.provider_id = #{reqVO.providerId}</if>
                  <if test="reqVO.capability != null and reqVO.capability != ''">AND usage_log.capability = #{reqVO.capability}</if>
                  <if test="reqVO.status != null">AND usage_log.status = #{reqVO.status}</if>
                  <if test="reqVO.createTime != null and reqVO.createTime.length == 2">
                      AND usage_log.create_time BETWEEN #{reqVO.createTime[0]} AND #{reqVO.createTime[1]}
                  </if>
                GROUP BY usage_log.model_id, model.name, COALESCE(model.type, 0)
            ) statistics
            ORDER BY dimensionOrder ASC,
                     CASE WHEN dimensionType = 'FAILURE_RATE' THEN failureRate ELSE usageCount END DESC,
                     usageCount DESC,
                     modelType ASC
            </script>
            """)
    List<AigcModelUsageTypeStatisticsRespVO> selectTypeStatistics(@Param("reqVO") AigcModelUsagePageReqVO reqVO);

}
