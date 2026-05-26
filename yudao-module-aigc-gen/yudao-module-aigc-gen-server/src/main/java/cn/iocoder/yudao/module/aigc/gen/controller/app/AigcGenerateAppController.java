package cn.iocoder.yudao.module.aigc.gen.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateRecordDO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateResultRespDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitRespDTO;
import cn.iocoder.yudao.module.aigc.gen.service.record.AigcGenerateRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户端 - AIGC 生成")
@RestController
@RequestMapping("/aigc/gen")
@Validated
public class AigcGenerateAppController {

    @Resource
    private AigcGenerateRecordService generateRecordService;

    @PostMapping("/submit")
    @Operation(summary = "提交生成任务")
    public CommonResult<AigcGenerateSubmitRespDTO> submit(@Valid @RequestBody AigcGenerateSubmitReqDTO reqDTO) {
        reqDTO.setUserId(getLoginUserId());
        AigcGenerateRecordDO record = generateRecordService.submitGenerate(reqDTO);
        return success(BeanUtils.toBean(record, AigcGenerateSubmitRespDTO.class));
    }

    @PostMapping("/text/generate")
    @Operation(summary = "文本生成")
    public CommonResult<AigcGenerateSubmitRespDTO> generateText(@Valid @RequestBody AigcGenerateSubmitReqDTO reqDTO) {
        reqDTO.setUserId(getLoginUserId()).setGenerateType("TEXT").setGenerateMode("TEXT_GENERATE");
        AigcGenerateRecordDO record = generateRecordService.submitGenerate(reqDTO);
        return success(BeanUtils.toBean(record, AigcGenerateSubmitRespDTO.class));
    }

    @PostMapping("/image/text-to-image")
    @Operation(summary = "文生图")
    public CommonResult<AigcGenerateSubmitRespDTO> textToImage(@Valid @RequestBody AigcGenerateSubmitReqDTO reqDTO) {
        reqDTO.setUserId(getLoginUserId()).setGenerateType("IMAGE").setGenerateMode("TEXT_TO_IMAGE");
        AigcGenerateRecordDO record = generateRecordService.submitGenerate(reqDTO);
        return success(BeanUtils.toBean(record, AigcGenerateSubmitRespDTO.class));
    }

    @PostMapping("/video/text-to-video")
    @Operation(summary = "文生视频")
    public CommonResult<AigcGenerateSubmitRespDTO> textToVideo(@Valid @RequestBody AigcGenerateSubmitReqDTO reqDTO) {
        reqDTO.setUserId(getLoginUserId()).setGenerateType("VIDEO").setGenerateMode("TEXT_TO_VIDEO");
        AigcGenerateRecordDO record = generateRecordService.submitGenerate(reqDTO);
        return success(BeanUtils.toBean(record, AigcGenerateSubmitRespDTO.class));
    }

    @GetMapping("/result")
    @Operation(summary = "获取生成结果")
    @Parameter(name = "taskId", description = "任务编号", required = true)
    public CommonResult<AigcGenerateResultRespDTO> getResult(@RequestParam("taskId") Long taskId) {
        AigcGenerateRecordDO record = generateRecordService.getGenerateRecordByTaskId(taskId);
        return success(BeanUtils.toBean(record, AigcGenerateResultRespDTO.class));
    }
}
