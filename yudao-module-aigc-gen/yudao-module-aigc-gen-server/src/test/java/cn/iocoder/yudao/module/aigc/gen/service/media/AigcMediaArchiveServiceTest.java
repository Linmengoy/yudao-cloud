package cn.iocoder.yudao.module.aigc.gen.service.media;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AigcMediaArchiveServiceTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AigcMediaArchiveService mediaArchiveService;

    @Mock
    private FileApi fileApi;

    @Test
    public void testArchiveInputParams_uploadsDataUrlAndRemovesRawPayload() {
        when(fileApi.createFile(any(byte[].class), any(), eq("aigc/input"), eq("image/png")))
                .thenReturn("https://oss.example.com/aigc/input/ref.png");
        String dataUrl = "data:image/png;base64,aW1hZ2UtYnl0ZXM=";

        String archived = mediaArchiveService.archiveInputParams("""
                {"referenceImages":["%s"],"inputImages":[{"dataUrl":"%s","fileName":"Image","mimeType":"image/png"}]}
                """.formatted(dataUrl, dataUrl));

        JSONObject json = JSONUtil.parseObj(archived);
        assertEquals("https://oss.example.com/aigc/input/ref.png", json.getJSONArray("referenceImages").getStr(0));
        JSONObject inputImage = json.getJSONArray("inputImages").getJSONObject(0);
        assertEquals("https://oss.example.com/aigc/input/ref.png", inputImage.getStr("url"));
        assertFalse(inputImage.containsKey("dataUrl"));
        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(fileApi, times(1)).createFile(contentCaptor.capture(), anyString(), eq("aigc/input"),
                eq("image/png"));
        assertArrayEquals("image-bytes".getBytes(StandardCharsets.UTF_8), contentCaptor.getValue());
    }

    @Test
    public void testArchiveOutputData_uploadsNestedMediaFields() {
        when(fileApi.createFile(any(byte[].class), any(), eq("aigc/output"), eq("image/jpeg")))
                .thenReturn("https://oss.example.com/aigc/output/result.jpg");

        String archived = mediaArchiveService.archiveOutputData("""
                {"data":[{"imageUrl":"data:image/jpeg;base64,cmVzdWx0LWJ5dGVz"}]}
                """);

        JSONObject json = JSONUtil.parseObj(archived);
        assertEquals("https://oss.example.com/aigc/output/result.jpg",
                json.getJSONArray("data").getJSONObject(0).getStr("imageUrl"));
        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(fileApi, times(1)).createFile(contentCaptor.capture(), anyString(), eq("aigc/output"),
                eq("image/jpeg"));
        assertArrayEquals("result-bytes".getBytes(StandardCharsets.UTF_8), contentCaptor.getValue());
    }

}
