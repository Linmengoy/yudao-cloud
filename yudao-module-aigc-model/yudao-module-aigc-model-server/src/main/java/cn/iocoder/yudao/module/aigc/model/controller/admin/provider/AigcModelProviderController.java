package cn.iocoder.yudao.module.aigc.model.controller.admin.provider;

import cn.hutool.core.util.DesensitizedUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.provider.vo.AigcModelProviderPageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.provider.vo.AigcModelProviderSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProviderDO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelProviderRespDTO;
import cn.iocoder.yudao.module.aigc.model.service.provider.AigcModelProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 模型渠道商")
@RestController
@RequestMapping("/aigc/model/provider")
@Validated
public class AigcModelProviderController {

    @Resource
    private AigcModelProviderService providerService;

    @PostMapping("/create")
    @Operation(summary = "创建渠道商")
    @PreAuthorize("@ss.hasPermission('aigc:model:provider:create')")
    public CommonResult<Long> createProvider(@Valid @RequestBody AigcModelProviderSaveReqVO reqVO) {
        Long id = providerService.createProvider(reqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新渠道商")
    @PreAuthorize("@ss.hasPermission('aigc:model:provider:update')")
    public CommonResult<Boolean> updateProvider(@Valid @RequestBody AigcModelProviderSaveReqVO reqVO) {
        providerService.updateProvider(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除渠道商")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:provider:delete')")
    public CommonResult<Boolean> deleteProvider(@RequestParam("id") Long id) {
        providerService.deleteProvider(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取渠道商")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:provider:query')")
    public CommonResult<AigcModelProviderRespDTO> getProvider(@RequestParam("id") Long id) {
        AigcModelProviderDO provider = providerService.getProvider(id);
        return success(buildProviderRespDTO(provider, true));
    }

    @GetMapping("/page")
    @Operation(summary = "获取渠道商列表")
    @PreAuthorize("@ss.hasPermission('aigc:model:provider:query')")
    public CommonResult<PageResult<AigcModelProviderRespDTO>> getProviderPage(@Valid AigcModelProviderPageReqVO reqVO) {
        PageResult<AigcModelProviderDO> pageResult = providerService.getProviderPage(reqVO);
        return success(new PageResult<>(pageResult.getList().stream()
                .map(provider -> buildProviderRespDTO(provider, false))
                .toList(), pageResult.getTotal()));
    }

    @PutMapping("/status")
    @Operation(summary = "更新渠道商状态")
    @Parameter(name = "id", description = "ID", required = true)
    @Parameter(name = "status", description = "状态", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:provider:update')")
    public CommonResult<Boolean> updateProviderStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        providerService.updateProviderStatus(id, status);
        return success(true);
    }

    @PostMapping("/test")
    @Operation(summary = "测试渠道商")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:provider:query')")
    public CommonResult<Boolean> testProvider(@RequestParam("id") Long id) {
        providerService.testProvider(id);
        return success(true);
    }

    private AigcModelProviderRespDTO buildProviderRespDTO(AigcModelProviderDO provider, boolean detail) {
        AigcModelProviderRespDTO respDTO = BeanUtils.toBean(provider, AigcModelProviderRespDTO.class);
        respDTO.setApiKey(detail ? maskSecret(provider.getApiKey()) : null);
        respDTO.setSecretKey(detail ? maskSecret(provider.getSecretKey()) : null);
        respDTO.setProxyPassword(detail ? maskSecret(provider.getProxyPassword()) : null);
        return respDTO;
    }

    private String maskSecret(String value) {
        return DesensitizedUtil.password(value);
    }

}
