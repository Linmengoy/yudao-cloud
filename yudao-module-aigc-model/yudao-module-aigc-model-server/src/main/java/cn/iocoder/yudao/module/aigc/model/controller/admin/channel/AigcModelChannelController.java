package cn.iocoder.yudao.module.aigc.model.controller.admin.channel;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.model.controller.admin.channel.vo.AigcModelChannelPageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.channel.vo.AigcModelChannelSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelChannelDO;
import cn.iocoder.yudao.module.aigc.model.service.channel.AigcModelChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 模型渠道实现")
@RestController
@RequestMapping("/aigc/model/channel")
@Validated
public class AigcModelChannelController {

    @Resource
    private AigcModelChannelService channelService;

    @PostMapping("/create")
    @Operation(summary = "创建模型渠道实现")
    @PreAuthorize("@ss.hasPermission('aigc:model:channel:create')")
    public CommonResult<Long> createChannel(@Valid @RequestBody AigcModelChannelSaveReqVO reqVO) {
        return success(channelService.createChannel(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新模型渠道实现")
    @PreAuthorize("@ss.hasPermission('aigc:model:channel:update')")
    public CommonResult<Boolean> updateChannel(@Valid @RequestBody AigcModelChannelSaveReqVO reqVO) {
        channelService.updateChannel(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除模型渠道实现")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:channel:delete')")
    public CommonResult<Boolean> deleteChannel(@RequestParam("id") Long id) {
        channelService.deleteChannel(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取模型渠道实现")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:channel:query')")
    public CommonResult<AigcModelChannelDO> getChannel(@RequestParam("id") Long id) {
        return success(channelService.getChannel(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取模型渠道实现分页")
    @PreAuthorize("@ss.hasPermission('aigc:model:channel:query')")
    public CommonResult<PageResult<AigcModelChannelDO>> getChannelPage(@Valid AigcModelChannelPageReqVO reqVO) {
        return success(channelService.getChannelPage(reqVO));
    }

    @PutMapping("/status")
    @Operation(summary = "更新模型渠道实现状态")
    @Parameter(name = "id", description = "ID", required = true)
    @Parameter(name = "status", description = "状态", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:channel:update')")
    public CommonResult<Boolean> updateChannelStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        channelService.updateChannelStatus(id, status);
        return success(true);
    }

}
