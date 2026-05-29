package cn.iocoder.yudao.module.aigc.billing.controller.app.packageconfig;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.billing.controller.app.packageconfig.vo.AppAigcRechargePackageRespVO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargePackageDO;
import cn.iocoder.yudao.module.aigc.billing.service.packageconfig.AigcRechargePackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "用户端 - AIGC 充值套餐")
@RestController
@RequestMapping("/aigc/billing/recharge-package")
@Validated
public class AppAigcRechargePackageController {

    @Resource
    private AigcRechargePackageService rechargePackageService;

    @GetMapping("/list-enabled")
    @Operation(summary = "获取启用充值套餐列表")
    public CommonResult<List<AppAigcRechargePackageRespVO>> getEnabledRechargePackageList() {
        List<AigcRechargePackageDO> list = rechargePackageService.getEnabledRechargePackageList();
        return success(BeanUtils.toBean(list, AppAigcRechargePackageRespVO.class));
    }

}
