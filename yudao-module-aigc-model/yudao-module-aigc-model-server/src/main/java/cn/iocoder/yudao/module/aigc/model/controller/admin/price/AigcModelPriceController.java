package cn.iocoder.yudao.module.aigc.model.controller.admin.price;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.price.vo.AigcModelPriceSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelPriceDO;
import cn.iocoder.yudao.module.aigc.model.service.price.AigcModelPriceService;
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

@Tag(name = "管理后台 - AIGC 模型价格规则")
@RestController
@RequestMapping("/aigc/model/price")
@Validated
public class AigcModelPriceController {

    @Resource
    private AigcModelPriceService priceService;

    @PostMapping("/create")
    @Operation(summary = "创建价格规则")
    @PreAuthorize("@ss.hasPermission('aigc:model:price:create')")
    public CommonResult<Long> createPrice(@Valid @RequestBody AigcModelPriceSaveReqVO reqVO) {
        Long id = priceService.createPrice(reqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新价格规则")
    @PreAuthorize("@ss.hasPermission('aigc:model:price:update')")
    public CommonResult<Boolean> updatePrice(@Valid @RequestBody AigcModelPriceSaveReqVO reqVO) {
        priceService.updatePrice(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除价格规则")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:price:delete')")
    public CommonResult<Boolean> deletePrice(@RequestParam("id") Long id) {
        priceService.deletePrice(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取价格规则")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:price:query')")
    public CommonResult<AigcModelPriceDO> getPrice(@RequestParam("id") Long id) {
        return success(priceService.getPrice(id));
    }

    @GetMapping("/list")
    @Operation(summary = "获取价格规则列表")
    @Parameter(name = "modelId", description = "模型ID", required = true)
    @Parameter(name = "capability", description = "能力", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:price:query')")
    public CommonResult<List<AigcModelPriceDO>> getPriceList(
            @RequestParam("modelId") Long modelId,
            @RequestParam("capability") String capability) {
        return success(priceService.getPriceList(modelId, capability));
    }

    @PutMapping("/status")
    @Operation(summary = "更新价格规则状态")
    @Parameter(name = "id", description = "ID", required = true)
    @Parameter(name = "status", description = "状态", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:price:update')")
    public CommonResult<Boolean> updatePriceStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        priceService.updatePriceStatus(id, status);
        return success(true);
    }

}
