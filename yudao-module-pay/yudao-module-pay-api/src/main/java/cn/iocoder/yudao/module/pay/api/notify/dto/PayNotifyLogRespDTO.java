package cn.iocoder.yudao.module.pay.api.notify.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayNotifyLogRespDTO {

    private Long id;
    private Long taskId;
    private Integer notifyTimes;
    private String response;
    private Integer status;
    private LocalDateTime createTime;

}
