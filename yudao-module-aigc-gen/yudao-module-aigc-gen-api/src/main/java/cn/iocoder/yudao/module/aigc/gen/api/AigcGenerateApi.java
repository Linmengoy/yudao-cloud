package cn.iocoder.yudao.module.aigc.gen.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateResultRespDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitRespDTO;
import cn.iocoder.yudao.module.aigc.gen.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - AIGC 生成")
public interface AigcGenerateApi {

    String PREFIX = ApiConstants.PREFIX;

    @PostMapping(PREFIX + "/submit")
    @Operation(summary = "提交生成任务")
    CommonResult<AigcGenerateSubmitRespDTO> submit(@Valid @RequestBody AigcGenerateSubmitReqDTO reqDTO);

    @GetMapping(PREFIX + "/result")
    @Operation(summary = "获取生成结果")
    CommonResult<AigcGenerateResultRespDTO> getResult(@RequestParam("taskId") Long taskId);

    @GetMapping(PREFIX + "/results")
    @Operation(summary = "批量获取生成结果")
    CommonResult<List<AigcGenerateResultRespDTO>> getResults(@RequestParam("taskIds") List<Long> taskIds);

    @PostMapping(PREFIX + "/callback")
    @Operation(summary = "处理第三方回调")
    CommonResult<Boolean> handleCallback(@Valid @RequestBody AigcGenerateCallbackReqDTO reqDTO);

    @PostMapping(PREFIX + "/sync-task")
    @Operation(summary = "同步第三方任务")
    CommonResult<Boolean> syncTask(@RequestParam("taskId") Long taskId);

}
