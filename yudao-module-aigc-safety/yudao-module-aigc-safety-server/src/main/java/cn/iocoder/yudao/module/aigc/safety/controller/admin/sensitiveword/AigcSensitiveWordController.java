package cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo.AigcSensitiveWordPageReqVO;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo.AigcSensitiveWordRespVO;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo.AigcSensitiveWordSaveReqVO;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo.AigcSensitiveWordStatusReqVO;
import cn.iocoder.yudao.module.aigc.safety.dal.dataobject.AigcSensitiveWordDO;
import cn.iocoder.yudao.module.aigc.safety.service.sensitiveword.AigcSensitiveWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 敏感词")
@RestController
@RequestMapping("/aigc/safety/sensitive-word")
@Validated
public class AigcSensitiveWordController {

    @Resource
    private AigcSensitiveWordService sensitiveWordService;

    @PostMapping("/create")
    @Operation(summary = "创建敏感词")
    @PreAuthorize("@ss.hasPermission('aigc:safety-sensitive-word:create')")
    public CommonResult<Long> createSensitiveWord(@Valid @RequestBody AigcSensitiveWordSaveReqVO reqVO) {
        return success(sensitiveWordService.createSensitiveWord(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新敏感词")
    @PreAuthorize("@ss.hasPermission('aigc:safety-sensitive-word:update')")
    public CommonResult<Boolean> updateSensitiveWord(@Valid @RequestBody AigcSensitiveWordSaveReqVO reqVO) {
        sensitiveWordService.updateSensitiveWord(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除敏感词")
    @Parameter(name = "id", description = "敏感词编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:safety-sensitive-word:delete')")
    public CommonResult<Boolean> deleteSensitiveWord(@RequestParam("id") Long id) {
        sensitiveWordService.deleteSensitiveWord(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取敏感词")
    @Parameter(name = "id", description = "敏感词编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:safety-sensitive-word:query')")
    public CommonResult<AigcSensitiveWordRespVO> getSensitiveWord(@RequestParam("id") Long id) {
        AigcSensitiveWordDO sensitiveWord = sensitiveWordService.validateSensitiveWordExists(id);
        return success(BeanUtils.toBean(sensitiveWord, AigcSensitiveWordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取敏感词分页")
    @PreAuthorize("@ss.hasPermission('aigc:safety-sensitive-word:query')")
    public CommonResult<PageResult<AigcSensitiveWordRespVO>> getSensitiveWordPage(@Valid AigcSensitiveWordPageReqVO reqVO) {
        PageResult<AigcSensitiveWordDO> pageResult = sensitiveWordService.getSensitiveWordPage(reqVO);
        return success(BeanUtils.toBean(pageResult, AigcSensitiveWordRespVO.class));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新敏感词状态")
    @PreAuthorize("@ss.hasPermission('aigc:safety-sensitive-word:update')")
    public CommonResult<Boolean> updateSensitiveWordStatus(@Valid @RequestBody AigcSensitiveWordStatusReqVO reqVO) {
        sensitiveWordService.updateSensitiveWordStatus(reqVO);
        return success(true);
    }

}
