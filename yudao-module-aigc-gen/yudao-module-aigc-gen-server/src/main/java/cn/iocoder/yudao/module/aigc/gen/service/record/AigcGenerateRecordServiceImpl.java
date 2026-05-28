package cn.iocoder.yudao.module.aigc.gen.service.record;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.asset.api.AigcAssetApi;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateRespDTO;
import cn.iocoder.yudao.module.aigc.billing.api.AigcBillingApi;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingConfirmReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeRespDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingReleaseReqDTO;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.callback.vo.AigcGenerateCallbackPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.providerlog.vo.AigcGenerateProviderLogPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.record.vo.AigcGenerateRecordPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateCallbackDO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateProviderLogDO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateRecordDO;
import cn.iocoder.yudao.module.aigc.gen.dal.mysql.AigcGenerateCallbackMapper;
import cn.iocoder.yudao.module.aigc.gen.dal.mysql.AigcGenerateProviderLogMapper;
import cn.iocoder.yudao.module.aigc.gen.dal.mysql.AigcGenerateRecordMapper;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.enums.AigcGenerateStatusEnum;
import cn.iocoder.yudao.module.aigc.gen.framework.client.AigcProviderClient;
import cn.iocoder.yudao.module.aigc.gen.framework.client.AigcProviderClientFactory;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitRespDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.security.AigcGenerateFileSecurityUtils;
import cn.iocoder.yudao.module.aigc.model.api.AigcModelApi;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelProviderRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelUsageRecordReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelValidateReqDTO;
import cn.iocoder.yudao.module.aigc.safety.api.AigcSafetyApi;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckRespDTO;
import cn.iocoder.yudao.module.aigc.task.api.AigcTaskApi;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCallbackCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.gen.enums.ErrorCodeConstants.GENERATE_PROMPT_NOT_PASS;
import static cn.iocoder.yudao.module.aigc.gen.enums.ErrorCodeConstants.GENERATE_PROVIDER_CALLBACK_INVALID;
import static cn.iocoder.yudao.module.aigc.gen.enums.ErrorCodeConstants.GENERATE_PROVIDER_RESULT_INVALID;
import static cn.iocoder.yudao.module.aigc.gen.enums.ErrorCodeConstants.GENERATE_RECORD_NOT_EXISTS;

@Service
@Validated
public class AigcGenerateRecordServiceImpl implements AigcGenerateRecordService {

    private static final Set<String> FILE_TYPES = Set.of("IMAGE", "VIDEO", "AUDIO", "DOCUMENT");
    private static final Set<String> WAITING_STATUSES = Set.of(AigcGenerateStatusEnum.SUBMITTED.getCode(), AigcGenerateStatusEnum.CALLBACK_WAITING.getCode(), AigcGenerateStatusEnum.SYNCING.getCode());

    @Resource
    private AigcGenerateRecordMapper generateRecordMapper;
    @Resource
    private AigcGenerateCallbackMapper callbackMapper;
    @Resource
    private AigcGenerateProviderLogMapper providerLogMapper;
    @Resource
    private AigcModelApi modelApi;
    @Resource
    private AigcBillingApi billingApi;
    @Resource
    private AigcTaskApi taskApi;
    @Resource
    private AigcAssetApi assetApi;
    @Resource
    private AigcSafetyApi safetyApi;
    @Resource
    private AigcProviderClientFactory providerClientFactory;
    @Resource
    private ObjectProvider<MeterRegistry> meterRegistryProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcGenerateRecordDO createGenerateRecord(AigcGenerateSubmitReqDTO reqDTO) {
        if (reqDTO.getClientRequestId() != null) {
            AigcGenerateRecordDO exists = generateRecordMapper.selectByClientRequestId(reqDTO.getUserId(), reqDTO.getClientRequestId());
            if (exists != null) {
                return exists;
            }
        }
        AigcGenerateRecordDO record = BeanUtils.toBean(reqDTO, AigcGenerateRecordDO.class)
                .setGenerateNo(generateGenerateNo())
                .setStatus(AigcGenerateStatusEnum.CREATED.getCode());
        generateRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcGenerateRecordDO submitGenerate(AigcGenerateSubmitReqDTO reqDTO) {
        AigcGenerateRecordDO exists = reqDTO.getClientRequestId() == null ? null : generateRecordMapper.selectByClientRequestId(reqDTO.getUserId(), reqDTO.getClientRequestId());
        if (exists != null) {
            return exists;
        }
        checkPrompt(reqDTO);
        recordMetric("aigc_gen_submit_total");
        AigcModelRespDTO model = modelApi.validateModel(reqDTO.getModelId(), reqDTO.getGenerateMode()).getCheckedData();
        AigcModelProviderRespDTO provider = model.getProviderId() == null ? null : modelApi.getProvider(model.getProviderId()).getCheckedData();
        modelApi.validateParams(new AigcModelValidateReqDTO().setModelId(reqDTO.getModelId()).setCapability(reqDTO.getGenerateMode()).setParams(Map.of())).getCheckedData();
        AigcModelPriceCalculateRespDTO price = modelApi.calculatePrice(new AigcModelPriceCalculateReqDTO()
                .setModelId(reqDTO.getModelId()).setCapability(reqDTO.getGenerateMode()).setTaskType(reqDTO.getGenerateType()).setParams(Map.of())).getCheckedData();
        AigcBillingFreezeRespDTO freeze = billingApi.freeze(new AigcBillingFreezeReqDTO()
                .setUserId(reqDTO.getUserId()).setBizType("AIGC_GENERATE").setBizId(reqDTO.getClientRequestId() == null ? generateGenerateNo() : reqDTO.getClientRequestId())
                .setAmount(price.getSalePrice()).setTitle(reqDTO.getGenerateType() + "生成冻结").setPriceSnapshot(price.toString())).getCheckedData();
        Long taskId = taskApi.createTask(new AigcTaskCreateReqDTO()
                .setClientRequestId(reqDTO.getClientRequestId()).setUserId(reqDTO.getUserId()).setTaskType(reqDTO.getGenerateType())
                .setCapability(reqDTO.getGenerateMode()).setModelId(reqDTO.getModelId()).setProviderId(model.getProviderId()).setRequestParams(reqDTO.getInputParams())
                .setPriceSnapshot(price.toString()).setFreezeId(freeze.getId()).setSalePrice(price.getSalePrice()).setCostPrice(price.getCostPrice()).setCurrencyType(price.getCurrencyType())).getCheckedData();
        AigcGenerateRecordDO record = BeanUtils.toBean(reqDTO, AigcGenerateRecordDO.class)
                .setGenerateNo(generateGenerateNo()).setTaskId(taskId).setModelCode(model.getCode()).setProviderId(model.getProviderId())
                .setProviderCode(resolveProviderCode(provider)).setFreezeId(freeze.getId())
                .setPriceAmount(price.getSalePrice()).setCostAmount(price.getCostPrice()).setStatus(AigcGenerateStatusEnum.SUBMITTING.getCode()).setSubmitTime(LocalDateTime.now());
        generateRecordMapper.insert(record);
        taskApi.markRunning(taskId).getCheckedData();
        AigcProviderSubmitRespDTO providerResp = submitProvider(record, reqDTO, provider);
        if (!Boolean.TRUE.equals(providerResp.getSuccess())) {
            recordMetric("aigc_gen_submit_failed_total");
            failRecord(record, providerResp.getErrorCode(), providerResp.getErrorMessage());
            return generateRecordMapper.selectById(record.getId());
        }
        recordMetric("aigc_gen_submit_success_total");
        AigcGenerateRecordDO updateObj = new AigcGenerateRecordDO().setId(record.getId()).setProviderTaskId(providerResp.getProviderTaskId()).setProviderStatus(providerResp.getProviderStatus());
        if (Boolean.TRUE.equals(providerResp.getFinished())) {
            updateObj.setStatus(AigcGenerateStatusEnum.SUCCESS.getCode()).setOutputText(providerResp.getOutputText()).setOutputData(providerResp.getOutputData()).setOutputUrls(providerResp.getOutputUrls()).setFinishTime(LocalDateTime.now());
            generateRecordMapper.updateById(updateObj);
            finishSuccess(generateRecordMapper.selectById(record.getId()), providerResp);
        } else {
            updateObj.setStatus(AigcGenerateStatusEnum.CALLBACK_WAITING.getCode());
            generateRecordMapper.updateById(updateObj);
            taskApi.markCallbackWaiting(new AigcTaskStatusUpdateReqDTO().setTaskId(taskId).setExternalTaskId(providerResp.getProviderTaskId()).setProgress(30)).getCheckedData();
        }
        return generateRecordMapper.selectById(record.getId());
    }

    @Override
    public AigcGenerateRecordDO getGenerateRecord(Long id) {
        return generateRecordMapper.selectById(id);
    }

    @Override
    public AigcGenerateRecordDO getGenerateRecordByTaskId(Long taskId) {
        return generateRecordMapper.selectByTaskId(taskId);
    }

    @Override
    public AigcGenerateRecordDO validateGenerateRecordExists(Long id) {
        AigcGenerateRecordDO record = generateRecordMapper.selectById(id);
        if (record == null) {
            throw exception(GENERATE_RECORD_NOT_EXISTS);
        }
        return record;
    }

    @Override
    public PageResult<AigcGenerateRecordDO> getGenerateRecordPage(AigcGenerateRecordPageReqVO reqVO) {
        return generateRecordMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<AigcGenerateCallbackDO> getCallbackPage(AigcGenerateCallbackPageReqVO reqVO) {
        return callbackMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<AigcGenerateProviderLogDO> getProviderLogPage(AigcGenerateProviderLogPageReqVO reqVO) {
        return providerLogMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCallback(AigcGenerateCallbackReqDTO reqDTO) {
        if (reqDTO.getCallbackNo() != null && callbackMapper.selectByCallbackNo(reqDTO.getProviderCode(), reqDTO.getCallbackNo()) != null) {
            return;
        }
        recordMetric("aigc_gen_callback_total");
        AigcGenerateRecordDO record = generateRecordMapper.selectByProviderTask(reqDTO.getProviderCode(), reqDTO.getProviderTaskId());
        AigcProviderClient client = providerClientFactory.getClient(reqDTO.getProviderCode());
        boolean signatureValid = client.verifyCallback(reqDTO);
        AigcGenerateCallbackDO callback = BeanUtils.toBean(reqDTO, AigcGenerateCallbackDO.class)
                .setRecordId(record == null ? null : record.getId()).setTaskId(record == null ? null : record.getTaskId())
                .setSignatureValid(signatureValid).setProcessStatus(signatureValid ? AigcGenerateStatusEnum.SUCCESS.getCode() : AigcGenerateStatusEnum.FAILED.getCode())
                .setProcessTime(LocalDateTime.now());
        callbackMapper.insert(callback);
        if (!signatureValid) {
            recordMetric("aigc_gen_callback_invalid_total");
            throw exception(GENERATE_PROVIDER_CALLBACK_INVALID);
        }
        if (record == null) {
            return;
        }
        taskApi.createCallbackRecord(new AigcTaskCallbackCreateReqDTO().setTaskId(record.getTaskId()).setProviderId(record.getProviderId()).setProviderCode(reqDTO.getProviderCode())
                .setExternalTaskId(reqDTO.getProviderTaskId()).setCallbackType(reqDTO.getCallbackType() == null ? "GEN_CALLBACK" : reqDTO.getCallbackType()).setRawBody(reqDTO.getRawBody()).setSignature(reqDTO.getSignature())).getCheckedData();
        if (AigcGenerateStatusEnum.SUCCESS.getCode().equals(reqDTO.getResultStatus())) {
            AigcProviderSubmitRespDTO resp = new AigcProviderSubmitRespDTO().setProviderTaskId(reqDTO.getProviderTaskId()).setProviderStatus(reqDTO.getResultStatus())
                    .setOutputText(reqDTO.getOutputText()).setOutputData(reqDTO.getOutputData()).setOutputUrls(reqDTO.getOutputUrls()).setSuccess(true).setFinished(true);
            generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId()).setStatus(AigcGenerateStatusEnum.SUCCESS.getCode()).setOutputText(reqDTO.getOutputText())
                    .setOutputData(reqDTO.getOutputData()).setOutputUrls(reqDTO.getOutputUrls()).setCallbackTime(LocalDateTime.now()).setFinishTime(LocalDateTime.now()));
            finishSuccess(generateRecordMapper.selectById(record.getId()), resp);
        } else if (AigcGenerateStatusEnum.FAILED.getCode().equals(reqDTO.getResultStatus())) {
            failRecord(record, "PROVIDER_FAILED", reqDTO.getFailReason());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTask(Long taskId) {
        AigcGenerateRecordDO record = generateRecordMapper.selectByTaskId(taskId);
        if (record == null) {
            throw exception(GENERATE_RECORD_NOT_EXISTS);
        }
        AigcProviderSubmitRespDTO resp = providerClientFactory.getClient(record.getProviderCode()).query(record.getProviderTaskId());
        if (!Boolean.TRUE.equals(resp.getSuccess())) {
            failRecord(record, resp.getErrorCode(), resp.getErrorMessage());
            return;
        }
        if (Boolean.TRUE.equals(resp.getFinished())) {
            generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId()).setStatus(AigcGenerateStatusEnum.SUCCESS.getCode())
                    .setProviderStatus(resp.getProviderStatus()).setOutputText(resp.getOutputText()).setOutputData(resp.getOutputData()).setOutputUrls(resp.getOutputUrls()).setFinishTime(LocalDateTime.now()));
            finishSuccess(generateRecordMapper.selectById(record.getId()), resp);
        } else {
            generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId()).setStatus(AigcGenerateStatusEnum.CALLBACK_WAITING.getCode()).setProviderStatus(resp.getProviderStatus()));
        }
    }

    @Override
    public int syncTimeoutTasks() {
        List<AigcGenerateRecordDO> records = generateRecordMapper.selectTimeoutList(WAITING_STATUSES, LocalDateTime.now().minusMinutes(5));
        records.forEach(record -> syncTask(record.getTaskId()));
        if (!records.isEmpty()) {
            recordMetric("aigc_gen_timeout_total", records.size());
        }
        return records.size();
    }

    private void checkPrompt(AigcGenerateSubmitReqDTO reqDTO) {
        if (reqDTO.getPrompt() == null || reqDTO.getPrompt().isBlank()) {
            return;
        }
        AigcSafetyPromptCheckRespDTO result = safetyApi.checkPrompt(new AigcSafetyPromptCheckReqDTO()
                .setPrompt(reqDTO.getPrompt()).setScene("PROMPT").setModelId(reqDTO.getModelId()).setUserId(reqDTO.getUserId()).setBizId(reqDTO.getClientRequestId())).getCheckedData();
        if (!Boolean.TRUE.equals(result.getPass())) {
            throw exception(GENERATE_PROMPT_NOT_PASS);
        }
    }

    private AigcProviderSubmitRespDTO submitProvider(AigcGenerateRecordDO record, AigcGenerateSubmitReqDTO reqDTO, AigcModelProviderRespDTO provider) {
        AigcProviderSubmitReqDTO providerReq = new AigcProviderSubmitReqDTO().setRecordId(record.getId()).setTaskId(record.getTaskId()).setUserId(record.getUserId())
                .setModelId(record.getModelId()).setModelCode(record.getModelCode()).setProviderId(record.getProviderId()).setProviderCode(record.getProviderCode())
                .setProviderBaseUrl(provider == null ? null : provider.getApiBaseUrl()).setProviderApiKey(provider == null ? null : provider.getApiKey())
                .setProviderSecretKey(provider == null ? null : provider.getSecretKey()).setProviderExtraConfig(provider == null ? null : provider.getExtraConfig())
                .setProviderTimeoutSeconds(provider == null ? null : provider.getTimeoutSeconds())
                .setGenerateType(record.getGenerateType()).setGenerateMode(record.getGenerateMode()).setPrompt(record.getPrompt()).setInputParams(record.getInputParams()).setSync(reqDTO.getSync());
        long start = System.currentTimeMillis();
        AigcProviderSubmitRespDTO resp;
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            resp = providerClientFactory.getClient(record.getProviderCode()).submit(providerReq);
        } else {
            resp = Timer.builder("aigc_gen_provider_duration_ms")
                    .tag("provider", record.getProviderCode() == null ? "unknown" : record.getProviderCode())
                    .register(meterRegistry)
                    .record(() -> providerClientFactory.getClient(record.getProviderCode()).submit(providerReq));
        }
        providerLogMapper.insert(new AigcGenerateProviderLogDO().setRecordId(record.getId()).setTaskId(record.getTaskId()).setProviderCode(record.getProviderCode()).setModelCode(record.getModelCode())
                .setApiAction("submit").setRequestId(record.getGenerateNo()).setRequestSummary(maskPrompt(record.getPrompt())).setResponseSummary(resp.getProviderStatus())
                .setSuccess(Boolean.TRUE.equals(resp.getSuccess())).setErrorCode(resp.getErrorCode()).setErrorMessage(resp.getErrorMessage()).setDurationMs(System.currentTimeMillis() - start));
        return resp;
    }

    private void finishSuccess(AigcGenerateRecordDO record, AigcProviderSubmitRespDTO resp) {
        Long assetId = createAssetIfNecessary(record);
        taskApi.markSuccess(new AigcTaskStatusUpdateReqDTO().setTaskId(record.getTaskId()).setExternalTaskId(record.getProviderTaskId()).setOutputText(record.getOutputText())
                .setOutputData(record.getOutputData()).setOutputAssetId(assetId).setOutputAssetType(assetId == null ? null : record.getGenerateType()).setProgress(100)).getCheckedData();
        billingApi.confirmFreeze(new AigcBillingConfirmReqDTO().setFreezeId(record.getFreezeId()).setTaskId(record.getTaskId()).setActualAmount(record.getPriceAmount())
                .setModelId(record.getModelId()).setProviderId(record.getProviderId()).setPriceSnapshot(record.getInputParams())).getCheckedData();
        modelApi.recordUsage(new AigcModelUsageRecordReqDTO().setTaskId(record.getTaskId()).setUserId(record.getUserId()).setModelId(record.getModelId()).setProviderId(record.getProviderId())
                .setCapability(record.getGenerateMode()).setRequestId(record.getGenerateNo()).setExternalTaskId(record.getProviderTaskId()).setPromptTokens(resp.getPromptTokens())
                .setCompletionTokens(resp.getCompletionTokens()).setTotalTokens(resp.getTotalTokens()).setCostPrice(record.getCostAmount()).setSalePrice(record.getPriceAmount()).setCurrencyType("POINT")
                .setStatus(0).setDurationMillis(resp.getDurationMillis())).getCheckedData();
        recordMetric("aigc_gen_success_total");
    }

    private Long createAssetIfNecessary(AigcGenerateRecordDO record) {
        if (!FILE_TYPES.contains(record.getGenerateType()) || record.getOutputUrls() == null || record.getOutputUrls().isBlank()) {
            return null;
        }
        String url = firstUrl(record.getOutputUrls());
        if (!AigcGenerateFileSecurityUtils.isSafeRemoteUrl(url)) {
            throw exception(GENERATE_PROVIDER_RESULT_INVALID);
        }
        AigcAssetCreateReqDTO reqDTO = new AigcAssetCreateReqDTO().setUserId(record.getUserId()).setAssetType(record.getGenerateType()).setSourceType("GENERATE").setBizType("TASK")
                .setBizId(record.getGenerateNo()).setTaskId(record.getTaskId()).setModelId(record.getModelId()).setProviderId(record.getProviderId()).setTitle(record.getGenerateType() + "生成资产")
                .setOriginUrl(url).setPromptSnapshot(record.getPrompt()).setGenerateSnapshot(record.getInputParams()).setVisibility("PRIVATE").setAuditStatus("PENDING");
        AigcAssetCreateRespDTO asset = switch (record.getGenerateType()) {
            case "IMAGE" -> assetApi.createImageAsset(reqDTO).getCheckedData();
            case "VIDEO" -> assetApi.createVideoAsset(reqDTO).getCheckedData();
            case "AUDIO" -> assetApi.createAudioAsset(reqDTO).getCheckedData();
            case "DOCUMENT" -> assetApi.createDocumentAsset(reqDTO).getCheckedData();
            default -> assetApi.createAsset(reqDTO).getCheckedData();
        };
        generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId()).setAssetIds("[" + asset.getId() + "]"));
        return asset.getId();
    }

    private void failRecord(AigcGenerateRecordDO record, String failCode, String failReason) {
        generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId()).setStatus(AigcGenerateStatusEnum.FAILED.getCode()).setFailReason(failCode).setFailMessage(failReason).setFinishTime(LocalDateTime.now()));
        recordMetric("aigc_gen_failed_total");
        taskApi.markFailed(new AigcTaskStatusUpdateReqDTO().setTaskId(record.getTaskId()).setFailCode(failCode).setFailReason(failReason)).getCheckedData();
        if (record.getFreezeId() != null) {
            billingApi.releaseFreeze(new AigcBillingReleaseReqDTO().setFreezeId(record.getFreezeId()).setTaskId(record.getTaskId()).setReason(failReason)).getCheckedData();
            taskApi.markRefunded(record.getTaskId()).getCheckedData();
        }
    }

    private String generateGenerateNo() {
        return "GEN" + IdUtil.getSnowflakeNextIdStr();
    }

    private String resolveProviderCode(AigcModelProviderRespDTO provider) {
        if (provider != null && provider.getCode() != null) {
            return provider.getCode();
        }
        return "mock";
    }

    private void recordMetric(String name) {
        recordMetric(name, 1D);
    }

    private void recordMetric(String name, double amount) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry != null) {
            counter(meterRegistry, name).increment(amount);
        }
    }

    private Counter counter(MeterRegistry meterRegistry, String name) {
        return Counter.builder(name).register(meterRegistry);
    }

    private String maskPrompt(String prompt) {
        if (prompt == null) {
            return null;
        }
        return prompt.length() <= 16 ? prompt : prompt.substring(0, 16) + "***";
    }

    private String firstUrl(String outputUrls) {
        return outputUrls.replace("[", "").replace("]", "").replace("\"", "").split(",")[0].trim();
    }
}
