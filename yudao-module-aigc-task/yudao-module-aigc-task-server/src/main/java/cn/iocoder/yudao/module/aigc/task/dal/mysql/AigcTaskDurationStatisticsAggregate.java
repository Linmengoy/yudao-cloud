package cn.iocoder.yudao.module.aigc.task.dal.mysql;

import lombok.Data;

@Data
public class AigcTaskDurationStatisticsAggregate {

    private Long sampleCount;
    private Long avgDurationMillis;
    private Long p95DurationMillis;

}
