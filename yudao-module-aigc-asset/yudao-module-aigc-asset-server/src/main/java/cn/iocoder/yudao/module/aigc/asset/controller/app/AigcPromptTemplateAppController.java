package cn.iocoder.yudao.module.aigc.asset.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplateModelRespVO;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplatePageReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplateRespVO;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplateShareReqVO;
import cn.iocoder.yudao.module.aigc.asset.service.template.AigcPromptTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "用户端 - AIGC 提示词模板")
@RestController
@RequestMapping("/aigc/asset/prompt-template")
@Validated
public class AigcPromptTemplateAppController {

    private static final int MAX_TEMPLATE_PAGE_SIZE = 60;

    @Resource
    private AigcPromptTemplateService promptTemplateService;

    @GetMapping("/page")
    @Operation(summary = "获取提示词模板分页")
    public CommonResult<PageResult<AigcPromptTemplateRespVO>> getTemplatePage(
            @Valid AigcPromptTemplatePageReqVO reqVO) {
        limitPageSize(reqVO);
        return success(promptTemplateService.getTemplatePage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获取提示词模板详情")
    @Parameter(name = "id", description = "模板编号", required = true)
    public CommonResult<AigcPromptTemplateRespVO> getTemplate(@RequestParam("id") Long id) {
        promptTemplateService.increaseViewCount(id);
        return success(promptTemplateService.getTemplate(id));
    }

    @GetMapping("/categories")
    @Operation(summary = "获取提示词模板分类")
    public CommonResult<List<String>> getTemplateCategories() {
        return success(promptTemplateService.getCategoryList());
    }

    @GetMapping("/models")
    @Operation(summary = "获取提示词模板模型")
    public CommonResult<List<AigcPromptTemplateModelRespVO>> getTemplateModels() {
        return success(promptTemplateService.getModelList());
    }

    @PostMapping("/share")
    @Operation(summary = "分享提示词模板")
    public CommonResult<Long> shareTemplate(@Valid @RequestBody AigcPromptTemplateShareReqVO reqVO) {
        return success(promptTemplateService.shareTemplate(getLoginUserId(), reqVO));
    }

    @PostMapping("/copy")
    @Operation(summary = "记录提示词模板复制")
    @Parameter(name = "id", description = "模板编号", required = true)
    public CommonResult<Boolean> copyTemplate(@RequestParam("id") Long id) {
        promptTemplateService.increaseCopyCount(id);
        return success(true);
    }

    @PostMapping("/use")
    @Operation(summary = "记录提示词模板复用")
    @Parameter(name = "id", description = "模板编号", required = true)
    public CommonResult<Boolean> useTemplate(@RequestParam("id") Long id) {
        promptTemplateService.increaseUseCount(id);
        return success(true);
    }

    private void limitPageSize(AigcPromptTemplatePageReqVO reqVO) {
        if (reqVO.getPageSize() == null || reqVO.getPageSize() > MAX_TEMPLATE_PAGE_SIZE) {
            reqVO.setPageSize(MAX_TEMPLATE_PAGE_SIZE);
        }
    }

}
