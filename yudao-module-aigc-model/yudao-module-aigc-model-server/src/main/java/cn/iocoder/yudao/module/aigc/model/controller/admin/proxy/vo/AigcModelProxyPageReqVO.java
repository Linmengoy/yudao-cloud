package cn.iocoder.yudao.module.aigc.model.controller.admin.proxy.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AIGC 模型代理分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcModelProxyPageReqVO extends PageParam {

    @Schema(description = "代理名称", example = "google_cloud")
    private String name;

    @Schema(description = "代理协议", example = "SOCKS5")
    private String protocol;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
