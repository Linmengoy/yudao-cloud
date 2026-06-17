package cn.iocoder.yudao.module.infra.api.config.dto;

import lombok.Data;

@Data
public class ConfigSaveValueReqDTO {

    private String key;

    private String category;

    private String name;

    private String value;

    private Boolean visible;

    private String remark;

}
