package cn.iocoder.yudao.module.infra.api.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - 文件预签名 Response DTO")
@Data
@Accessors(chain = true)
public class FilePresignRespDTO {

    @Schema(description = "访问 URL")
    private String url;

    @Schema(description = "有效期，单位秒")
    private Integer expireSeconds;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "是否公开访问")
    private Boolean publicAccess;

}
