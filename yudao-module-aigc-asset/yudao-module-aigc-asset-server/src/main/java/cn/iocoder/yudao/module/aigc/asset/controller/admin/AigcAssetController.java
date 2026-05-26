package cn.iocoder.yudao.module.aigc.asset.controller.admin;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetDownloadLogPageReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetDownloadLogRespVO;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetPageReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetSaveReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetStatisticsRespVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDownloadLogDO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAuditUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetVisibilityUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.service.asset.AigcAssetService;
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

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;

@Tag(name = "管理后台 - AIGC 资产")
@RestController
@RequestMapping("/aigc/asset")
@Validated
public class AigcAssetController {

    @Resource
    private AigcAssetService assetService;

    @PostMapping("/create")
    @Operation(summary = "创建资产")
    @PreAuthorize("@ss.hasPermission('aigc:asset:create')")
    public CommonResult<Long> createAsset(@Valid @RequestBody AigcAssetSaveReqVO reqVO) {
        return success(assetService.createAsset(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资产")
    @PreAuthorize("@ss.hasPermission('aigc:asset:update')")
    public CommonResult<Boolean> updateAsset(@Valid @RequestBody AigcAssetSaveReqVO reqVO) {
        assetService.updateAsset(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资产")
    @Parameter(name = "id", description = "资产编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:asset:delete')")
    public CommonResult<Boolean> deleteAsset(@RequestParam("id") Long id) {
        assetService.deleteAsset(id);
        return success(true);
    }

    @PutMapping("/recover")
    @Operation(summary = "恢复资产")
    @Parameter(name = "id", description = "资产编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:asset:update')")
    public CommonResult<Boolean> recoverAsset(@RequestParam("id") Long id) {
        assetService.recoverAsset(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取资产")
    @Parameter(name = "id", description = "资产编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:asset:query')")
    public CommonResult<AigcAssetRespDTO> getAsset(@RequestParam("id") Long id) {
        AigcAssetDO asset = assetService.validateAssetExists(id);
        return success(BeanUtils.toBean(asset, AigcAssetRespDTO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取资产分页")
    @PreAuthorize("@ss.hasPermission('aigc:asset:query')")
    public CommonResult<PageResult<AigcAssetRespDTO>> getAssetPage(@Valid AigcAssetPageReqVO reqVO) {
        PageResult<AigcAssetDO> pageResult = assetService.getAssetPage(reqVO);
        return success(BeanUtils.toBean(pageResult, AigcAssetRespDTO.class));
    }

    @PutMapping("/audit")
    @Operation(summary = "更新审核状态")
    @PreAuthorize("@ss.hasPermission('aigc:asset:audit')")
    public CommonResult<Boolean> updateAuditStatus(@Valid @RequestBody AigcAssetAuditUpdateReqDTO reqDTO) {
        assetService.updateAuditStatus(reqDTO);
        return success(true);
    }

    @PutMapping("/visibility")
    @Operation(summary = "更新可见性")
    @PreAuthorize("@ss.hasPermission('aigc:asset:update')")
    public CommonResult<Boolean> updateVisibility(@Valid @RequestBody AigcAssetVisibilityUpdateReqDTO reqDTO) {
        assetService.updateVisibility(reqDTO);
        return success(true);
    }

    @GetMapping("/download-log/page")
    @Operation(summary = "获取下载日志分页")
    @PreAuthorize("@ss.hasPermission('aigc:asset:query')")
    public CommonResult<PageResult<AigcAssetDownloadLogRespVO>> getDownloadLogPage(@Valid AigcAssetDownloadLogPageReqVO reqVO) {
        PageResult<AigcAssetDownloadLogDO> pageResult = assetService.getDownloadLogPage(reqVO);
        return success(BeanUtils.toBean(pageResult, AigcAssetDownloadLogRespVO.class));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取资产统计")
    @PreAuthorize("@ss.hasPermission('aigc:asset:query')")
    public CommonResult<AigcAssetStatisticsRespVO> getStatistics() {
        return success(new AigcAssetStatisticsRespVO()
                .setAssetCount(assetService.getAssetCount())
                .setDownloadCount(assetService.getDownloadCount()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出资产列表")
    @PreAuthorize("@ss.hasPermission('aigc:asset:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAssetExcel(@Valid AigcAssetPageReqVO reqVO, HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AigcAssetDO> list = assetService.getAssetPage(reqVO).getList();
        ExcelUtils.write(response, "AIGC资产.xls", "数据", AigcAssetRespDTO.class,
                BeanUtils.toBean(list, AigcAssetRespDTO.class));
    }

}
