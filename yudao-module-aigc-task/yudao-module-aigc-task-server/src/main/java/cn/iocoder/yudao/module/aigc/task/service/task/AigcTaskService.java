package cn.iocoder.yudao.module.aigc.task.service.task;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.task.controller.admin.task.vo.AigcTaskPageReqVO;
import cn.iocoder.yudao.module.aigc.task.controller.admin.task.vo.AigcTaskStatisticsRespVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskDO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskDurationStatisticsReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskDurationStatisticsRespDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;

public interface AigcTaskService {

    Long createTask(AigcTaskCreateReqDTO reqDTO);

    AigcTaskDO getTask(Long id);

    AigcTaskDO getTaskByTaskNo(String taskNo);

    AigcTaskDO getTaskWithResult(Long id);

    AigcTaskDO getTaskByTaskNoWithResult(String taskNo);

    AigcTaskDO validateTaskExists(Long id);

    PageResult<AigcTaskDO> getTaskPage(AigcTaskPageReqVO reqVO);

    PageResult<AigcTaskDO> getUserTaskPage(PageParam reqVO, Long userId);

    AigcTaskStatisticsRespVO getTaskStatistics();

    AigcTaskDurationStatisticsRespDTO getSuccessDurationStatistics(AigcTaskDurationStatisticsReqDTO reqDTO);

    AigcTaskDO getUserTask(Long id, Long userId);

    AigcTaskDO getUserTaskWithResult(Long id, Long userId);

    void updateTaskStatus(Long taskId, String toStatus);

    void updateTaskStatus(AigcTaskStatusUpdateReqDTO reqDTO, String toStatus);

    void cancelTask(Long taskId);

    void cancelUserTask(Long taskId, Long userId);

    void increaseRetryCount(Long taskId);

}
