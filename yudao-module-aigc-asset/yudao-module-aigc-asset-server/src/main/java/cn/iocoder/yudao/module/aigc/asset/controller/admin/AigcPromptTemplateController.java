package cn.iocoder.yudao.module.aigc.asset.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcPromptTemplateImportReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcPromptTemplateImportRespVO;
import cn.iocoder.yudao.module.aigc.asset.service.template.AigcPromptTemplateImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 提示词模板")
@RestController
@RequestMapping("/aigc/asset/prompt-template")
@Validated
public class AigcPromptTemplateController {

    @Resource
    private AigcPromptTemplateImportService promptTemplateImportService;

    @PostMapping("/import-awesome-gpt-image")
    @Operation(summary = "导入 awesome-gpt-image-2 模板")
    @PreAuthorize("@ss.hasPermission('aigc:asset:create')")
    public CommonResult<AigcPromptTemplateImportRespVO> importAwesomeGptImageCases(
            @Valid @RequestBody AigcPromptTemplateImportReqVO reqVO) {
        return success(promptTemplateImportService.importAwesomeGptImageCases(reqVO));
    }

    @PostMapping("/import-awesome-gpt-image-files")
    @Operation(summary = "上传并导入 awesome-gpt-image-2 模板")
    @PreAuthorize("@ss.hasPermission('aigc:asset:create')")
    public CommonResult<AigcPromptTemplateImportRespVO> importAwesomeGptImageCaseFiles(
            @RequestParam("casesJson") MultipartFile casesJson,
            @RequestParam("images") MultipartFile[] images,
            @RequestParam(value = "storageDirectory", defaultValue = "aigc/templates") @NotBlank String storageDirectory) {
        return success(promptTemplateImportService.importAwesomeGptImageCaseFiles(casesJson, images, storageDirectory));
    }

}
