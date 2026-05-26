package cn.iocoder.yudao.module.aigc.gen.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateRecordDO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateResultRespDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitRespDTO;
import cn.iocoder.yudao.module.aigc.gen.service.record.AigcGenerateRecordService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class AigcGenerateApiImpl implements AigcGenerateApi {

    @Resource
    private AigcGenerateRecordService generateRecordService;

    @Override
    public CommonResult<AigcGenerateSubmitRespDTO> submit(AigcGenerateSubmitReqDTO reqDTO) {
        AigcGenerateRecordDO record = generateRecordService.submitGenerate(reqDTO);
        return success(BeanUtils.toBean(record, AigcGenerateSubmitRespDTO.class));
    }

    @Override
    public CommonResult<AigcGenerateResultRespDTO> getResult(Long taskId) {
        AigcGenerateRecordDO record = generateRecordService.getGenerateRecordByTaskId(taskId);
        return success(BeanUtils.toBean(record, AigcGenerateResultRespDTO.class));
    }

    @Override
    public CommonResult<Boolean> handleCallback(AigcGenerateCallbackReqDTO reqDTO) {
        generateRecordService.createCallback(reqDTO);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> syncTask(Long taskId) {
        generateRecordService.syncTask(taskId);
        return success(true);
    }
}
