package cn.iocoder.yudao.module.aigc.task.dal.mysql;

import lombok.Data;

@Data
public class AigcTaskStatisticsAggregate {

    private Long totalCount;
    private Long successCount;
    private Long failedCount;
    private Long finishedCount;
    private Long refundingCount;
    private Long backlogCount;
    private Long timeoutCount;
    private Long avgDurationMillis;

}
