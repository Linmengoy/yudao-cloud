package cn.iocoder.yudao.module.aigc.asset.service.asset;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetFileDO;
import cn.iocoder.yudao.module.aigc.asset.dal.mysql.AigcAssetFileMapper;
import cn.iocoder.yudao.module.aigc.asset.dal.mysql.AigcAssetMapper;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetFileRoleEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetTypeEnum;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.dto.FileCreateRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AigcAssetServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AigcAssetServiceImpl assetService;

    @Mock
    private AigcAssetMapper assetMapper;
    @Mock
    private AigcAssetFileMapper assetFileMapper;
    @Mock
    private FileApi fileApi;

    @Test
    public void testUploadAsset_createThumbnailForImage() throws Exception {
        when(assetMapper.insert(any(AigcAssetDO.class))).thenAnswer(invocation -> {
            invocation.<AigcAssetDO>getArgument(0).setId(100L);
            return 1;
        });
        when(fileApi.createFileV2(any(byte[].class), anyString(), anyString(), anyString()))
                .thenReturn(new FileCreateRespDTO()
                        .setId(10L)
                        .setName("origin.png")
                        .setPath("aigc/asset/origin.png")
                        .setType("image/png")
                        .setSize(2048L)
                        .setPublicAccess(true)
                        .setUrl("https://cdn.example.com/origin.png"))
                .thenReturn(new FileCreateRespDTO()
                        .setId(11L)
                        .setName("origin-thumbnail.jpg")
                        .setPath("aigc/asset/thumbnail/origin-thumbnail.jpg")
                        .setType("image/jpeg")
                        .setSize(512L)
                        .setPublicAccess(true)
                        .setUrl("https://cdn.example.com/origin-thumbnail.jpg"));

        Long assetId = assetService.uploadAsset(1L, AigcAssetTypeEnum.IMAGE.getCode(), "origin", "origin.png",
                "image/png", createPngBytes(1200, 800));

        assertEquals(100L, assetId);
        ArgumentCaptor<AigcAssetFileDO> fileCaptor = ArgumentCaptor.forClass(AigcAssetFileDO.class);
        verify(assetFileMapper, org.mockito.Mockito.times(2)).insert(fileCaptor.capture());
        List<AigcAssetFileDO> files = fileCaptor.getAllValues();
        assertEquals(AigcAssetFileRoleEnum.ORIGINAL.getCode(), files.get(0).getFileRole());
        assertEquals(AigcAssetFileRoleEnum.THUMBNAIL.getCode(), files.get(1).getFileRole());
        assertEquals("image/jpeg", files.get(1).getMimeType());
        assertTrue(files.get(1).getWidth() <= 512);
        assertTrue(files.get(1).getHeight() <= 512);
    }

    private byte[] createPngBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

}
