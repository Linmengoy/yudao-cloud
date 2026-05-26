package cn.iocoder.yudao.module.aigc.safety.service.sensitiveword;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo.AigcSensitiveWordPageReqVO;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo.AigcSensitiveWordSaveReqVO;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo.AigcSensitiveWordStatusReqVO;
import cn.iocoder.yudao.module.aigc.safety.dal.dataobject.AigcSensitiveWordDO;

import java.util.List;

public interface AigcSensitiveWordService {

    Long createSensitiveWord(AigcSensitiveWordSaveReqVO reqVO);

    void updateSensitiveWord(AigcSensitiveWordSaveReqVO reqVO);

    void deleteSensitiveWord(Long id);

    AigcSensitiveWordDO getSensitiveWord(Long id);

    AigcSensitiveWordDO validateSensitiveWordExists(Long id);

    PageResult<AigcSensitiveWordDO> getSensitiveWordPage(AigcSensitiveWordPageReqVO reqVO);

    void updateSensitiveWordStatus(AigcSensitiveWordStatusReqVO reqVO);

    List<AigcSensitiveWordDO> getEnabledSensitiveWords(String scene);

}
