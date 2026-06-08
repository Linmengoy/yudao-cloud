package cn.iocoder.yudao.module.aigc.model.controller.admin.proxy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 模型代理新增/修改 Request VO")
@Data
public class AigcModelProxySaveReqVO {

    @Schema(description = "代理编号", example = "1024")
    private Long id;

    @Schema(description = "代理名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "google_cloud")
    @NotBlank(message = "代理名称不能为空")
    private String name;

    @Schema(description = "代理协议", requiredMode = Schema.RequiredMode.REQUIRED, example = "SOCKS5")
    @NotBlank(message = "代理协议不能为空")
    private String protocol;

    @Schema(description = "代理主机", requiredMode = Schema.RequiredMode.REQUIRED, example = "127.0.0.1")
    @NotBlank(message = "代理主机不能为空")
    private String host;

    @Schema(description = "代理端口", requiredMode = Schema.RequiredMode.REQUIRED, example = "1080")
    @NotNull(message = "代理端口不能为空")
    private Integer port;

    @Schema(description = "代理用户名")
    private String username;

    @Schema(description = "代理密码")
    private String password;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "主代理")
    private String remark;

}
