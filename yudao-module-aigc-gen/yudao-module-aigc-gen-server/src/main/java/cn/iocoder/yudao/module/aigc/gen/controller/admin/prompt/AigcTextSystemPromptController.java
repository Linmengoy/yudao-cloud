package cn.iocoder.yudao.module.aigc.gen.controller.admin.prompt;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.prompt.vo.AigcTextSystemPromptRespVO;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.prompt.vo.AigcTextSystemPromptSaveReqVO;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.infra.api.config.dto.ConfigSaveValueReqDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC Text 系统提示词")
@RestController
@RequestMapping("/aigc/gen/text-system-prompt")
@Validated
public class AigcTextSystemPromptController {

    private static final String CONFIG_KEY = "aigc.text.system-prompt";
    private static final String CONFIG_CATEGORY = "aigc";
    private static final String CONFIG_NAME = "Text 系统提示词";
    private static final String CONFIG_REMARK = "Text Composer 优化提示词时使用的全局系统提示词";

    @Resource
    private ConfigApi configApi;

    @GetMapping("/get")
    @Operation(summary = "获取 Text 系统提示词")
    @PreAuthorize("@ss.hasPermission('aigc:prompt:text-system:query')")
    public CommonResult<AigcTextSystemPromptRespVO> getTextSystemPrompt() {
        AigcTextSystemPromptRespVO respVO = new AigcTextSystemPromptRespVO();
        respVO.setKey(CONFIG_KEY);
        respVO.setValue(configApi.getConfigValueByKey(CONFIG_KEY).getCheckedData());
        return success(respVO);
    }

    @PutMapping("/save")
    @Operation(summary = "保存 Text 系统提示词")
    @PreAuthorize("@ss.hasPermission('aigc:prompt:text-system:update')")
    public CommonResult<Boolean> saveTextSystemPrompt(@Valid @RequestBody AigcTextSystemPromptSaveReqVO reqVO) {
        ConfigSaveValueReqDTO configReqDTO = new ConfigSaveValueReqDTO();
        configReqDTO.setKey(CONFIG_KEY);
        configReqDTO.setCategory(CONFIG_CATEGORY);
        configReqDTO.setName(CONFIG_NAME);
        configReqDTO.setValue(reqVO.getValue());
        configReqDTO.setVisible(false);
        configReqDTO.setRemark(CONFIG_REMARK);
        return configApi.saveConfigValueByKey(configReqDTO);
    }

}
