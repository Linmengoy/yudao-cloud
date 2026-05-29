package cn.iocoder.yudao.module.aigc.billing.controller.admin.packageconfig;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.packageconfig.vo.AigcRechargePackagePageReqVO;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.packageconfig.vo.AigcRechargePackageRespVO;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.packageconfig.vo.AigcRechargePackageSaveReqVO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargePackageDO;
import cn.iocoder.yudao.module.aigc.billing.service.packageconfig.AigcRechargePackageService;
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

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 充值套餐")
@RestController
@RequestMapping("/aigc/billing/recharge-package")
@Validated
public class AigcRechargePackageController {

    @Resource
    private AigcRechargePackageService rechargePackageService;

    @PostMapping("/create")
    @Operation(summary = "创建充值套餐")
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge-package:create')")
    public CommonResult<Long> createRechargePackage(@Valid @RequestBody AigcRechargePackageSaveReqVO createReqVO) {
        return success(rechargePackageService.createRechargePackage(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新充值套餐")
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge-package:update')")
    public CommonResult<Boolean> updateRechargePackage(@Valid @RequestBody AigcRechargePackageSaveReqVO updateReqVO) {
        rechargePackageService.updateRechargePackage(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除充值套餐")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge-package:delete')")
    public CommonResult<Boolean> deleteRechargePackage(@RequestParam("id") Long id) {
        rechargePackageService.deleteRechargePackage(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取充值套餐")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge-package:query')")
    public CommonResult<AigcRechargePackageRespVO> getRechargePackage(@RequestParam("id") Long id) {
        AigcRechargePackageDO rechargePackage = rechargePackageService.getRechargePackage(id);
        return success(BeanUtils.toBean(rechargePackage, AigcRechargePackageRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取充值套餐分页")
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge-package:query')")
    public CommonResult<PageResult<AigcRechargePackageRespVO>> getRechargePackagePage(@Valid AigcRechargePackagePageReqVO pageReqVO) {
        PageResult<AigcRechargePackageDO> pageResult = rechargePackageService.getRechargePackagePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AigcRechargePackageRespVO.class));
    }

    @GetMapping("/list-enabled")
    @Operation(summary = "获取启用充值套餐列表")
    public CommonResult<List<AigcRechargePackageRespVO>> getEnabledRechargePackageList() {
        List<AigcRechargePackageDO> list = rechargePackageService.getEnabledRechargePackageList();
        return success(BeanUtils.toBean(list, AigcRechargePackageRespVO.class));
    }

}
