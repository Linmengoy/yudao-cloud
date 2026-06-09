package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户端 - AIGC 画布成员 Response VO")
@Data
public class AigcCanvasMemberRespVO {

    @Schema(description = "成员编号")
    private Long id;
    @Schema(description = "项目编号")
    private Long projectId;
    @Schema(description = "用户编号")
    private Long userId;
    @Schema(description = "用户昵称")
    private String nickname;
    @Schema(description = "用户头像")
    private String avatar;
    @Schema(description = "用户手机号")
    private String mobile;
    @Schema(description = "用户邮箱")
    private String email;
    @Schema(description = "用户状态")
    private Integer userStatus;
    @Schema(description = "成员角色")
    private String role;
    @Schema(description = "邀请人用户编号")
    private Long inviteUserId;
    @Schema(description = "加入时间")
    private LocalDateTime joinedTime;
    @Schema(description = "最后活跃时间")
    private LocalDateTime lastActiveTime;

}
