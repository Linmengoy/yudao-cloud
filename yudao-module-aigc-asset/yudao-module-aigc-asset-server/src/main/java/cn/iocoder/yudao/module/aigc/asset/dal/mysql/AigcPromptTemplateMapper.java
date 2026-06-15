package cn.iocoder.yudao.module.aigc.asset.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplatePageReqVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcPromptTemplateDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AigcPromptTemplateMapper extends BaseMapperX<AigcPromptTemplateDO> {

    default PageResult<AigcPromptTemplateDO> selectPage(AigcPromptTemplatePageReqVO reqVO) {
        return selectPage(reqVO, buildPageWrapper(reqVO)
                .orderByDesc(AigcPromptTemplateDO::getFeatured)
                .orderByAsc(AigcPromptTemplateDO::getSort)
                .orderByDesc(AigcPromptTemplateDO::getId));
    }

    default AigcPromptTemplateDO selectBySource(String sourceRepo, Long sourceCaseId) {
        return selectOne(AigcPromptTemplateDO::getSourceRepo, sourceRepo,
                AigcPromptTemplateDO::getSourceCaseId, sourceCaseId);
    }

    default LambdaQueryWrapperX<AigcPromptTemplateDO> buildPageWrapper(AigcPromptTemplatePageReqVO reqVO) {
        LambdaQueryWrapperX<AigcPromptTemplateDO> wrapper = new LambdaQueryWrapperX<AigcPromptTemplateDO>()
                .eq(AigcPromptTemplateDO::getStatus, "NORMAL")
                .eq(AigcPromptTemplateDO::getVisibility, "PUBLIC")
                .eq(AigcPromptTemplateDO::getAuditStatus, "PASS")
                .eqIfPresent(AigcPromptTemplateDO::getCategory, reqVO.getCategory())
                .eqIfPresent(AigcPromptTemplateDO::getFeatured, reqVO.getFeatured());
        if (reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank()) {
            String keyword = reqVO.getKeyword().trim();
            wrapper.and(w -> w.like(AigcPromptTemplateDO::getTitle, keyword)
                    .or().like(AigcPromptTemplateDO::getPrompt, keyword)
                    .or().like(AigcPromptTemplateDO::getCategory, keyword)
                    .or().like(AigcPromptTemplateDO::getTags, keyword)
                    .or().like(AigcPromptTemplateDO::getStyles, keyword)
                    .or().like(AigcPromptTemplateDO::getScenes, keyword));
        }
        if (reqVO.getStyle() != null && !reqVO.getStyle().isBlank()) {
            wrapper.like(AigcPromptTemplateDO::getStyles, reqVO.getStyle().trim());
        }
        if (reqVO.getScene() != null && !reqVO.getScene().isBlank()) {
            wrapper.like(AigcPromptTemplateDO::getScenes, reqVO.getScene().trim());
        }
        return wrapper;
    }

    @Select("""
            SELECT DISTINCT category
            FROM aigc_prompt_template
            WHERE deleted = 0
              AND status = 'NORMAL'
              AND visibility = 'PUBLIC'
              AND audit_status = 'PASS'
              AND category IS NOT NULL
              AND category <> ''
            ORDER BY category ASC
            """)
    List<String> selectCategoryList();

    default int increaseViewCount(Long id) {
        return update(null, new LambdaUpdateWrapper<AigcPromptTemplateDO>()
                .eq(AigcPromptTemplateDO::getId, id)
                .setSql("view_count = view_count + 1"));
    }

    default int increaseCopyCount(Long id) {
        return update(null, new LambdaUpdateWrapper<AigcPromptTemplateDO>()
                .eq(AigcPromptTemplateDO::getId, id)
                .setSql("copy_count = copy_count + 1"));
    }

    default int increaseUseCount(Long id) {
        return update(null, new LambdaUpdateWrapper<AigcPromptTemplateDO>()
                .eq(AigcPromptTemplateDO::getId, id)
                .setSql("use_count = use_count + 1"));
    }

}
