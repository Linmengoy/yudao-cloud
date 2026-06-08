package cn.iocoder.yudao.module.aigc.asset.service.asset;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetDownloadLogPageReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetPageReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetSaveReqVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDownloadLogDO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAccessUrlReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAccessUrlRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAuditUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetDownloadReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetPageReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetVisibilityUpdateReqDTO;

import java.util.Collection;
import java.util.List;

public interface AigcAssetService {

    Long createAsset(AigcAssetSaveReqVO reqVO);

    Long uploadAsset(Long userId, String assetType, String title, String fileName, String mimeType, byte[] content);

    Long captureVideoFrame(Long userId, Long videoAssetId, String capturedAt, java.math.BigDecimal timeSec, String title);

    AigcAssetCreateRespDTO createAsset(AigcAssetCreateReqDTO reqDTO);

    void updateAsset(AigcAssetSaveReqVO reqVO);

    void updateAsset(AigcAssetUpdateReqDTO reqDTO);

    void deleteAsset(Long id);

    void recoverAsset(Long id);

    AigcAssetDO getAsset(Long id);

    AigcAssetRespDTO getAssetResp(Long id, Long userId);

    List<AigcAssetDO> getAssetList(Collection<Long> ids);

    List<AigcAssetRespDTO> getAssetRespList(Collection<Long> ids, Long userId);

    AigcAssetDO validateAssetExists(Long id);

    AigcAssetDO getAssetByTaskId(Long taskId);

    PageResult<AigcAssetDO> getAssetPage(AigcAssetPageReqVO reqVO);

    PageResult<AigcAssetDO> getAssetPage(AigcAssetPageReqDTO reqDTO);

    AigcAssetDO getUserAsset(Long id, Long userId);

    AigcAssetDO getAccessibleAsset(Long id, Long userId);

    PageResult<AigcAssetDO> getUserAssetPage(AigcAssetPageReqVO reqVO, Long userId);

    List<AigcAssetDO> getUserAssetList(AigcAssetPageReqVO reqVO, Long userId);

    void updateAuditStatus(AigcAssetAuditUpdateReqDTO reqDTO);

    void updateVisibility(AigcAssetVisibilityUpdateReqDTO reqDTO);

    void increaseDownloadCount(AigcAssetDownloadReqDTO reqDTO);

    AigcAssetAccessUrlRespDTO getAccessUrl(AigcAssetAccessUrlReqDTO reqDTO, Long userId);

    List<AigcAssetAccessUrlRespDTO> getAccessUrls(List<AigcAssetAccessUrlReqDTO> reqDTOs, Long userId);

    void increaseUseCount(Long id, Long userId);

    PageResult<AigcAssetDownloadLogDO> getDownloadLogPage(AigcAssetDownloadLogPageReqVO reqVO);

    Long getAssetCount();

    Long getDownloadCount();

}
