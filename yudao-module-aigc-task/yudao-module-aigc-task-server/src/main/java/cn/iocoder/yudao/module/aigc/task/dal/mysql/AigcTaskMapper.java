package cn.iocoder.yudao.module.aigc.task.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.task.controller.admin.task.vo.AigcTaskPageReqVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface AigcTaskMapper extends BaseMapperX<AigcTaskDO> {

    default AigcTaskDO selectByTaskNo(String taskNo) {
        return selectOne(AigcTaskDO::getTaskNo, taskNo);
    }

    default AigcTaskDO selectByClientRequestId(Long userId, String clientRequestId) {
        return selectOne(new LambdaQueryWrapperX<AigcTaskDO>()
                .eq(AigcTaskDO::getUserId, userId)
                .eq(AigcTaskDO::getClientRequestId, clientRequestId));
    }

    default PageResult<AigcTaskDO> selectPage(AigcTaskPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcTaskDO>()
                .eqIfPresent(AigcTaskDO::getUserId, reqVO.getUserId())
                .likeIfPresent(AigcTaskDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(AigcTaskDO::getTaskType, reqVO.getTaskType())
                .eqIfPresent(AigcTaskDO::getModelId, reqVO.getModelId())
                .eqIfPresent(AigcTaskDO::getStatus, reqVO.getStatus())
                .orderByDesc(AigcTaskDO::getId));
    }

    default PageResult<AigcTaskDO> selectPageByUserId(cn.iocoder.yudao.framework.common.pojo.PageParam reqVO, Long userId) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcTaskDO>()
                .select(AigcTaskDO::getId, AigcTaskDO::getTaskNo, AigcTaskDO::getTaskType,
                        AigcTaskDO::getStatus, AigcTaskDO::getProgress, AigcTaskDO::getEstimatedDurationMillis, AigcTaskDO::getSalePrice,
                        AigcTaskDO::getCurrencyType, AigcTaskDO::getOutputAssetId, AigcTaskDO::getOutputAssetType,
                        AigcTaskDO::getOutputSummary, AigcTaskDO::getFailReason, AigcTaskDO::getCreateTime, AigcTaskDO::getSubmitTime,
                        AigcTaskDO::getStartTime, AigcTaskDO::getFinishTime)
                .eq(AigcTaskDO::getUserId, userId)
                .orderByDesc(AigcTaskDO::getId));
    }

    default int updateByIdAndStatus(AigcTaskDO updateObj, String fromStatus) {
        return update(updateObj, new LambdaUpdateWrapper<AigcTaskDO>()
                .eq(AigcTaskDO::getId, updateObj.getId())
                .eq(AigcTaskDO::getStatus, fromStatus));
    }

    default int incrementRetryCount(Long id) {
        return update(null, new LambdaUpdateWrapper<AigcTaskDO>()
                .setSql("retry_count = retry_count + 1")
                .eq(AigcTaskDO::getId, id));
    }

    default List<AigcTaskDO> selectTimeoutTasks(Collection<String> statuses, LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<AigcTaskDO>()
                .in(AigcTaskDO::getStatus, statuses)
                .isNotNull(AigcTaskDO::getExpireTime)
                .lt(AigcTaskDO::getExpireTime, now)
                .orderByAsc(AigcTaskDO::getExpireTime));
    }

    @Select("""
            <script>
            SELECT
                COUNT(1) AS totalCount,
                COALESCE(SUM(CASE WHEN status = #{successStatus} THEN 1 ELSE 0 END), 0) AS successCount,
                COALESCE(SUM(CASE WHEN status = #{failedStatus} THEN 1 ELSE 0 END), 0) AS failedCount,
                COALESCE(SUM(CASE WHEN status IN
                    <foreach collection="finishedStatuses" item="status" open="(" separator="," close=")">
                        #{status}
                    </foreach>
                    THEN 1 ELSE 0 END), 0) AS finishedCount,
                COALESCE(SUM(CASE WHEN status = #{refundingStatus} THEN 1 ELSE 0 END), 0) AS refundingCount,
                COALESCE(SUM(CASE WHEN status IN
                    <foreach collection="backlogStatuses" item="status" open="(" separator="," close=")">
                        #{status}
                    </foreach>
                    THEN 1 ELSE 0 END), 0) AS backlogCount,
                COALESCE(SUM(CASE WHEN expire_time IS NOT NULL AND expire_time &lt; #{now} AND status NOT IN
                    <foreach collection="finishedStatuses" item="status" open="(" separator="," close=")">
                        #{status}
                    </foreach>
                    THEN 1 ELSE 0 END), 0) AS timeoutCount,
                COALESCE(SUM(CASE WHEN retry_count &gt; 0 THEN 1 ELSE 0 END), 0) AS retryTaskCount,
                COALESCE(AVG(CASE WHEN submit_time IS NOT NULL AND finish_time IS NOT NULL THEN TIMESTAMPDIFF(MICROSECOND, submit_time, finish_time) / 1000 ELSE NULL END), 0) AS avgDurationMillis
            FROM aigc_task
            WHERE deleted = 0
            </script>
            """)
    AigcTaskStatisticsAggregate selectStatistics(@Param("successStatus") String successStatus,
                                                 @Param("failedStatus") String failedStatus,
                                                 @Param("refundingStatus") String refundingStatus,
                                                 @Param("finishedStatuses") Collection<String> finishedStatuses,
                                                 @Param("backlogStatuses") Collection<String> backlogStatuses,
                                                 @Param("now") LocalDateTime now);

    @Select("""
            <script>
            SELECT
                COUNT(1) AS sampleCount,
                COALESCE(AVG(duration_millis), 0) AS avgDurationMillis
            FROM (
                SELECT TIMESTAMPDIFF(MICROSECOND, submit_time, finish_time) / 1000 AS duration_millis
                FROM aigc_task
                WHERE deleted = 0
                  AND status IN
                  <foreach collection="statuses" item="status" open="(" separator="," close=")">
                      #{status}
                  </foreach>
                  AND submit_time IS NOT NULL
                  AND finish_time IS NOT NULL
                  <if test="providerId != null">
                    AND provider_id = #{providerId}
                  </if>
                  <if test="modelId != null">
                    AND model_id = #{modelId}
                  </if>
                  <if test="capability != null and capability != ''">
                    AND capability = #{capability}
                  </if>
                ORDER BY finish_time DESC
                LIMIT #{sampleSize}
            ) latest_success_tasks
            </script>
            """)
    AigcTaskDurationStatisticsAggregate selectDurationStatistics(@Param("statuses") Collection<String> statuses,
                                                                 @Param("providerId") Long providerId,
                                                                 @Param("modelId") Long modelId,
                                                                 @Param("capability") String capability,
                                                                 @Param("sampleSize") Integer sampleSize);

    @Select("""
            <script>
            SELECT duration_millis
            FROM (
                SELECT TIMESTAMPDIFF(MICROSECOND, submit_time, finish_time) / 1000 AS duration_millis
                FROM aigc_task
                WHERE deleted = 0
                  AND status IN
                  <foreach collection="statuses" item="status" open="(" separator="," close=")">
                      #{status}
                  </foreach>
                  AND submit_time IS NOT NULL
                  AND finish_time IS NOT NULL
                  <if test="providerId != null">
                    AND provider_id = #{providerId}
                  </if>
                  <if test="modelId != null">
                    AND model_id = #{modelId}
                  </if>
                  <if test="capability != null and capability != ''">
                    AND capability = #{capability}
                  </if>
                ORDER BY finish_time DESC
                LIMIT #{sampleSize}
            ) latest_success_tasks
            ORDER BY duration_millis ASC
            </script>
            """)
    List<Long> selectRecentDurations(@Param("statuses") Collection<String> statuses,
                                     @Param("providerId") Long providerId,
                                     @Param("modelId") Long modelId,
                                     @Param("capability") String capability,
                                     @Param("sampleSize") Integer sampleSize);

}
