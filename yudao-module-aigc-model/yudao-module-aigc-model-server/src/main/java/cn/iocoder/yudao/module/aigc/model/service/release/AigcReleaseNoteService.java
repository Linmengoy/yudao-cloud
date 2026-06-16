package cn.iocoder.yudao.module.aigc.model.service.release;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.model.controller.admin.release.vo.AigcReleaseNotePageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.release.vo.AigcReleaseNoteSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcReleaseNoteDO;

import java.util.List;

public interface AigcReleaseNoteService {

    Long createReleaseNote(AigcReleaseNoteSaveReqVO reqVO);

    void updateReleaseNote(AigcReleaseNoteSaveReqVO reqVO);

    void deleteReleaseNote(Long id);

    AigcReleaseNoteDO getReleaseNote(Long id);

    PageResult<AigcReleaseNoteDO> getReleaseNotePage(AigcReleaseNotePageReqVO reqVO);

    void updateReleaseNoteStatus(Long id, Integer status);

    List<AigcReleaseNoteDO> getPublishedReleaseNotes(Integer limit);

}
