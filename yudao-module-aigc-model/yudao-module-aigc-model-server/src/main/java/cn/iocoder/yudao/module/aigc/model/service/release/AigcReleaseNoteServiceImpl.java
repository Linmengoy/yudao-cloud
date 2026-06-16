package cn.iocoder.yudao.module.aigc.model.service.release;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.release.vo.AigcReleaseNotePageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.release.vo.AigcReleaseNoteSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcReleaseNoteDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcReleaseNoteMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.RELEASE_NOTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.RELEASE_NOTE_VERSION_DUPLICATE;

@Service
@Validated
public class AigcReleaseNoteServiceImpl implements AigcReleaseNoteService {

    @Resource
    private AigcReleaseNoteMapper releaseNoteMapper;

    @Override
    public Long createReleaseNote(AigcReleaseNoteSaveReqVO reqVO) {
        validateVersionUnique(null, reqVO.getVersion());
        AigcReleaseNoteDO releaseNote = BeanUtils.toBean(reqVO, AigcReleaseNoteDO.class);
        fillPublishTime(releaseNote, null);
        releaseNoteMapper.insert(releaseNote);
        return releaseNote.getId();
    }

    @Override
    public void updateReleaseNote(AigcReleaseNoteSaveReqVO reqVO) {
        AigcReleaseNoteDO oldReleaseNote = validateReleaseNoteExists(reqVO.getId());
        validateVersionUnique(reqVO.getId(), reqVO.getVersion());
        AigcReleaseNoteDO updateObj = BeanUtils.toBean(reqVO, AigcReleaseNoteDO.class);
        fillPublishTime(updateObj, oldReleaseNote);
        releaseNoteMapper.updateById(updateObj);
    }

    @Override
    public void deleteReleaseNote(Long id) {
        validateReleaseNoteExists(id);
        releaseNoteMapper.deleteById(id);
    }

    @Override
    public AigcReleaseNoteDO getReleaseNote(Long id) {
        return releaseNoteMapper.selectById(id);
    }

    @Override
    public PageResult<AigcReleaseNoteDO> getReleaseNotePage(AigcReleaseNotePageReqVO reqVO) {
        return releaseNoteMapper.selectPage(reqVO);
    }

    @Override
    public void updateReleaseNoteStatus(Long id, Integer status) {
        AigcReleaseNoteDO oldReleaseNote = validateReleaseNoteExists(id);
        AigcReleaseNoteDO updateObj = new AigcReleaseNoteDO().setId(id).setStatus(status);
        fillPublishTime(updateObj, oldReleaseNote);
        releaseNoteMapper.updateStatusAndPublishTime(id, status, updateObj.getPublishTime());
    }

    @Override
    public List<AigcReleaseNoteDO> getPublishedReleaseNotes(Integer limit) {
        return releaseNoteMapper.selectPublishedList(limit);
    }

    private AigcReleaseNoteDO validateReleaseNoteExists(Long id) {
        AigcReleaseNoteDO releaseNote = releaseNoteMapper.selectById(id);
        if (releaseNote == null) {
            throw exception(RELEASE_NOTE_NOT_EXISTS);
        }
        return releaseNote;
    }

    private void validateVersionUnique(Long id, String version) {
        AigcReleaseNoteDO releaseNote = releaseNoteMapper.selectByVersion(version);
        if (releaseNote == null || ObjectUtil.equal(releaseNote.getId(), id)) {
            return;
        }
        throw exception(RELEASE_NOTE_VERSION_DUPLICATE);
    }

    private void fillPublishTime(AigcReleaseNoteDO releaseNote, AigcReleaseNoteDO oldReleaseNote) {
        if (!CommonStatusEnum.isEnable(releaseNote.getStatus())) {
            releaseNote.setPublishTime(null);
            return;
        }
        if (oldReleaseNote != null && oldReleaseNote.getPublishTime() != null && CommonStatusEnum.isEnable(oldReleaseNote.getStatus())) {
            releaseNote.setPublishTime(oldReleaseNote.getPublishTime());
            return;
        }
        releaseNote.setPublishTime(LocalDateTime.now());
    }

}
