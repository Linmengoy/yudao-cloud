package cn.iocoder.yudao.module.aigc.task.service.log;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.task.controller.admin.log.vo.AigcTaskLogPageReqVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskLogDO;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
public class AigcTaskLogServiceImpl implements AigcTaskLogService {

    @Resource
    private AigcTaskLogMapper taskLogMapper;

    @Override
    public Long createTaskLog(Long taskId, String taskNo, String fromStatus, String toStatus, String action, String message) {
        AigcTaskLogDO log = new AigcTaskLogDO()
                .setTaskId(taskId)
                .setTaskNo(taskNo)
                .setFromStatus(fromStatus)
                .setToStatus(toStatus)
                .setAction(action)
                .setMessage(message)
                .setOperatorType("SYSTEM");
        taskLogMapper.insert(log);
        return log.getId();
    }

    @Override
    public PageResult<AigcTaskLogDO> getTaskLogPage(AigcTaskLogPageReqVO reqVO) {
        return taskLogMapper.selectPage(reqVO);
    }

    @Override
    public List<AigcTaskLogDO> getTaskLogList(Long taskId) {
        return taskLogMapper.selectListByTaskId(taskId);
    }

}
