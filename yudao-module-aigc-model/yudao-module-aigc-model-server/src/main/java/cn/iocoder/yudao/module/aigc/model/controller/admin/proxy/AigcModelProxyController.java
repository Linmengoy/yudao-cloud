package cn.iocoder.yudao.module.aigc.model.controller.admin.proxy;

import cn.hutool.core.util.DesensitizedUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.proxy.vo.AigcModelProxyPageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.proxy.vo.AigcModelProxySaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProxyDO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelProxyRespDTO;
import cn.iocoder.yudao.module.aigc.model.service.proxy.AigcModelProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 模型代理")
@RestController
@RequestMapping("/aigc/model/proxy")
@Validated
public class AigcModelProxyController {

    @Resource
    private AigcModelProxyService proxyService;

    @PostMapping("/create")
    @Operation(summary = "创建代理")
    @PreAuthorize("@ss.hasPermission('aigc:model:proxy:create')")
    public CommonResult<Long> createProxy(@Valid @RequestBody AigcModelProxySaveReqVO reqVO) {
        return success(proxyService.createProxy(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新代理")
    @PreAuthorize("@ss.hasPermission('aigc:model:proxy:update')")
    public CommonResult<Boolean> updateProxy(@Valid @RequestBody AigcModelProxySaveReqVO reqVO) {
        proxyService.updateProxy(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除代理")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:proxy:delete')")
    public CommonResult<Boolean> deleteProxy(@RequestParam("id") Long id) {
        proxyService.deleteProxy(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取代理")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:proxy:query')")
    public CommonResult<AigcModelProxyRespDTO> getProxy(@RequestParam("id") Long id) {
        return success(buildProxyRespDTO(proxyService.validateProxyExists(id), true));
    }

    @GetMapping("/page")
    @Operation(summary = "获取代理列表")
    @PreAuthorize("@ss.hasPermission('aigc:model:proxy:query')")
    public CommonResult<PageResult<AigcModelProxyRespDTO>> getProxyPage(@Valid AigcModelProxyPageReqVO reqVO) {
        PageResult<AigcModelProxyDO> pageResult = proxyService.getProxyPage(reqVO);
        return success(new PageResult<>(pageResult.getList().stream()
                .map(proxy -> buildProxyRespDTO(proxy, false))
                .toList(), pageResult.getTotal()));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获取启用代理精简列表")
    @PreAuthorize("@ss.hasPermission('aigc:model:provider:query') or @ss.hasPermission('aigc:model:proxy:query')")
    public CommonResult<List<AigcModelProxyRespDTO>> getSimpleProxyList() {
        return success(proxyService.getSimpleProxyList().stream()
                .map(proxy -> buildProxyRespDTO(proxy, false))
                .toList());
    }

    @PutMapping("/status")
    @Operation(summary = "更新代理状态")
    @Parameter(name = "id", description = "ID", required = true)
    @Parameter(name = "status", description = "状态", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:proxy:update')")
    public CommonResult<Boolean> updateProxyStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        proxyService.updateProxyStatus(id, status);
        return success(true);
    }

    @GetMapping("/test")
    @Operation(summary = "测试代理")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:proxy:query')")
    public CommonResult<Long> testProxy(@RequestParam("id") Long id) {
        return success(proxyService.testProxy(id));
    }

    private AigcModelProxyRespDTO buildProxyRespDTO(AigcModelProxyDO proxy, boolean detail) {
        AigcModelProxyRespDTO respDTO = BeanUtils.toBean(proxy, AigcModelProxyRespDTO.class);
        respDTO.setPassword(detail ? maskSecret(proxy.getPassword()) : null);
        return respDTO;
    }

    private String maskSecret(String value) {
        if (value == null) {
            return null;
        }
        return DesensitizedUtil.password(value);
    }

}
