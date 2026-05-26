package cn.iocoder.yudao.module.aigc.task.service.compensate;

public interface AigcTaskCompensateService {

    int compensateTimeoutTasks();

    void compensateFailedRefunding(Long taskId);

}
