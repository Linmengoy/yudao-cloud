package cn.iocoder.yudao.module.aigc.model.service.release;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.model.controller.admin.release.vo.AigcReleaseNoteSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcReleaseNoteDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcReleaseNoteMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(AigcReleaseNoteServiceImpl.class)
public class AigcReleaseNoteServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcReleaseNoteServiceImpl releaseNoteService;

    @Resource
    private AigcReleaseNoteMapper releaseNoteMapper;

    @Test
    public void testGetPublishedReleaseNotes_onlyReturnsPublishedRecords() {
        Long todayId = createReleaseNote("v1.2.0", LocalDate.now(), CommonStatusEnum.ENABLE.getStatus());
        Long historyId = createReleaseNote("v1.1.0", LocalDate.now().minusDays(1), CommonStatusEnum.ENABLE.getStatus());
        createReleaseNote("v1.3.0", LocalDate.now().plusDays(1), CommonStatusEnum.ENABLE.getStatus());
        createReleaseNote("v1.0.0", LocalDate.now(), CommonStatusEnum.DISABLE.getStatus());

        List<AigcReleaseNoteDO> notes = releaseNoteService.getPublishedReleaseNotes(10);

        assertEquals(List.of(todayId, historyId), notes.stream().map(AigcReleaseNoteDO::getId).toList());
        assertNotNull(notes.get(0).getPublishTime());
    }

    @Test
    public void testUpdateReleaseNoteStatus_unpublishClearsPublishTime() {
        Long id = createReleaseNote("v2.0.0", LocalDate.now(), CommonStatusEnum.ENABLE.getStatus());

        releaseNoteService.updateReleaseNoteStatus(id, CommonStatusEnum.DISABLE.getStatus());

        AigcReleaseNoteDO note = releaseNoteMapper.selectById(id);
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), note.getStatus());
        assertNull(note.getPublishTime());
    }

    private Long createReleaseNote(String version, LocalDate releaseDate, Integer status) {
        return releaseNoteService.createReleaseNote(new AigcReleaseNoteSaveReqVO()
                .setVersion(version)
                .setReleaseDate(releaseDate)
                .setTitle("Release " + version)
                .setSummary("Summary " + version)
                .setContent("Feature\nFix")
                .setStatus(status)
                .setPublisher("qa"));
    }

}
