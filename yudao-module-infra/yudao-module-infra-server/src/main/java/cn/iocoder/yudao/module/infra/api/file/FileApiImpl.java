package cn.iocoder.yudao.module.infra.api.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.infra.api.file.dto.FileCreateReqDTO;
import cn.iocoder.yudao.module.infra.api.file.dto.FileCreateRespDTO;
import cn.iocoder.yudao.module.infra.api.file.dto.FilePresignReqDTO;
import cn.iocoder.yudao.module.infra.api.file.dto.FilePresignRespDTO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class FileApiImpl implements FileApi {

    @Resource
    private FileService fileService;

    @Override
    public CommonResult<String> createFile(FileCreateReqDTO createReqDTO) {
        return success(fileService.createFile(createReqDTO.getContent(), createReqDTO.getName(),
                createReqDTO.getDirectory(), createReqDTO.getType()));
    }

    @Override
    public CommonResult<FileCreateRespDTO> createFileV2(FileCreateReqDTO createReqDTO) {
        return success(fileService.createFileV2(createReqDTO.getContent(), createReqDTO.getName(),
                createReqDTO.getDirectory(), createReqDTO.getType()));
    }

    @Override
    public CommonResult<String> presignGetUrl(String url, Integer expirationSeconds) {
        return success(fileService.presignGetUrl(url, expirationSeconds));
    }

    @Override
    public CommonResult<FilePresignRespDTO> presignGetUrlV2(Long configId, String path, Integer expirationSeconds) {
        return success(fileService.presignGetUrlV2(configId, path, expirationSeconds));
    }

    @Override
    public CommonResult<List<FilePresignRespDTO>> presignGetUrlListV2(List<FilePresignReqDTO> reqDTOs) {
        return success(fileService.presignGetUrlListV2(reqDTOs));
    }

    @Override
    public CommonResult<byte[]> getFileContent(Long configId, String path) {
        try {
            return success(fileService.getFileContent(configId, path));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
