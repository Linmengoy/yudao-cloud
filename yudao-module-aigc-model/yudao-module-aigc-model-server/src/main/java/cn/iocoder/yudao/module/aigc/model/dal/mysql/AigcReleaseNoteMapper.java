package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.model.controller.admin.release.vo.AigcReleaseNotePageReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcReleaseNoteDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AigcReleaseNoteMapper extends BaseMapperX<AigcReleaseNoteDO> {

    default AigcReleaseNoteDO selectByVersion(String version) {
        return selectOne(AigcReleaseNoteDO::getVersion, version);
    }

    default PageResult<AigcReleaseNoteDO> selectPage(AigcReleaseNotePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcReleaseNoteDO>()
                .likeIfPresent(AigcReleaseNoteDO::getVersion, reqVO.getVersion())
                .likeIfPresent(AigcReleaseNoteDO::getTitle, reqVO.getTitle())
                .eqIfPresent(AigcReleaseNoteDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AigcReleaseNoteDO::getReleaseDate, reqVO.getReleaseDate())
                .orderByDesc(AigcReleaseNoteDO::getReleaseDate)
                .orderByDesc(AigcReleaseNoteDO::getPublishTime)
                .orderByDesc(AigcReleaseNoteDO::getId));
    }

    default List<AigcReleaseNoteDO> selectPublishedList(Integer limit) {
        return selectList(new LambdaQueryWrapperX<AigcReleaseNoteDO>()
                .eq(AigcReleaseNoteDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .le(AigcReleaseNoteDO::getReleaseDate, LocalDate.now())
                .orderByDesc(AigcReleaseNoteDO::getReleaseDate)
                .orderByDesc(AigcReleaseNoteDO::getPublishTime)
                .orderByDesc(AigcReleaseNoteDO::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit == null ? 20 : limit, 50))));
    }

    default int updateStatusAndPublishTime(Long id, Integer status, LocalDateTime publishTime) {
        return update(null, new LambdaUpdateWrapper<AigcReleaseNoteDO>()
                .set(AigcReleaseNoteDO::getStatus, status)
                .set(AigcReleaseNoteDO::getPublishTime, publishTime)
                .eq(AigcReleaseNoteDO::getId, id));
    }

}
