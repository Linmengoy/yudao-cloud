package cn.iocoder.yudao.module.infra.api.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - 文件预签名 Request DTO")
@Data
@Accessors(chain = true)
public class FilePresignReqDTO {

    @Schema(description = "配置编号")
    private Long configId;

    @Schema(description = "文件路径")
    @NotEmpty(message = "文件路径不能为空")
    private String path;

    @Schema(description = "访问有效期，单位秒")
    private Integer expirationSeconds;

}
