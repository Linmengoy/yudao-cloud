package cn.iocoder.yudao.module.aigc.gen.service.record;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.callback.vo.AigcGenerateCallbackPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.providerlog.vo.AigcGenerateProviderLogPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.record.vo.AigcGenerateRecordPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateCallbackDO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateProviderLogDO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateRecordDO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitReqDTO;

public interface AigcGenerateRecordService {

    AigcGenerateRecordDO createGenerateRecord(AigcGenerateSubmitReqDTO reqDTO);

    AigcGenerateRecordDO submitGenerate(AigcGenerateSubmitReqDTO reqDTO);

    AigcGenerateRecordDO getGenerateRecord(Long id);

    AigcGenerateRecordDO getGenerateRecordByTaskId(Long taskId);

    AigcGenerateRecordDO validateGenerateRecordExists(Long id);

    PageResult<AigcGenerateRecordDO> getGenerateRecordPage(AigcGenerateRecordPageReqVO reqVO);

    PageResult<AigcGenerateCallbackDO> getCallbackPage(AigcGenerateCallbackPageReqVO reqVO);

    PageResult<AigcGenerateProviderLogDO> getProviderLogPage(AigcGenerateProviderLogPageReqVO reqVO);

    void createCallback(AigcGenerateCallbackReqDTO reqDTO);

    void syncTask(Long taskId);

    int syncTimeoutTasks();

}
