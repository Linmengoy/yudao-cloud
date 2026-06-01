package cn.iocoder.yudao.module.system.controller.app.notify;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.app.notify.vo.AppNotifyMessagePageReqVO;
import cn.iocoder.yudao.module.system.controller.app.notify.vo.AppNotifyMessageRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.service.notify.NotifyMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 我的站内信")
@RestController
@RequestMapping("/system/notify-message")
@Validated
public class AppNotifyMessageController {

    @Resource
    private NotifyMessageService notifyMessageService;

    @GetMapping("/my-page")
    @Operation(summary = "获得我的站内信分页")
    public CommonResult<PageResult<AppNotifyMessageRespVO>> getMyNotifyMessagePage(@Valid AppNotifyMessagePageReqVO pageVO) {
        PageResult<NotifyMessageDO> pageResult = notifyMessageService.getAppNotifyMessagePage(
                pageVO, getLoginUserId(), UserTypeEnum.MEMBER.getValue());
        return success(BeanUtils.toBean(pageResult, AppNotifyMessageRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得我的站内信")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<AppNotifyMessageRespVO> getMyNotifyMessage(@RequestParam("id") Long id) {
        NotifyMessageDO message = notifyMessageService.getNotifyMessage(id);
        if (message == null || !getLoginUserId().equals(message.getUserId())
                || !UserTypeEnum.MEMBER.getValue().equals(message.getUserType())) {
            return success(null);
        }
        return success(BeanUtils.toBean(message, AppNotifyMessageRespVO.class));
    }

    @PutMapping("/update-read")
    @Operation(summary = "标记站内信为已读")
    @Parameter(name = "ids", description = "编号列表", required = true, example = "1024,2048")
    public CommonResult<Boolean> updateNotifyMessageRead(@RequestParam("ids") List<Long> ids) {
        notifyMessageService.updateNotifyMessageRead(ids, getLoginUserId(), UserTypeEnum.MEMBER.getValue());
        return success(Boolean.TRUE);
    }

    @PutMapping("/update-all-read")
    @Operation(summary = "标记所有站内信为已读")
    public CommonResult<Boolean> updateAllNotifyMessageRead() {
        notifyMessageService.updateAllNotifyMessageRead(getLoginUserId(), UserTypeEnum.MEMBER.getValue());
        return success(Boolean.TRUE);
    }

    @GetMapping("/get-unread-list")
    @Operation(summary = "获取当前用户的最新站内信列表，默认 10 条")
    @Parameter(name = "size", description = "10")
    public CommonResult<List<AppNotifyMessageRespVO>> getUnreadNotifyMessageList(
            @RequestParam(name = "size", defaultValue = "10") Integer size) {
        List<NotifyMessageDO> list = notifyMessageService.getUnreadNotifyMessageList(
                getLoginUserId(), UserTypeEnum.MEMBER.getValue(), size);
        return success(BeanUtils.toBean(list, AppNotifyMessageRespVO.class));
    }

    @GetMapping("/get-unread-count")
    @Operation(summary = "获得当前用户的未读站内信数量")
    @ApiAccessLog(enable = false)
    public CommonResult<Long> getUnreadNotifyMessageCount() {
        return success(notifyMessageService.getUnreadNotifyMessageCount(
                getLoginUserId(), UserTypeEnum.MEMBER.getValue()));
    }

}
