package cn.iocoder.yudao.module.aigc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - AIGC 模型代理 Response DTO")
@Data
public class AigcModelProxyRespDTO {

    @Schema(description = "代理编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "代理名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "google_cloud")
    private String name;

    @Schema(description = "代理协议", requiredMode = Schema.RequiredMode.REQUIRED, example = "SOCKS5")
    private String protocol;

    @Schema(description = "代理主机", requiredMode = Schema.RequiredMode.REQUIRED, example = "127.0.0.1")
    private String host;

    @Schema(description = "代理端口", requiredMode = Schema.RequiredMode.REQUIRED, example = "1080")
    private Integer port;

    @Schema(description = "代理用户名")
    private String username;

    @Schema(description = "代理密码")
    private String password;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "主代理")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
