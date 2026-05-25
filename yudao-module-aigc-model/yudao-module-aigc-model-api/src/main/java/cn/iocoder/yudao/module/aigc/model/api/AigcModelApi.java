package cn.iocoder.yudao.module.aigc.model.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.aigc.model.dto.*;
import cn.iocoder.yudao.module.aigc.model.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - AIGC 模型")
public interface AigcModelApi {

    String PREFIX = ApiConstants.PREFIX;

    @GetMapping(PREFIX + "/validate-model")
    @Operation(summary = "校验模型是否可用")
    @Parameters({
            @Parameter(name = "modelId", description = "模型编号", required = true, example = "1024"),
            @Parameter(name = "capability", description = "模型能力", required = true, example = "TEXT_TO_IMAGE")
    })
    CommonResult<AigcModelRespDTO> validateModel(@RequestParam("modelId") Long modelId,
                                                 @RequestParam("capability") String capability);

    @GetMapping(PREFIX + "/get-provider")
    @Operation(summary = "获取渠道商")
    @Parameter(name = "providerId", description = "渠道商编号", required = true, example = "1024")
    CommonResult<AigcModelProviderRespDTO> getProvider(@RequestParam("providerId") Long providerId);

    @GetMapping(PREFIX + "/get-model")
    @Operation(summary = "获取模型")
    @Parameter(name = "modelId", description = "模型编号", required = true, example = "1024")
    CommonResult<AigcModelRespDTO> getModel(@RequestParam("modelId") Long modelId);

    @GetMapping(PREFIX + "/list-available-models")
    @Operation(summary = "获取可用模型列表")
    @Parameters({
            @Parameter(name = "type", description = "模型类型", example = "1"),
            @Parameter(name = "capability", description = "模型能力", example = "TEXT_TO_IMAGE")
    })
    CommonResult<List<AigcModelRespDTO>> listAvailableModels(@RequestParam(value = "type", required = false) Integer type,
                                                             @RequestParam(value = "capability", required = false) String capability);

    @GetMapping(PREFIX + "/get-param-templates")
    @Operation(summary = "获取模型参数模板")
    @Parameters({
            @Parameter(name = "modelId", description = "模型编号", required = true, example = "1024"),
            @Parameter(name = "capability", description = "模型能力", required = true, example = "TEXT_TO_IMAGE")
    })
    CommonResult<List<AigcModelParamTemplateRespDTO>> getParamTemplates(@RequestParam("modelId") Long modelId,
                                                                        @RequestParam("capability") String capability);

    @PostMapping(PREFIX + "/validate-params")
    @Operation(summary = "校验模型参数")
    CommonResult<Boolean> validateParams(@RequestBody AigcModelValidateReqDTO reqDTO);

    @PostMapping(PREFIX + "/calculate-price")
    @Operation(summary = "计算模型价格")
    CommonResult<AigcModelPriceCalculateRespDTO> calculatePrice(@RequestBody AigcModelPriceCalculateReqDTO reqDTO);

}
