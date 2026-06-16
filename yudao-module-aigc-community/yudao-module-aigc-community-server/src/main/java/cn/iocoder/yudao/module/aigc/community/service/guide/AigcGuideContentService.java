package cn.iocoder.yudao.module.aigc.community.service.guide;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.community.controller.guide.vo.AigcGuideContentPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.guide.vo.AigcGuideContentSaveReqVO;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcGuideContentDO;

import java.util.List;

public interface AigcGuideContentService {

    Long createContent(AigcGuideContentSaveReqVO reqVO);

    void updateContent(AigcGuideContentSaveReqVO reqVO);

    void deleteContent(Long id);

    AigcGuideContentDO getContent(Long id);

    PageResult<AigcGuideContentDO> getContentPage(AigcGuideContentPageReqVO reqVO);

    void publishContent(Long id, Long publisherUserId);

    void unpublishContent(Long id);

    List<AigcGuideContentDO> getPublishedContentList();

    AigcGuideContentDO getPublishedContent(String slug);

}
