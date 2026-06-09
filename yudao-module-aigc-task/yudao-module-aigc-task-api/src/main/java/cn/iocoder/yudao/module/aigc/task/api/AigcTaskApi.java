package cn.iocoder.yudao.module.aigc.task.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCallbackCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskDurationStatisticsReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskDurationStatisticsRespDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskRespDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskRetryCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.task.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - AIGC 任务")
public interface AigcTaskApi {

    String PREFIX = ApiConstants.PREFIX;

    @PostMapping(PREFIX + "/create-task")
    @Operation(summary = "创建任务")
    CommonResult<Long> createTask(@RequestBody AigcTaskCreateReqDTO reqDTO);

    @GetMapping(PREFIX + "/get-task")
    @Operation(summary = "获取任务")
    @Parameter(name = "taskId", description = "任务编号", required = true, example = "1024")
    CommonResult<AigcTaskRespDTO> getTask(@RequestParam("taskId") Long taskId);

    @GetMapping(PREFIX + "/get-task-by-no")
    @Operation(summary = "根据任务编号获取任务")
    @Parameter(name = "taskNo", description = "任务编号", required = true, example = "TASK202605260001")
    CommonResult<AigcTaskRespDTO> getTaskByTaskNo(@RequestParam("taskNo") String taskNo);

    @PostMapping(PREFIX + "/success-duration-statistics")
    @Operation(summary = "获取成功任务耗时统计")
    CommonResult<AigcTaskDurationStatisticsRespDTO> getSuccessDurationStatistics(@RequestBody AigcTaskDurationStatisticsReqDTO reqDTO);

    @PutMapping(PREFIX + "/mark-queued")
    @Operation(summary = "标记排队中")
    CommonResult<Boolean> markQueued(@RequestParam("taskId") Long taskId);

    @PutMapping(PREFIX + "/mark-running")
    @Operation(summary = "标记运行中")
    CommonResult<Boolean> markRunning(@RequestParam("taskId") Long taskId);

    @PutMapping(PREFIX + "/mark-submitted")
    @Operation(summary = "标记已提交第三方")
    CommonResult<Boolean> markSubmitted(@RequestBody AigcTaskStatusUpdateReqDTO reqDTO);

    @PutMapping(PREFIX + "/mark-callback-waiting")
    @Operation(summary = "标记等待回调")
    CommonResult<Boolean> markCallbackWaiting(@RequestBody AigcTaskStatusUpdateReqDTO reqDTO);

    @PutMapping(PREFIX + "/mark-downloading")
    @Operation(summary = "标记下载中")
    CommonResult<Boolean> markDownloading(@RequestParam("taskId") Long taskId);

    @PutMapping(PREFIX + "/mark-asset-creating")
    @Operation(summary = "标记资产创建中")
    CommonResult<Boolean> markAssetCreating(@RequestParam("taskId") Long taskId);

    @PutMapping(PREFIX + "/mark-success")
    @Operation(summary = "标记成功")
    CommonResult<Boolean> markSuccess(@RequestBody AigcTaskStatusUpdateReqDTO reqDTO);

    @PutMapping(PREFIX + "/mark-failed")
    @Operation(summary = "标记失败")
    CommonResult<Boolean> markFailed(@RequestBody AigcTaskStatusUpdateReqDTO reqDTO);

    @PutMapping(PREFIX + "/mark-refunding")
    @Operation(summary = "标记退款中")
    CommonResult<Boolean> markRefunding(@RequestParam("taskId") Long taskId);

    @PutMapping(PREFIX + "/mark-refunded")
    @Operation(summary = "标记已退款")
    CommonResult<Boolean> markRefunded(@RequestParam("taskId") Long taskId);

    @PostMapping(PREFIX + "/create-callback-record")
    @Operation(summary = "创建回调记录")
    CommonResult<Long> createCallbackRecord(@RequestBody AigcTaskCallbackCreateReqDTO reqDTO);

    @PostMapping(PREFIX + "/create-retry-record")
    @Operation(summary = "创建重试记录")
    CommonResult<Long> createRetryRecord(@RequestBody AigcTaskRetryCreateReqDTO reqDTO);

}
