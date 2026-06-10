package cn.iocoder.yudao.module.aigc.model.service.channel;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.model.controller.admin.channel.vo.AigcModelChannelPageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.channel.vo.AigcModelChannelSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelChannelDO;

import java.util.List;

public interface AigcModelChannelService {

    Long createChannel(AigcModelChannelSaveReqVO reqVO);

    void updateChannel(AigcModelChannelSaveReqVO reqVO);

    void deleteChannel(Long id);

    AigcModelChannelDO getChannel(Long id);

    AigcModelChannelDO validateChannelExists(Long id);

    AigcModelChannelDO validateChannelExistsAndEnable(Long id);

    PageResult<AigcModelChannelDO> getChannelPage(AigcModelChannelPageReqVO reqVO);

    List<AigcModelChannelDO> listChannelsByModelId(Long modelId);

    List<AigcModelChannelDO> listEnabledChannelsByModelId(Long modelId);

    void updateChannelStatus(Long id, Integer status);

}
