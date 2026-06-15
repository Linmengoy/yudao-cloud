package cn.iocoder.yudao.module.aigc.community.service.guide;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.community.controller.guide.vo.AigcGuideContentPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.guide.vo.AigcGuideContentSaveReqVO;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcGuideContentDO;
import cn.iocoder.yudao.module.aigc.community.dal.mysql.AigcGuideContentMapper;
import cn.iocoder.yudao.module.aigc.community.enums.AigcGuidePublishStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.community.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AigcGuideContentServiceImpl implements AigcGuideContentService {

    @Resource
    private AigcGuideContentMapper guideContentMapper;

    @Override
    public Long createContent(AigcGuideContentSaveReqVO reqVO) {
        validateSlugUnique(null, reqVO.getSlug());
        AigcGuideContentDO content = BeanUtils.toBean(reqVO, AigcGuideContentDO.class)
                .setPublishStatus(AigcGuidePublishStatusEnum.DRAFT.getCode());
        guideContentMapper.insert(content);
        return content.getId();
    }

    @Override
    public void updateContent(AigcGuideContentSaveReqVO reqVO) {
        AigcGuideContentDO slugContent = guideContentMapper.selectBySlug(reqVO.getSlug());
        if (slugContent != null && !ObjectUtil.equal(slugContent.getId(), reqVO.getId())) {
            throw exception(GUIDE_CONTENT_SLUG_DUPLICATE);
        }
        if (guideContentMapper.updateById(BeanUtils.toBean(reqVO, AigcGuideContentDO.class)) == 0) {
            throw exception(GUIDE_CONTENT_NOT_EXISTS);
        }
    }

    @Override
    public void deleteContent(Long id) {
        if (guideContentMapper.deleteById(id) == 0) {
            throw exception(GUIDE_CONTENT_NOT_EXISTS);
        }
    }

    @Override
    public AigcGuideContentDO getContent(Long id) {
        return validateContentExists(id);
    }

    @Override
    public PageResult<AigcGuideContentDO> getContentPage(AigcGuideContentPageReqVO reqVO) {
        return guideContentMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishContent(Long id, Long publisherUserId) {
        if (guideContentMapper.updateById(new AigcGuideContentDO()
                .setId(id)
                .setPublishStatus(AigcGuidePublishStatusEnum.PUBLISHED.getCode())
                .setPublishTime(LocalDateTime.now())
                .setPublisherUserId(publisherUserId)) == 0) {
            throw exception(GUIDE_CONTENT_NOT_EXISTS);
        }
    }

    @Override
    public void unpublishContent(Long id) {
        if (guideContentMapper.updateById(new AigcGuideContentDO()
                .setId(id)
                .setPublishStatus(AigcGuidePublishStatusEnum.DRAFT.getCode())) == 0) {
            throw exception(GUIDE_CONTENT_NOT_EXISTS);
        }
    }

    @Override
    public List<AigcGuideContentDO> getPublishedContentList() {
        return guideContentMapper.selectPublishedList();
    }

    @Override
    public AigcGuideContentDO getPublishedContent(String slug) {
        AigcGuideContentDO content = guideContentMapper.selectPublishedBySlug(slug);
        if (content == null) {
            throw exception(GUIDE_CONTENT_NOT_PUBLISHED);
        }
        return content;
    }

    private AigcGuideContentDO validateContentExists(Long id) {
        AigcGuideContentDO content = guideContentMapper.selectById(id);
        if (content == null) {
            throw exception(GUIDE_CONTENT_NOT_EXISTS);
        }
        return content;
    }

    private void validateSlugUnique(Long id, String slug) {
        AigcGuideContentDO content = guideContentMapper.selectBySlug(slug);
        if (content == null || ObjectUtil.equal(content.getId(), id)) {
            return;
        }
        throw exception(GUIDE_CONTENT_SLUG_DUPLICATE);
    }

}
