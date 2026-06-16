package cn.iocoder.yudao.module.aigc.model.controller.admin.release;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.model.controller.admin.release.vo.AigcReleaseNotePageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.release.vo.AigcReleaseNoteSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcReleaseNoteDO;
import cn.iocoder.yudao.module.aigc.model.service.release.AigcReleaseNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 版本更新记录")
@RestController
@RequestMapping("/aigc/release-note")
@Validated
public class AigcReleaseNoteController {

    @Resource
    private AigcReleaseNoteService releaseNoteService;

    @PostMapping("/create")
    @Operation(summary = "创建版本更新记录")
    @PreAuthorize("@ss.hasPermission('aigc:release-note:create')")
    public CommonResult<Long> createReleaseNote(@Valid @RequestBody AigcReleaseNoteSaveReqVO reqVO) {
        return success(releaseNoteService.createReleaseNote(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新版本更新记录")
    @PreAuthorize("@ss.hasPermission('aigc:release-note:update')")
    public CommonResult<Boolean> updateReleaseNote(@Valid @RequestBody AigcReleaseNoteSaveReqVO reqVO) {
        releaseNoteService.updateReleaseNote(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除版本更新记录")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:release-note:delete')")
    public CommonResult<Boolean> deleteReleaseNote(@RequestParam("id") Long id) {
        releaseNoteService.deleteReleaseNote(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取版本更新记录")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:release-note:query')")
    public CommonResult<AigcReleaseNoteDO> getReleaseNote(@RequestParam("id") Long id) {
        return success(releaseNoteService.getReleaseNote(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取版本更新记录分页")
    @PreAuthorize("@ss.hasPermission('aigc:release-note:query')")
    public CommonResult<PageResult<AigcReleaseNoteDO>> getReleaseNotePage(@Valid AigcReleaseNotePageReqVO reqVO) {
        return success(releaseNoteService.getReleaseNotePage(reqVO));
    }

    @PutMapping("/status")
    @Operation(summary = "发布或下线版本更新记录")
    @PreAuthorize("@ss.hasPermission('aigc:release-note:publish')")
    public CommonResult<Boolean> updateReleaseNoteStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        releaseNoteService.updateReleaseNoteStatus(id, status);
        return success(true);
    }

}
