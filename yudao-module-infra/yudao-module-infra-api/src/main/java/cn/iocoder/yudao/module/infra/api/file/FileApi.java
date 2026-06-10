package cn.iocoder.yudao.module.infra.api.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.infra.api.file.dto.FileCreateReqDTO;
import cn.iocoder.yudao.module.infra.api.file.dto.FileCreateRespDTO;
import cn.iocoder.yudao.module.infra.api.file.dto.FilePresignReqDTO;
import cn.iocoder.yudao.module.infra.api.file.dto.FilePresignPutRespDTO;
import cn.iocoder.yudao.module.infra.api.file.dto.FilePresignRespDTO;
import cn.iocoder.yudao.module.infra.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = ApiConstants.NAME) // TODO 芋艿：fallbackFactory =
@Tag(name = "RPC 服务 - 文件")
public interface FileApi {

    String PREFIX = ApiConstants.PREFIX + "/file";

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content 文件内容
     * @return 文件路径
     */
    default String createFile(byte[] content) {
        return createFile(content, null, null, null);
    }

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content 文件内容
     * @param name 文件名称，允许空
     * @return 文件路径
     */
    default String createFile(byte[] content, String name) {
        return createFile(content, name, null, null);
    }

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content 文件内容
     * @param name 文件名称，允许空
     * @param directory 目录，允许空
     * @param type 文件的 MIME 类型，允许空
     * @return 文件路径
     */
    default String createFile(@NotEmpty(message = "文件内容不能为空") byte[] content,
                              String name, String directory, String type) {
        return createFile(new FileCreateReqDTO().setName(name).setDirectory(directory).setType(type).setContent(content)).getCheckedData();
    }

    @PostMapping(PREFIX + "/create")
    @Operation(summary = "保存文件，并返回文件的访问路径")
    CommonResult<String> createFile(@Valid @RequestBody FileCreateReqDTO createReqDTO);

    default FileCreateRespDTO createFileV2(@NotEmpty(message = "文件内容不能为空") byte[] content,
                                           String name, String directory, String type) {
        return createFileV2(new FileCreateReqDTO().setName(name).setDirectory(directory).setType(type).setContent(content)).getCheckedData();
    }

    @PostMapping(PREFIX + "/create-v2")
    @Operation(summary = "保存文件，并返回文件信息")
    CommonResult<FileCreateRespDTO> createFileV2(@Valid @RequestBody FileCreateReqDTO createReqDTO);

    @PostMapping(PREFIX + "/create-record-v2")
    @Operation(summary = "创建文件记录，并返回文件信息")
    CommonResult<FileCreateRespDTO> createFileRecordV2(@Valid @RequestBody FileCreateRespDTO createRespDTO);

    @GetMapping(PREFIX + "/presigned-put-url-v2")
    @Operation(summary = "生成文件预签名地址，用于上传")
    CommonResult<FilePresignPutRespDTO> presignPutUrlV2(
            @NotEmpty(message = "文件名不能为空") @RequestParam("name") String name,
            @RequestParam(value = "directory", required = false) String directory);

    /**
     * 生成文件预签名地址，用于读取
     *
     * @param url 完整的文件访问地址
     * @param expirationSeconds 访问有效期，单位秒
     * @return 文件预签名地址
     */
    @GetMapping(PREFIX + "/presigned-url")
    @Operation(summary = "生成文件预签名地址，用于读取")
    CommonResult<String> presignGetUrl(@NotEmpty(message = "URL 不能为空") @RequestParam("url") String url,
                                       Integer expirationSeconds);

    @GetMapping(PREFIX + "/presigned-url-v2")
    @Operation(summary = "生成文件预签名地址，用于读取")
    CommonResult<FilePresignRespDTO> presignGetUrlV2(@RequestParam("configId") Long configId,
                                                     @NotEmpty(message = "文件路径不能为空") @RequestParam("path") String path,
                                                     @RequestParam(value = "expirationSeconds", required = false) Integer expirationSeconds);

    @PostMapping(PREFIX + "/presigned-url-v2-batch")
    @Operation(summary = "批量生成文件预签名地址，用于读取")
    CommonResult<List<FilePresignRespDTO>> presignGetUrlListV2(@Valid @RequestBody List<FilePresignReqDTO> reqDTOs);

    @GetMapping(PREFIX + "/content")
    @Operation(summary = "读取文件内容")
    CommonResult<byte[]> getFileContent(@RequestParam("configId") Long configId,
                                        @NotEmpty(message = "文件路径不能为空") @RequestParam("path") String path);

}
