package cn.iocoder.yudao.module.aigc.task.service.log;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.task.controller.admin.log.vo.AigcTaskLogPageReqVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskLogDO;

import java.util.List;

public interface AigcTaskLogService {

    Long createTaskLog(Long taskId, String taskNo, String fromStatus, String toStatus, String action, String message);

    PageResult<AigcTaskLogDO> getTaskLogPage(AigcTaskLogPageReqVO reqVO);

    List<AigcTaskLogDO> getTaskLogList(Long taskId);
}
