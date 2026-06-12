package cn.iocoder.yudao.module.aigc.gen.service.record;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.asset.api.AigcAssetApi;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateRespDTO;
import cn.iocoder.yudao.module.aigc.billing.api.AigcBillingApi;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingConfirmReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeRespDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingReleaseReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcCostRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.callback.vo.AigcGenerateCallbackPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.providerlog.vo.AigcGenerateProviderLogPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.record.vo.AigcGenerateRecordPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateAttemptDO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateCallbackDO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateProviderLogDO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateRecordDO;
import cn.iocoder.yudao.module.aigc.gen.dal.mysql.AigcGenerateAttemptMapper;
import cn.iocoder.yudao.module.aigc.gen.dal.mysql.AigcGenerateCallbackMapper;
import cn.iocoder.yudao.module.aigc.gen.dal.mysql.AigcGenerateProviderLogMapper;
import cn.iocoder.yudao.module.aigc.gen.dal.mysql.AigcGenerateRecordMapper;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.enums.AigcGenerateAttemptStatusEnum;
import cn.iocoder.yudao.module.aigc.gen.enums.AigcGenerateMetricEnum;
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
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelSubmitCandidateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelSubmitCandidateRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelSubmitPrepareRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelUsageRecordReqDTO;
import cn.iocoder.yudao.module.aigc.safety.api.AigcSafetyApi;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckRespDTO;
import cn.iocoder.yudao.module.aigc.task.api.AigcTaskApi;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCallbackCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskDurationStatisticsReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskDurationStatisticsRespDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.aigc.gen.enums.ErrorCodeConstants.GENERATE_PROMPT_NOT_PASS;
import static cn.iocoder.yudao.module.aigc.gen.enums.ErrorCodeConstants.GENERATE_PROVIDER_CALLBACK_INVALID;
import static cn.iocoder.yudao.module.aigc.gen.enums.ErrorCodeConstants.GENERATE_PROVIDER_RESULT_INVALID;
import static cn.iocoder.yudao.module.aigc.gen.enums.ErrorCodeConstants.GENERATE_RECORD_NO_DUPLICATE;
import static cn.iocoder.yudao.module.aigc.gen.enums.ErrorCodeConstants.GENERATE_RECORD_NOT_EXISTS;

@Service
@Validated
@Slf4j
public class AigcGenerateRecordServiceImpl implements AigcGenerateRecordService {

    private static final Set<String> FILE_TYPES = Set.of("IMAGE", "VIDEO", "AUDIO", "DOCUMENT");
    private static final int MAX_ATTEMPT_COUNT = 6;
    private static final int MAX_HEDGING_CANDIDATES = 3;
    private static final String STRATEGY_PRIMARY = "PRIMARY";
    private static final String STRATEGY_CHANNEL_RETRY = "CHANNEL_RETRY";
    private static final String STRATEGY_PROVIDER_FALLBACK = "PROVIDER_FALLBACK";
    private static final String STRATEGY_HEDGING = "HEDGING";
    private static final Set<String> WAITING_STATUSES = Set.of(AigcGenerateStatusEnum.SUBMITTING.getCode(),
            AigcGenerateStatusEnum.SUBMITTED.getCode(),
            AigcGenerateStatusEnum.CALLBACK_WAITING.getCode(), AigcGenerateStatusEnum.RETRYING.getCode(),
            AigcGenerateStatusEnum.FALLBACKING.getCode(),
            AigcGenerateStatusEnum.HEDGING.getCode(), AigcGenerateStatusEnum.SYNCING.getCode());
    private static final Set<String> WINNABLE_STATUSES = Set.of(AigcGenerateStatusEnum.SUBMITTING.getCode(),
            AigcGenerateStatusEnum.SUBMITTED.getCode(),
            AigcGenerateStatusEnum.CALLBACK_WAITING.getCode(), AigcGenerateStatusEnum.RETRYING.getCode(),
            AigcGenerateStatusEnum.FALLBACKING.getCode(),
            AigcGenerateStatusEnum.HEDGING.getCode(), AigcGenerateStatusEnum.SYNCING.getCode());
    private static final Set<String> TERMINAL_RECORD_STATUSES = Set.of(AigcGenerateStatusEnum.SUCCESS.getCode(),
            AigcGenerateStatusEnum.FAILED.getCode(),
            AigcGenerateStatusEnum.CANCELLED.getCode());

    @Resource
    private AigcGenerateRecordMapper generateRecordMapper;
    @Resource
    private AigcGenerateAttemptMapper attemptMapper;
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
    private FileApi fileApi;
    @Resource
    private AigcSafetyApi safetyApi;
    @Resource
    private AigcProviderClientFactory providerClientFactory;
    @Resource
    private ObjectProvider<MeterRegistry> meterRegistryProvider;
    @Resource
    private ObjectProvider<TaskExecutor> taskExecutorProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcGenerateRecordDO createGenerateRecord(AigcGenerateSubmitReqDTO reqDTO) {
        if (reqDTO.getClientRequestId() != null) {
            AigcGenerateRecordDO exists = generateRecordMapper.selectByClientRequestId(reqDTO.getUserId(),
                    reqDTO.getClientRequestId());
            if (exists != null) {
                return exists;
            }
        }
        AigcGenerateRecordDO record = BeanUtils.toBean(reqDTO, AigcGenerateRecordDO.class)
                .setInputParams(sanitizeInputParamsSnapshot(reqDTO.getInputParams()))
                .setGenerateNo(generateGenerateNo())
                .setStatus(AigcGenerateStatusEnum.CREATED.getCode());
        return insertRecordHandlingDuplicate(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcGenerateRecordDO submitGenerate(AigcGenerateSubmitReqDTO reqDTO) {

        // 判断是否重复提交
        String clientRequestId = reqDTO.getClientRequestId();
        AigcGenerateRecordDO exists = null;
        if (clientRequestId != null) {
            exists = generateRecordMapper.selectByClientRequestId(reqDTO.getUserId(), clientRequestId);
        }
        if (exists != null) {
            return exists;
        }

        // 提示词审核
        checkPrompt(reqDTO);
        // 服务端统计埋点
        recordMetric(AigcGenerateMetricEnum.GEN_SUBMIT_TOTAL);

        Map<String, Object> inputParams = parseInputParams(reqDTO.getInputParams());
        // 关联的图片需要看看传的什么（尽量传资源ID，由我方获取ID进行上传）
        String inputParamsSnapshot = sanitizeInputParamsSnapshot(reqDTO.getInputParams());
        String generateNo = generateGenerateNo();
        AigcModelSubmitPrepareRespDTO prepare = modelApi.prepareSubmit(new AigcModelPriceCalculateReqDTO()
                .setModelId(reqDTO.getModelId()).setCapability(reqDTO.getGenerateMode())
                .setTaskType(reqDTO.getGenerateType()).setParams(inputParams)).getCheckedData();
        AigcModelRespDTO model = prepare.getModel();
        AigcModelProviderRespDTO provider = prepare.getProvider();
        AigcModelPriceCalculateRespDTO price = prepare.getPrice();
        AigcBillingFreezeRespDTO freeze = billingApi.freeze(new AigcBillingFreezeReqDTO()
                .setUserId(reqDTO.getUserId()).setBizType("AIGC_GENERATE")
                .setBizId(resolveBillingBizId(reqDTO, generateNo))
                .setAmount(price.getSalePrice()).setTitle(reqDTO.getGenerateType() + "生成冻结")
                .setPriceSnapshot(JsonUtils.toJsonString(price))).getCheckedData();
        Long taskId = null;
        AigcGenerateRecordDO record = null;
        AigcGenerateAttemptDO attempt = null;
        try {
            taskId = createTask(reqDTO, model, provider, price, freeze, inputParamsSnapshot);
            record = buildSubmittingRecord(reqDTO, inputParamsSnapshot, generateNo, taskId, model, provider, price,
                    freeze);
            AigcGenerateRecordDO inserted = insertRecordHandlingDuplicate(record);
            if (!Objects.equals(inserted.getId(), record.getId())) {
                return inserted;
            }
            attempt = createAttempt(record, model, provider, price, STRATEGY_PRIMARY, 1);
            taskApi.markQueued(taskId).getCheckedData();
            taskApi.markRunning(taskId).getCheckedData();
        } catch (Exception ex) {
            markTaskFailedQuietly(taskId, "SUBMIT_PREPARE_FAILED", ex.getMessage());
            if (releaseFreezeQuietly(freeze.getId(), taskId, ex.getMessage())) {
                markTaskRefundedQuietly(taskId);
            }
            throw ex;
        }
        if (!Boolean.TRUE.equals(reqDTO.getSync())) {
            submitProviderAfterCommit(record, reqDTO, attempt, provider);
            return generateRecordMapper.selectById(record.getId());
        }
        return processProviderSubmitSafely(record, reqDTO, attempt, provider);
    }

    private AigcGenerateRecordDO processProviderSubmit(AigcGenerateRecordDO record, AigcGenerateSubmitReqDTO reqDTO,
            AigcGenerateAttemptDO attempt, AigcModelProviderRespDTO provider) {
        AigcProviderSubmitRespDTO providerResp = submitProvider(record, reqDTO, attempt, provider);
        if (!Boolean.TRUE.equals(providerResp.getSuccess())) {
            recordMetric(AigcGenerateMetricEnum.SUBMIT_FAILED_TOTAL);
            handleAttemptFailure(record, reqDTO, attempt, providerResp.getErrorCode(), providerResp.getErrorMessage());
            return generateRecordMapper.selectById(record.getId());
        }
        recordMetric(AigcGenerateMetricEnum.SUBMIT_SUCCESS_TOTAL);
        AigcGenerateRecordDO freshRecord = generateRecordMapper.selectById(record.getId());
        AigcGenerateAttemptDO freshAttempt = attemptMapper.selectById(attempt.getId());
        if (freshRecord == null || freshAttempt == null) {
            return freshRecord;
        }
        if (TERMINAL_RECORD_STATUSES.contains(freshRecord.getStatus())
                || AigcGenerateAttemptStatusEnum.IGNORED.getCode().equals(freshAttempt.getStatus())) {
            recordAttemptCost(freshRecord, freshAttempt, BigDecimal.ZERO);
            return freshRecord;
        }
        AigcGenerateAttemptDO attemptUpdate = new AigcGenerateAttemptDO().setId(attempt.getId())
                .setProviderTaskId(providerResp.getProviderTaskId()).setProviderStatus(providerResp.getProviderStatus())
                .setResponseSummary(buildResponseSummary(providerResp));
        if (Boolean.TRUE.equals(providerResp.getFinished())) {
            attemptMapper.updateById(attemptUpdate.setStatus(AigcGenerateAttemptStatusEnum.SUCCESS.getCode())
                    .setFinishTime(LocalDateTime.now()));
            finishAttemptSuccess(freshRecord, attemptMapper.selectById(attempt.getId()), providerResp);
        } else {
            attemptMapper.updateById(attemptUpdate.setStatus(AigcGenerateAttemptStatusEnum.CALLBACK_WAITING.getCode()));
            generateRecordMapper.updateByIdAndStatuses(
                    new AigcGenerateRecordDO().setId(record.getId()).setProviderTaskId(providerResp.getProviderTaskId())
                            .setProviderStatus(providerResp.getProviderStatus())
                            .setStatus(AigcGenerateStatusEnum.CALLBACK_WAITING.getCode()),
                    WINNABLE_STATUSES);
            taskApi.markCallbackWaiting(new AigcTaskStatusUpdateReqDTO().setTaskId(record.getTaskId())
                    .setExternalTaskId(providerResp.getProviderTaskId()).setProgress(30)).getCheckedData();
        }
        return generateRecordMapper.selectById(record.getId());
    }

    private AigcGenerateRecordDO processProviderSubmitSafely(AigcGenerateRecordDO record,
            AigcGenerateSubmitReqDTO reqDTO,
            AigcGenerateAttemptDO attempt, AigcModelProviderRespDTO provider) {
        try {
            return processProviderSubmit(record, reqDTO, attempt, provider);
        } catch (Exception ex) {
            handleAttemptFailure(record, reqDTO, attempt, "SUBMIT_EXCEPTION", ex.getMessage());
            return generateRecordMapper.selectById(record.getId());
        }
    }

    private void handleAttemptFailure(AigcGenerateRecordDO record, AigcGenerateSubmitReqDTO reqDTO,
            AigcGenerateAttemptDO attempt,
            String failCode, String failReason) {
        AigcGenerateRecordDO freshRecord = generateRecordMapper.selectById(record.getId());
        AigcGenerateAttemptDO freshAttempt = attempt == null ? null : attemptMapper.selectById(attempt.getId());
        if (freshRecord == null || freshAttempt == null) {
            failRecord(record, failCode, failReason);
            return;
        }
        if (TERMINAL_RECORD_STATUSES.contains(freshRecord.getStatus())) {
            if (!AigcGenerateAttemptStatusEnum.IGNORED.getCode().equals(freshAttempt.getStatus())) {
                attemptMapper.updateById(new AigcGenerateAttemptDO().setId(freshAttempt.getId())
                        .setStatus(AigcGenerateAttemptStatusEnum.FAILED.getCode())
                        .setFailCode(failCode)
                        .setFailReason(StrUtil.maxLength(Objects.toString(failReason, ""), 512))
                        .setFinishTime(LocalDateTime.now()));
            }
            recordAttemptCost(freshRecord, freshAttempt, BigDecimal.ZERO);
            return;
        }
        if (AigcGenerateAttemptStatusEnum.IGNORED.getCode().equals(freshAttempt.getStatus())) {
            recordAttemptCost(freshRecord, freshAttempt, BigDecimal.ZERO);
            return;
        }
        markAttemptFailed(freshRecord, freshAttempt, failCode, failReason);
        if (STRATEGY_HEDGING.equals(freshAttempt.getStrategy())
                && !attemptMapper.selectActiveListByRecordId(freshRecord.getId()).isEmpty()) {
            return;
        }
        if (!isRetryableFailure(failCode, failReason)
                || attemptMapper.selectAttemptCount(freshRecord.getId()) >= MAX_ATTEMPT_COUNT) {
            failRecordIfNotTerminal(freshRecord, failCode, failReason);
            return;
        }
        if (STRATEGY_PRIMARY.equals(freshAttempt.getStrategy())
                && submitNextSingleAttempt(freshRecord, reqDTO, freshAttempt, STRATEGY_CHANNEL_RETRY)) {
            return;
        }
        if ((STRATEGY_PRIMARY.equals(freshAttempt.getStrategy())
                || STRATEGY_CHANNEL_RETRY.equals(freshAttempt.getStrategy()))
                && submitNextSingleAttempt(freshRecord, reqDTO, freshAttempt, STRATEGY_PROVIDER_FALLBACK)) {
            return;
        }
        if (submitHedgingAttempts(freshRecord, reqDTO)) {
            return;
        }
        failRecordIfNotTerminal(freshRecord, failCode, failReason);
    }

    private boolean submitNextSingleAttempt(AigcGenerateRecordDO record, AigcGenerateSubmitReqDTO reqDTO,
            AigcGenerateAttemptDO failedAttempt, String strategy) {
        List<AigcModelSubmitCandidateRespDTO.Candidate> candidates = prepareCandidates(record, reqDTO, failedAttempt,
                strategy, 1);
        if (candidates.isEmpty()) {
            return false;
        }
        AigcModelSubmitCandidateRespDTO.Candidate candidate = candidates.get(0);
        AigcGenerateAttemptDO nextAttempt = createAttempt(record, candidate.getModel(), candidate.getProvider(),
                candidate.getPrice(), strategy, nextBatchNo(record.getId()));
        processProviderSubmitSafely(generateRecordMapper.selectById(record.getId()), reqDTO, nextAttempt,
                candidate.getProvider());
        return true;
    }

    private boolean submitHedgingAttempts(AigcGenerateRecordDO record, AigcGenerateSubmitReqDTO reqDTO) {
        List<AigcModelSubmitCandidateRespDTO.Candidate> candidates = prepareCandidates(record, reqDTO, null,
                STRATEGY_HEDGING, MAX_HEDGING_CANDIDATES);
        if (candidates.isEmpty()) {
            return false;
        }
        int batchNo = nextBatchNo(record.getId());
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (AigcModelSubmitCandidateRespDTO.Candidate candidate : candidates) {
            if (attemptMapper.selectAttemptCount(record.getId()) >= MAX_ATTEMPT_COUNT) {
                break;
            }
            AigcGenerateAttemptDO hedgeAttempt = createAttempt(record, candidate.getModel(), candidate.getProvider(),
                    candidate.getPrice(), STRATEGY_HEDGING, batchNo);
            futures.add(CompletableFuture
                    .runAsync(() -> processProviderSubmitSafely(generateRecordMapper.selectById(record.getId()), reqDTO,
                            hedgeAttempt, candidate.getProvider()), resolveAsyncExecutor()));
        }
        return !futures.isEmpty();
    }

    private List<AigcModelSubmitCandidateRespDTO.Candidate> prepareCandidates(AigcGenerateRecordDO record,
            AigcGenerateSubmitReqDTO reqDTO,
            AigcGenerateAttemptDO failedAttempt, String strategy, int maxCandidates) {
        Set<Long> triedChannelIds = new HashSet<>();
        Set<Long> triedProviderIds = new HashSet<>();
        for (AigcGenerateAttemptDO attempt : attemptMapper.selectListByRecordId(record.getId())) {
            if (attempt.getChannelId() != null) {
                triedChannelIds.add(attempt.getChannelId());
            }
            if (attempt.getProviderId() != null) {
                triedProviderIds.add(attempt.getProviderId());
            }
        }
        Set<Long> excludedProviders = STRATEGY_PROVIDER_FALLBACK.equals(strategy) ? triedProviderIds
                : Collections.emptySet();
        Long preferredProviderId = STRATEGY_CHANNEL_RETRY.equals(strategy) && failedAttempt != null
                ? failedAttempt.getProviderId()
                : null;
        AigcModelSubmitCandidateRespDTO respDTO = modelApi.prepareSubmitCandidates(new AigcModelSubmitCandidateReqDTO()
                .setModelId(record.getModelId())
                .setCapability(record.getGenerateMode())
                .setTaskType(record.getGenerateType())
                .setParams(parseInputParams(reqDTO.getInputParams()))
                .setExcludeChannelIds(triedChannelIds)
                .setExcludeProviderIds(excludedProviders)
                .setPreferredProviderId(preferredProviderId)
                .setMaxCandidates(maxCandidates)
                .setStrategy(strategy)).getCheckedData();
        if (respDTO == null || respDTO.getCandidates() == null) {
            return List.of();
        }
        return respDTO.getCandidates();
    }

    private void markAttemptFailed(AigcGenerateRecordDO record, AigcGenerateAttemptDO attempt, String failCode,
            String failReason) {
        attemptMapper.updateById(new AigcGenerateAttemptDO().setId(attempt.getId())
                .setStatus(AigcGenerateAttemptStatusEnum.FAILED.getCode())
                .setWinner(false)
                .setFailCode(failCode)
                .setFailReason(StrUtil.maxLength(Objects.toString(failReason, ""), 512))
                .setFinishTime(LocalDateTime.now()));
        recordAttemptCost(record, attemptMapper.selectById(attempt.getId()), BigDecimal.ZERO);
        generateRecordMapper.updateById(
                new AigcGenerateRecordDO().setId(record.getId()).setCostAmount(sumAttemptCost(record.getId())));
    }

    private boolean isRetryableFailure(String failCode, String failReason) {
        String code = StrUtil.nullToEmpty(failCode).toUpperCase();
        String reason = StrUtil.nullToEmpty(failReason).toUpperCase();
        return !(code.contains("PARAM") || code.contains("SAFETY") || code.contains("UNSUPPORTED") || code.contains("NO_BALANCE")
                || code.contains("INSUFFICIENT") || code.contains("HTTP_400") || code.contains("HTTP_401") || code.contains("HTTP_403")
                || code.contains("FORBIDDEN") || code.contains("PROVIDER_TASK_ID_MISSING")
                || reason.contains("PARAM") || reason.contains("SAFETY") || reason.contains("UNSUPPORTED")
                || reason.contains("FORBIDDEN") || reason.contains("\"RETRYABLE\":FALSE") || reason.contains("\"RETRYABLE\": FALSE"));
    }

    private int nextBatchNo(Long recordId) {
        List<AigcGenerateAttemptDO> attempts = attemptMapper.selectListByRecordId(recordId);
        int max = 0;
        for (AigcGenerateAttemptDO attempt : attempts) {
            if (attempt.getBatchNo() != null && attempt.getBatchNo() > max) {
                max = attempt.getBatchNo();
            }
        }
        return max + 1;
    }

    private Long resolveEstimatedDurationMillis(AigcModelRespDTO model, AigcModelProviderRespDTO provider,
            String capability) {
        Long statisticsDurationMillis = resolveStatisticsDurationMillis(model, capability);
        if (statisticsDurationMillis != null && statisticsDurationMillis > 0) {
            return statisticsDurationMillis;
        }
        Integer timeoutSeconds = model.getTimeoutSeconds() != null ? model.getTimeoutSeconds()
                : (provider == null ? null : provider.getTimeoutSeconds());
        return timeoutSeconds == null || timeoutSeconds <= 0 ? null : timeoutSeconds * 1000L;
    }

    private Long resolveStatisticsDurationMillis(AigcModelRespDTO model, String capability) {
        if (model.getProviderId() == null || model.getId() == null || StrUtil.isBlank(capability)) {
            return null;
        }
        AigcTaskDurationStatisticsRespDTO statistics;
        try {
            statistics = taskApi.getSuccessDurationStatistics(new AigcTaskDurationStatisticsReqDTO()
                    .setProviderId(model.getProviderId())
                    .setModelId(model.getId())
                    .setCapability(capability)
                    .setSampleSize(50)).getCheckedData();
        } catch (Exception ex) {
            log.warn("获取任务耗时统计失败，使用模型超时时间兜底，modelId={}, providerId={}, capability={}",
                    model.getId(), model.getProviderId(), capability, ex);
            return null;
        }
        if (statistics == null || statistics.getSampleCount() == null || statistics.getSampleCount() <= 0) {
            return null;
        }
        return statistics.getAvgDurationMillis();
    }

    private Long createTask(AigcGenerateSubmitReqDTO reqDTO, AigcModelRespDTO model, AigcModelProviderRespDTO provider,
            AigcModelPriceCalculateRespDTO price, AigcBillingFreezeRespDTO freeze, String inputParamsSnapshot) {
        Long estimatedDurationMillis = resolveEstimatedDurationMillis(model, provider, reqDTO.getGenerateMode());
        return taskApi.createTask(new AigcTaskCreateReqDTO()
                .setClientRequestId(reqDTO.getClientRequestId()).setUserId(reqDTO.getUserId())
                .setTaskType(reqDTO.getGenerateType())
                .setCapability(reqDTO.getGenerateMode()).setModelId(model.getId()).setProviderId(model.getProviderId())
                .setChannelId(model.getChannelId()).setRequestParams(inputParamsSnapshot)
                .setEstimatedDurationMillis(estimatedDurationMillis).setPriceSnapshot(JsonUtils.toJsonString(price))
                .setFreezeId(freeze.getId())
                .setSalePrice(price.getSalePrice()).setCostPrice(price.getCostPrice())
                .setCurrencyType(price.getCurrencyType())).getCheckedData();
    }

    private AigcGenerateRecordDO buildSubmittingRecord(AigcGenerateSubmitReqDTO reqDTO, String inputParamsSnapshot,
            String generateNo, Long taskId,
            AigcModelRespDTO model, AigcModelProviderRespDTO provider,
            AigcModelPriceCalculateRespDTO price, AigcBillingFreezeRespDTO freeze) {
        return new AigcGenerateRecordDO()
                .setUserId(reqDTO.getUserId())
                .setClientRequestId(reqDTO.getClientRequestId())
                .setGenerateType(reqDTO.getGenerateType())
                .setGenerateMode(reqDTO.getGenerateMode())
                .setPrompt(reqDTO.getPrompt())
                .setInputParams(inputParamsSnapshot)
                .setModelId(model.getId())
                .setChannelId(model.getChannelId())
                .setProviderModel(model.getProviderModel())
                .setGenerateNo(generateNo)
                .setTaskId(taskId)
                .setModelCode(model.getCode())
                .setProviderId(model.getProviderId())
                .setProviderCode(resolveProviderCode(provider))
                .setFreezeId(freeze.getId())
                .setPriceAmount(price.getSalePrice())
                .setCostAmount(price.getCostPrice())
                .setStatus(AigcGenerateStatusEnum.SUBMITTING.getCode())
                .setSubmitTime(LocalDateTime.now());
    }

    private AigcGenerateAttemptDO createAttempt(AigcGenerateRecordDO record, AigcModelRespDTO model,
            AigcModelProviderRespDTO provider,
            AigcModelPriceCalculateRespDTO price, String strategy, Integer batchNo) {
        Long attemptCount = attemptMapper.selectAttemptCount(record.getId());
        AigcGenerateAttemptDO attempt = new AigcGenerateAttemptDO()
                .setRecordId(record.getId())
                .setTaskId(record.getTaskId())
                .setAttemptNo(attemptCount.intValue() + 1)
                .setBatchNo(batchNo == null ? 1 : batchNo)
                .setStrategy(strategy)
                .setModelId(model.getId())
                .setModelCode(model.getCode())
                .setChannelId(model.getChannelId())
                .setProviderModel(model.getProviderModel())
                .setProviderId(model.getProviderId())
                .setProviderCode(resolveProviderCode(provider))
                .setStatus(AigcGenerateAttemptStatusEnum.SUBMITTING.getCode())
                .setSaleAmount(price == null ? null : price.getSalePrice())
                .setCostAmount(price == null ? null : price.getCostPrice())
                .setCurrencyType(price == null ? null : price.getCurrencyType())
                .setBillingUnit(price == null ? null : price.getBillingUnit())
                .setPriceSnapshot(price == null ? null : JsonUtils.toJsonString(price))
                .setWinner(false)
                .setSubmitTime(LocalDateTime.now());
        attemptMapper.insert(attempt);
        generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId())
                .setModelId(model.getId())
                .setChannelId(model.getChannelId())
                .setProviderModel(model.getProviderModel())
                .setModelCode(model.getCode())
                .setProviderId(model.getProviderId())
                .setProviderCode(resolveProviderCode(provider))
                .setCostAmount(sumAttemptCost(record.getId()))
                .setStatus(resolveRecordStatusForStrategy(strategy)));
        return attempt;
    }

    private String resolveRecordStatusForStrategy(String strategy) {
        if (STRATEGY_CHANNEL_RETRY.equals(strategy)) {
            return AigcGenerateStatusEnum.RETRYING.getCode();
        }
        if (STRATEGY_PROVIDER_FALLBACK.equals(strategy)) {
            return AigcGenerateStatusEnum.FALLBACKING.getCode();
        }
        if (STRATEGY_HEDGING.equals(strategy)) {
            return AigcGenerateStatusEnum.HEDGING.getCode();
        }
        return AigcGenerateStatusEnum.SUBMITTING.getCode();
    }

    private String resolveBillingBizId(AigcGenerateSubmitReqDTO reqDTO, String generateNo) {
        return reqDTO.getClientRequestId() == null ? generateNo : reqDTO.getClientRequestId();
    }

    private AigcGenerateSubmitReqDTO buildRetrySubmitReq(AigcGenerateRecordDO record) {
        return new AigcGenerateSubmitReqDTO()
                .setUserId(record.getUserId())
                .setClientRequestId(record.getClientRequestId())
                .setGenerateType(record.getGenerateType())
                .setGenerateMode(record.getGenerateMode())
                .setModelId(record.getModelId())
                .setPrompt(record.getPrompt())
                .setInputParams(record.getInputParams())
                .setSync(false);
    }

    private AigcGenerateRecordDO insertRecordHandlingDuplicate(AigcGenerateRecordDO record) {
        try {
            generateRecordMapper.insert(record);
            return record;
        } catch (DuplicateKeyException ex) {
            AigcGenerateRecordDO exists = resolveDuplicateRecord(record);
            if (exists != null) {
                return exists;
            }
            throw exception(GENERATE_RECORD_NO_DUPLICATE);
        }
    }

    private AigcGenerateRecordDO resolveDuplicateRecord(AigcGenerateRecordDO record) {
        if (record.getClientRequestId() != null) {
            AigcGenerateRecordDO exists = generateRecordMapper.selectByClientRequestId(record.getUserId(),
                    record.getClientRequestId());
            if (exists != null) {
                return exists;
            }
        }
        if (record.getTaskId() != null) {
            AigcGenerateRecordDO exists = generateRecordMapper.selectByTaskId(record.getTaskId());
            if (exists != null) {
                return exists;
            }
        }
        if (record.getGenerateNo() != null) {
            return generateRecordMapper.selectByGenerateNo(record.getGenerateNo());
        }
        return null;
    }

    private boolean releaseFreezeQuietly(Long freezeId, Long taskId, String reason) {
        if (freezeId == null) {
            return false;
        }
        try {
            billingApi.releaseFreeze(new AigcBillingReleaseReqDTO().setFreezeId(freezeId).setTaskId(taskId)
                    .setReason(StrUtil.maxLength(Objects.toString(reason, ""), 512))).getCheckedData();
            return true;
        } catch (Exception releaseEx) {
            log.warn("[releaseFreezeQuietly][freezeId({}) taskId({}) release failed]", freezeId, taskId, releaseEx);
            return false;
        }
    }

    private void markTaskFailedQuietly(Long taskId, String failCode, String failReason) {
        if (taskId == null) {
            return;
        }
        try {
            taskApi.markFailed(new AigcTaskStatusUpdateReqDTO().setTaskId(taskId).setFailCode(failCode)
                    .setFailReason(StrUtil.maxLength(Objects.toString(failReason, ""), 512))).getCheckedData();
        } catch (Exception markEx) {
            log.warn("[markTaskFailedQuietly][taskId({}) mark failed]", taskId, markEx);
        }
    }

    private void markTaskRefundedQuietly(Long taskId) {
        if (taskId == null) {
            return;
        }
        try {
            taskApi.markRefunded(taskId).getCheckedData();
        } catch (Exception markEx) {
            log.warn("[markTaskRefundedQuietly][taskId({}) mark refunded failed]", taskId, markEx);
        }
    }

    private void submitProviderAfterCommit(AigcGenerateRecordDO record, AigcGenerateSubmitReqDTO reqDTO,
            AigcGenerateAttemptDO attempt, AigcModelProviderRespDTO provider) {
        Runnable task = () -> CompletableFuture.runAsync(() -> {
            try {
                processProviderSubmit(record, reqDTO, attempt, provider);
            } catch (Exception ex) {
                handleAttemptFailure(record, reqDTO, attempt, "SUBMIT_EXCEPTION", ex.getMessage());
            }
        }, resolveAsyncExecutor());
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private Executor resolveAsyncExecutor() {
        TaskExecutor taskExecutor = taskExecutorProvider.getIfUnique();
        return taskExecutor == null ? CompletableFuture.delayedExecutor(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                : taskExecutor::execute;
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseInputParams(String inputParams) {
        if (StrUtil.isBlank(inputParams)) {
            return Map.of();
        }
        if (!JsonUtils.isJsonObject(inputParams)) {
            throw invalidParamException("输入参数必须是 JSON 对象");
        }
        Map<String, Object> params = JsonUtils.parseObject(inputParams, Map.class);
        return params == null ? Map.of() : params;
    }

    private String sanitizeInputParamsSnapshot(String inputParams) {
        if (StrUtil.isBlank(inputParams)) {
            return inputParams;
        }
        if (!JSONUtil.isTypeJSONObject(inputParams)) {
            throw invalidParamException("输入参数必须是 JSON 对象");
        }
        JSONObject params = JSONUtil.parseObj(inputParams);
        JSONArray inputImages = params.getJSONArray("inputImages");
        if (inputImages == null || inputImages.isEmpty()) {
            return params.toString();
        }
        JSONArray sanitizedImages = new JSONArray();
        for (Object item : inputImages) {
            JSONObject image = JSONUtil.parseObj(item);
            image.remove("dataUrl");
            sanitizedImages.add(image);
        }
        params.set("inputImages", sanitizedImages);
        return params.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCallback(AigcGenerateCallbackReqDTO reqDTO) {
        if (reqDTO.getCallbackNo() != null
                && callbackMapper.selectByCallbackNo(reqDTO.getProviderCode(), reqDTO.getCallbackNo()) != null) {
            return;
        }
        recordMetric(AigcGenerateMetricEnum.CALLBACK_TOTAL);
        AigcGenerateAttemptDO attempt = attemptMapper.selectByProviderTask(reqDTO.getProviderCode(),
                reqDTO.getProviderTaskId());
        AigcGenerateRecordDO record = attempt == null
                ? generateRecordMapper.selectByProviderTask(reqDTO.getProviderCode(), reqDTO.getProviderTaskId())
                : generateRecordMapper.selectById(attempt.getRecordId());
        AigcProviderClient client = providerClientFactory.getClient(reqDTO.getProviderCode());
        boolean signatureValid = client.verifyCallback(reqDTO);
        AigcGenerateCallbackDO callback = BeanUtils.toBean(reqDTO, AigcGenerateCallbackDO.class)
                .setRecordId(record == null ? null : record.getId())
                .setAttemptId(attempt == null ? null : attempt.getId())
                .setTaskId(record == null ? null : record.getTaskId())
                .setSignatureValid(signatureValid)
                .setProcessStatus(signatureValid ? AigcGenerateStatusEnum.SUCCESS.getCode()
                        : AigcGenerateStatusEnum.FAILED.getCode())
                .setProcessTime(LocalDateTime.now());
        callbackMapper.insert(callback);
        if (!signatureValid) {
            recordMetric(AigcGenerateMetricEnum.CALLBACK_INVALID_TOTAL);
            throw exception(GENERATE_PROVIDER_CALLBACK_INVALID);
        }
        if (record == null) {
            return;
        }
        taskApi.createCallbackRecord(new AigcTaskCallbackCreateReqDTO().setTaskId(record.getTaskId())
                .setProviderId(attempt == null ? record.getProviderId() : attempt.getProviderId())
                .setProviderCode(reqDTO.getProviderCode())
                .setExternalTaskId(reqDTO.getProviderTaskId())
                .setCallbackType(reqDTO.getCallbackType() == null ? "GEN_CALLBACK" : reqDTO.getCallbackType())
                .setRawBody(reqDTO.getRawBody()).setSignature(reqDTO.getSignature())).getCheckedData();
        if (AigcGenerateStatusEnum.SUCCESS.getCode().equals(reqDTO.getResultStatus())) {
            AigcProviderSubmitRespDTO resp = new AigcProviderSubmitRespDTO()
                    .setProviderTaskId(reqDTO.getProviderTaskId()).setProviderStatus(reqDTO.getResultStatus())
                    .setOutputText(reqDTO.getOutputText()).setOutputData(reqDTO.getOutputData())
                    .setOutputUrls(reqDTO.getOutputUrls()).setSuccess(true).setFinished(true);
            if (attempt == null) {
                generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId())
                        .setStatus(AigcGenerateStatusEnum.ASSET_CREATING.getCode())
                        .setOutputText(reqDTO.getOutputText())
                        .setOutputData(reqDTO.getOutputData()).setOutputUrls(reqDTO.getOutputUrls())
                        .setCallbackTime(LocalDateTime.now()));
                finishSuccess(generateRecordMapper.selectById(record.getId()), resp);
                generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId())
                        .setStatus(AigcGenerateStatusEnum.SUCCESS.getCode()).setFinishTime(LocalDateTime.now()));
            } else {
                attemptMapper.updateById(new AigcGenerateAttemptDO().setId(attempt.getId())
                        .setStatus(AigcGenerateAttemptStatusEnum.SUCCESS.getCode())
                        .setProviderStatus(reqDTO.getResultStatus())
                        .setCallbackTime(LocalDateTime.now()).setFinishTime(LocalDateTime.now()));
                finishAttemptSuccess(record, attemptMapper.selectById(attempt.getId()), resp);
            }
        } else if (AigcGenerateStatusEnum.FAILED.getCode().equals(reqDTO.getResultStatus())) {
            if (attempt == null) {
                failRecord(record, "PROVIDER_FAILED", reqDTO.getFailReason());
            } else {
                handleAttemptFailure(record, buildRetrySubmitReq(record), attempt, "PROVIDER_FAILED",
                        reqDTO.getFailReason());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTask(Long taskId) {
        AigcGenerateRecordDO record = generateRecordMapper.selectByTaskId(taskId);
        if (record == null) {
            throw exception(GENERATE_RECORD_NOT_EXISTS);
        }
        List<AigcGenerateAttemptDO> activeAttempts = attemptMapper.selectActiveListByRecordId(record.getId());
        if (activeAttempts.isEmpty()) {
            if (StrUtil.isBlank(record.getProviderTaskId())) {
                failRecord(record, "PROVIDER_TASK_ID_MISSING", "Provider task id is missing; cannot query generation status");
                return;
            }
            AigcProviderSubmitReqDTO providerReq = buildProviderQueryReq(record);
            AigcProviderSubmitRespDTO resp = providerClientFactory.getClient(providerReq).query(providerReq);
            processSyncResponse(record, null, resp);
            return;
        }
        for (AigcGenerateAttemptDO attempt : activeAttempts) {
            if (StrUtil.isBlank(attempt.getProviderTaskId())) {
                handleAttemptFailure(record, buildRetrySubmitReq(record), attempt, "PROVIDER_TASK_ID_MISSING",
                        "Provider accepted the generation without returning a queryable task id");
                if (TERMINAL_RECORD_STATUSES.contains(generateRecordMapper.selectById(record.getId()).getStatus())) {
                    return;
                }
                continue;
            }
            AigcProviderSubmitReqDTO providerReq = buildProviderQueryReq(record, attempt);
            AigcProviderSubmitRespDTO resp = providerClientFactory.getClient(providerReq).query(providerReq);
            processSyncResponse(record, attempt, resp);
            if (TERMINAL_RECORD_STATUSES.contains(generateRecordMapper.selectById(record.getId()).getStatus())) {
                return;
            }
        }
    }

    private void processSyncResponse(AigcGenerateRecordDO record, AigcGenerateAttemptDO attempt,
            AigcProviderSubmitRespDTO resp) {
        if (!Boolean.TRUE.equals(resp.getSuccess())) {
            if (attempt == null) {
                failRecord(record, resp.getErrorCode(), resp.getErrorMessage());
            } else {
                handleAttemptFailure(record, buildRetrySubmitReq(record), attempt, resp.getErrorCode(),
                        resp.getErrorMessage());
            }
            return;
        }
        if (Boolean.TRUE.equals(resp.getFinished())) {
            if (attempt == null) {
                generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId())
                        .setStatus(AigcGenerateStatusEnum.ASSET_CREATING.getCode())
                        .setProviderStatus(resp.getProviderStatus()).setOutputText(resp.getOutputText())
                        .setOutputData(resp.getOutputData()).setOutputUrls(resp.getOutputUrls()));
                finishSuccess(generateRecordMapper.selectById(record.getId()), resp);
                generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId())
                        .setStatus(AigcGenerateStatusEnum.SUCCESS.getCode()).setFinishTime(LocalDateTime.now()));
            } else {
                attemptMapper.updateById(new AigcGenerateAttemptDO().setId(attempt.getId())
                        .setStatus(AigcGenerateAttemptStatusEnum.SUCCESS.getCode())
                        .setProviderStatus(resp.getProviderStatus()).setResponseSummary(buildResponseSummary(resp))
                        .setFinishTime(LocalDateTime.now()));
                finishAttemptSuccess(record, attemptMapper.selectById(attempt.getId()), resp);
            }
        } else {
            if (attempt != null) {
                attemptMapper.updateById(new AigcGenerateAttemptDO().setId(attempt.getId())
                        .setStatus(AigcGenerateAttemptStatusEnum.CALLBACK_WAITING.getCode())
                        .setProviderStatus(resp.getProviderStatus()));
            }
            generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId())
                    .setStatus(AigcGenerateStatusEnum.CALLBACK_WAITING.getCode())
                    .setProviderStatus(resp.getProviderStatus()));
        }
    }

    @Override
    public int syncTimeoutTasks() {
        List<AigcGenerateRecordDO> records = generateRecordMapper.selectTimeoutList(WAITING_STATUSES,
                LocalDateTime.now().minusMinutes(5));
        records.forEach(record -> syncTask(record.getTaskId()));
        if (!records.isEmpty()) {
            recordMetric(AigcGenerateMetricEnum.TIMEOUT_TOTAL, records.size());
        }
        return records.size();
    }

    private void checkPrompt(AigcGenerateSubmitReqDTO reqDTO) {
        if (reqDTO.getPrompt() == null || reqDTO.getPrompt().isBlank()) {
            return;
        }
        // 审核提示词是否违规
        AigcSafetyPromptCheckRespDTO result = safetyApi.checkPrompt(new AigcSafetyPromptCheckReqDTO()
                .setPrompt(reqDTO.getPrompt())
                .setScene("PROMPT")
                .setModelId(reqDTO.getModelId())
                .setUserId(reqDTO.getUserId())
                .setBizId(reqDTO.getClientRequestId()))
                .getCheckedData();
        if (!Boolean.TRUE.equals(result.getPass())) {
            throw exception(GENERATE_PROMPT_NOT_PASS);
        }
    }

    private AigcProviderSubmitRespDTO submitProvider(AigcGenerateRecordDO record, AigcGenerateSubmitReqDTO reqDTO,
            AigcGenerateAttemptDO attempt, AigcModelProviderRespDTO provider) {
        AigcModelRespDTO model = modelApi.getModel(attempt.getModelId()).getCheckedData();
        AigcProviderSubmitReqDTO providerReq = new AigcProviderSubmitReqDTO().setRecordId(record.getId())
                .setTaskId(record.getTaskId()).setUserId(record.getUserId())
                .setModelId(attempt.getModelId()).setModelCode(attempt.getModelCode())
                .setChannelId(attempt.getChannelId())
                .setProviderModel(resolveProviderModel(attempt, model))
                .setProviderId(attempt.getProviderId()).setProviderCode(attempt.getProviderCode())
                .setProviderBaseUrl(provider == null ? null : provider.getApiBaseUrl())
                .setProviderApiKey(provider == null ? null : provider.getApiKey())
                .setProviderSecretKey(provider == null ? null : provider.getSecretKey())
                .setProviderExtraConfig(provider == null ? null : provider.getExtraConfig())
                .setProviderTimeoutSeconds(provider == null ? null : provider.getTimeoutSeconds())
                .setProxyEnabled(provider == null ? null : provider.getProxyEnabled())
                .setProxyProtocol(provider == null ? null : provider.getProxyProtocol())
                .setProxyHost(provider == null ? null : provider.getProxyHost())
                .setProxyPort(provider == null ? null : provider.getProxyPort())
                .setProxyUsername(provider == null ? null : provider.getProxyUsername())
                .setProxyPassword(provider == null ? null : provider.getProxyPassword())
                .setGenerateType(record.getGenerateType()).setGenerateMode(record.getGenerateMode())
                .setPrompt(record.getPrompt()).setInputParams(reqDTO.getInputParams()).setSync(reqDTO.getSync());
        long start = System.currentTimeMillis();
        AigcProviderSubmitRespDTO resp;
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            resp = providerClientFactory.getClient(providerReq).submit(providerReq);
        } else {
            resp = Timer.builder(AigcGenerateMetricEnum.PROVIDER_DURATION_MS.getName())
                    .tag("provider", attempt.getProviderCode() == null ? "unknown" : attempt.getProviderCode())
                    .register(meterRegistry)
                    .record(() -> providerClientFactory.getClient(providerReq).submit(providerReq));
        }
        String requestSummary = buildRequestSummary(record, attempt);
        String responseSummary = buildResponseSummary(resp);
        attemptMapper.updateById(new AigcGenerateAttemptDO().setId(attempt.getId()).setRequestSummary(requestSummary).setResponseSummary(responseSummary));
        try {
            providerLogMapper.insert(new AigcGenerateProviderLogDO().setRecordId(record.getId()).setAttemptId(attempt.getId()).setTaskId(record.getTaskId())
                    .setProviderCode(attempt.getProviderCode()).setModelCode(attempt.getModelCode())
                    .setApiAction("submit").setRequestId(record.getGenerateNo() + "-" + attempt.getAttemptNo()).setRequestSummary(requestSummary).setResponseSummary(responseSummary)
                    .setSuccess(Boolean.TRUE.equals(resp.getSuccess())).setErrorCode(resp.getErrorCode()).setErrorMessage(resp.getErrorMessage()).setDurationMs(System.currentTimeMillis() - start));
        } catch (Exception ex) {
            log.warn("[submitProvider][recordId({}) attemptId({}) provider log failed]", record.getId(), attempt.getId(), ex);
        }
        return resp;
    }

    private AigcProviderSubmitReqDTO buildProviderQueryReq(AigcGenerateRecordDO record) {
        AigcModelRespDTO model = modelApi.getModel(record.getModelId()).getCheckedData();
        AigcModelProviderRespDTO provider = record.getProviderId() == null ? null
                : modelApi.getProvider(record.getProviderId()).getCheckedData();
        return new AigcProviderSubmitReqDTO()
                .setRecordId(record.getId())
                .setTaskId(record.getTaskId())
                .setUserId(record.getUserId())
                .setModelId(record.getModelId())
                .setModelCode(record.getModelCode())
                .setChannelId(record.getChannelId())
                .setProviderModel(resolveProviderModel(record, model))
                .setProviderId(record.getProviderId())
                .setProviderCode(record.getProviderCode())
                .setProviderTaskId(record.getProviderTaskId())
                .setProviderBaseUrl(provider == null ? null : provider.getApiBaseUrl())
                .setProviderApiKey(provider == null ? null : provider.getApiKey())
                .setProviderSecretKey(provider == null ? null : provider.getSecretKey())
                .setProviderExtraConfig(provider == null ? null : provider.getExtraConfig())
                .setProviderTimeoutSeconds(provider == null ? null : provider.getTimeoutSeconds())
                .setProxyEnabled(provider == null ? null : provider.getProxyEnabled())
                .setProxyProtocol(provider == null ? null : provider.getProxyProtocol())
                .setProxyHost(provider == null ? null : provider.getProxyHost())
                .setProxyPort(provider == null ? null : provider.getProxyPort())
                .setProxyUsername(provider == null ? null : provider.getProxyUsername())
                .setProxyPassword(provider == null ? null : provider.getProxyPassword())
                .setGenerateType(record.getGenerateType())
                .setGenerateMode(record.getGenerateMode())
                .setPrompt(record.getPrompt())
                .setInputParams(record.getInputParams());
    }

    private AigcProviderSubmitReqDTO buildProviderQueryReq(AigcGenerateRecordDO record, AigcGenerateAttemptDO attempt) {
        AigcModelRespDTO model = modelApi.getModel(attempt.getModelId()).getCheckedData();
        AigcModelProviderRespDTO provider = attempt.getProviderId() == null ? null
                : modelApi.getProvider(attempt.getProviderId()).getCheckedData();
        return new AigcProviderSubmitReqDTO()
                .setRecordId(record.getId())
                .setTaskId(record.getTaskId())
                .setUserId(record.getUserId())
                .setModelId(attempt.getModelId())
                .setModelCode(attempt.getModelCode())
                .setChannelId(attempt.getChannelId())
                .setProviderModel(resolveProviderModel(attempt, model))
                .setProviderId(attempt.getProviderId())
                .setProviderCode(attempt.getProviderCode())
                .setProviderTaskId(attempt.getProviderTaskId())
                .setProviderBaseUrl(provider == null ? null : provider.getApiBaseUrl())
                .setProviderApiKey(provider == null ? null : provider.getApiKey())
                .setProviderSecretKey(provider == null ? null : provider.getSecretKey())
                .setProviderExtraConfig(provider == null ? null : provider.getExtraConfig())
                .setProviderTimeoutSeconds(provider == null ? null : provider.getTimeoutSeconds())
                .setProxyEnabled(provider == null ? null : provider.getProxyEnabled())
                .setProxyProtocol(provider == null ? null : provider.getProxyProtocol())
                .setProxyHost(provider == null ? null : provider.getProxyHost())
                .setProxyPort(provider == null ? null : provider.getProxyPort())
                .setProxyUsername(provider == null ? null : provider.getProxyUsername())
                .setProxyPassword(provider == null ? null : provider.getProxyPassword())
                .setGenerateType(record.getGenerateType())
                .setGenerateMode(record.getGenerateMode())
                .setPrompt(record.getPrompt())
                .setInputParams(record.getInputParams());
    }

    private void finishSuccess(AigcGenerateRecordDO record, AigcProviderSubmitRespDTO resp) {
        Long assetId = createAssetIfNecessary(record);
        taskApi.markSuccess(new AigcTaskStatusUpdateReqDTO().setTaskId(record.getTaskId())
                .setExternalTaskId(record.getProviderTaskId()).setOutputText(record.getOutputText())
                .setOutputData(record.getOutputData()).setOutputAssetId(assetId)
                .setOutputAssetType(assetId == null ? null : record.getGenerateType()).setProgress(100))
                .getCheckedData();
        billingApi.confirmFreeze(new AigcBillingConfirmReqDTO().setFreezeId(record.getFreezeId())
                .setTaskId(record.getTaskId()).setActualAmount(record.getPriceAmount())
                .setModelId(record.getModelId()).setProviderId(record.getProviderId())
                .setPriceSnapshot(record.getInputParams())).getCheckedData();
        modelApi.recordUsage(new AigcModelUsageRecordReqDTO().setTaskId(record.getTaskId())
                .setUserId(record.getUserId()).setModelId(record.getModelId()).setProviderId(record.getProviderId())
                .setChannelId(record.getChannelId())
                .setCapability(record.getGenerateMode()).setRequestId(record.getGenerateNo())
                .setExternalTaskId(record.getProviderTaskId()).setPromptTokens(resp.getPromptTokens())
                .setCompletionTokens(resp.getCompletionTokens()).setTotalTokens(resp.getTotalTokens())
                .setCostPrice(record.getCostAmount()).setSalePrice(record.getPriceAmount()).setCurrencyType("POINT")
                .setStatus(0).setDurationMillis(resp.getDurationMillis())).getCheckedData();
        recordMetric(AigcGenerateMetricEnum.SUCCESS_TOTAL);
    }

    private void finishAttemptSuccess(AigcGenerateRecordDO record, AigcGenerateAttemptDO attempt,
            AigcProviderSubmitRespDTO resp) {
        if (attempt == null || attemptMapper.selectWinnerByRecordId(record.getId()) != null) {
            if (attempt != null) {
                recordAttemptCost(record, attempt, BigDecimal.ZERO);
                attemptMapper.updateById(new AigcGenerateAttemptDO().setId(attempt.getId())
                        .setStatus(AigcGenerateAttemptStatusEnum.IGNORED.getCode())
                        .setFailCode("WINNER_EXISTS").setFailReason("Another attempt has already completed this record")
                        .setFinishTime(LocalDateTime.now()));
            }
            return;
        }
        int updated = generateRecordMapper.updateByIdAndStatuses(new AigcGenerateRecordDO().setId(record.getId())
                .setStatus(AigcGenerateStatusEnum.ASSET_CREATING.getCode())
                .setModelId(attempt.getModelId())
                .setModelCode(attempt.getModelCode())
                .setChannelId(attempt.getChannelId())
                .setProviderModel(attempt.getProviderModel())
                .setProviderId(attempt.getProviderId())
                .setProviderCode(attempt.getProviderCode())
                .setProviderTaskId(resp.getProviderTaskId())
                .setProviderStatus(resp.getProviderStatus())
                .setOutputText(resp.getOutputText())
                .setOutputData(resp.getOutputData())
                .setOutputUrls(resp.getOutputUrls())
                .setCallbackTime(LocalDateTime.now())
                .setCostAmount(sumAttemptCost(record.getId())), WINNABLE_STATUSES);
        if (updated <= 0) {
            recordAttemptCost(record, attempt, BigDecimal.ZERO);
            attemptMapper.updateById(new AigcGenerateAttemptDO().setId(attempt.getId())
                    .setStatus(AigcGenerateAttemptStatusEnum.IGNORED.getCode())
                    .setFailCode("WINNER_EXISTS").setFailReason("Record is already terminal")
                    .setFinishTime(LocalDateTime.now()));
            return;
        }
        AigcGenerateRecordDO successRecord = generateRecordMapper.selectById(record.getId());
        successRecord.setCostAmount(attempt.getCostAmount());
        try {
            finishSuccess(successRecord, resp);
        } catch (Exception ex) {
            String failReason = StrUtil.maxLength(Objects.toString(ex.getMessage(), ""), 512);
            attemptMapper.updateById(new AigcGenerateAttemptDO().setId(attempt.getId())
                    .setStatus(AigcGenerateAttemptStatusEnum.FAILED.getCode())
                    .setWinner(false)
                    .setFailCode("POST_PROCESS_FAILED")
                    .setFailReason(failReason)
                    .setFinishTime(LocalDateTime.now()));
            generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId())
                    .setStatus(AigcGenerateStatusEnum.CALLBACK_WAITING.getCode())
                    .setFailReason("POST_PROCESS_FAILED")
                    .setFailMessage(failReason));
            handleAttemptFailure(record, buildRetrySubmitReq(record), attemptMapper.selectById(attempt.getId()),
                    "POST_PROCESS_FAILED", failReason);
            return;
        }
        attemptMapper.updateById(new AigcGenerateAttemptDO().setId(attempt.getId()).setWinner(true)
                .setStatus(AigcGenerateAttemptStatusEnum.SUCCESS.getCode())
                .setProviderTaskId(resp.getProviderTaskId()).setProviderStatus(resp.getProviderStatus())
                .setFinishTime(LocalDateTime.now()));
        recordAttemptCost(record, attemptMapper.selectById(attempt.getId()), record.getPriceAmount());
        generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId())
                .setStatus(AigcGenerateStatusEnum.SUCCESS.getCode())
                .setCostAmount(sumAttemptCost(record.getId()))
                .setFinishTime(LocalDateTime.now()));
        cancelLosingAttempts(generateRecordMapper.selectById(record.getId()), attempt.getId());
    }

    private void recordAttemptCost(AigcGenerateRecordDO record, AigcGenerateAttemptDO attempt, BigDecimal saleAmount) {
        if (attempt == null) {
            return;
        }
        try {
            billingApi.createCostRecord(new AigcCostRecordCreateReqDTO()
                    .setTaskId(record.getTaskId())
                    .setRecordId(record.getId())
                    .setAttemptId(attempt.getId())
                    .setUserId(record.getUserId())
                    .setModelId(attempt.getModelId())
                    .setProviderId(attempt.getProviderId())
                    .setChannelId(attempt.getChannelId())
                    .setCapability(record.getGenerateMode())
                    .setBillingUnit(attempt.getBillingUnit())
                    .setUsageAmount(BigDecimal.ONE)
                    .setCostAmount(defaultZero(attempt.getCostAmount()))
                    .setSaleAmount(defaultZero(saleAmount))
                    .setCurrencyType(StrUtil.blankToDefault(attempt.getCurrencyType(), "POINT"))
                    .setUsageSnapshot(buildAttemptUsageSnapshot(attempt))
                    .setPriceSnapshot(attempt.getPriceSnapshot())).getCheckedData();
        } catch (Exception ex) {
            log.warn("[recordAttemptCost][recordId({}) attemptId({}) failed]", record.getId(), attempt.getId(), ex);
        }
    }

    private String buildAttemptUsageSnapshot(AigcGenerateAttemptDO attempt) {
        return new JSONObject()
                .set("attemptNo", attempt.getAttemptNo())
                .set("strategy", attempt.getStrategy())
                .set("providerTaskId", attempt.getProviderTaskId())
                .set("status", attempt.getStatus())
                .set("failCode", attempt.getFailCode())
                .toString();
    }

    private BigDecimal sumAttemptCost(Long recordId) {
        BigDecimal total = BigDecimal.ZERO;
        for (AigcGenerateAttemptDO attempt : attemptMapper.selectListByRecordId(recordId)) {
            total = total.add(defaultZero(attempt.getCostAmount()));
        }
        return total;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void cancelLosingAttempts(AigcGenerateRecordDO record, Long winnerAttemptId) {
        if (record == null) {
            return;
        }
        List<Long> loserIds = new ArrayList<>();
        for (AigcGenerateAttemptDO attempt : attemptMapper.selectActiveListByRecordId(record.getId())) {
            if (!Objects.equals(attempt.getId(), winnerAttemptId)) {
                recordAttemptCost(record, attempt, BigDecimal.ZERO);
                loserIds.add(attempt.getId());
            }
        }
        attemptMapper.updateStatusByIds(loserIds, AigcGenerateAttemptStatusEnum.IGNORED.getCode(), "WINNER_EXISTS",
                "Another attempt has completed this record");
        generateRecordMapper.updateById(
                new AigcGenerateRecordDO().setId(record.getId()).setCostAmount(sumAttemptCost(record.getId())));
    }

    private Long createAssetIfNecessary(AigcGenerateRecordDO record) {
        if (!FILE_TYPES.contains(record.getGenerateType()) || record.getOutputUrls() == null
                || record.getOutputUrls().isBlank()) {
            return null;
        }
        String url = firstUrl(record.getOutputUrls());
        boolean dataUrl = StrUtil.startWithIgnoreCase(url, "data:");
        if (!dataUrl && !AigcGenerateFileSecurityUtils.isSafeRemoteUrl(url)) {
            throw exception(GENERATE_PROVIDER_RESULT_INVALID);
        }
        AigcAssetCreateReqDTO reqDTO = new AigcAssetCreateReqDTO().setUserId(record.getUserId())
                .setAssetType(record.getGenerateType()).setSourceType("GENERATE").setBizType("TASK")
                .setBizId(record.getGenerateNo()).setTaskId(record.getTaskId()).setModelId(record.getModelId())
                .setProviderId(record.getProviderId()).setTitle(record.getGenerateType() + "生成资产")
                .setPromptSnapshot(buildPromptSnapshot(record.getPrompt())).setGenerateSnapshot(record.getInputParams())
                .setVisibility("PRIVATE").setAuditStatus("PENDING");
        AigcModelProviderRespDTO provider = record.getProviderId() == null ? null
                : modelApi.getProvider(record.getProviderId()).getCheckedData();
        if (provider != null) {
            reqDTO.setProxyEnabled(provider.getProxyEnabled()).setProxyProtocol(provider.getProxyProtocol())
                    .setProxyHost(provider.getProxyHost()).setProxyPort(provider.getProxyPort())
                    .setProxyUsername(provider.getProxyUsername()).setProxyPassword(provider.getProxyPassword());
        }
        if (dataUrl) {
            reqDTO.setOriginUrl(url);
        } else {
            reqDTO.setOriginUrl(url);
        }
        AigcAssetCreateRespDTO asset = switch (record.getGenerateType()) {
            case "IMAGE" -> assetApi.createImageAsset(reqDTO).getCheckedData();
            case "VIDEO" -> assetApi.createVideoAsset(reqDTO).getCheckedData();
            case "AUDIO" -> assetApi.createAudioAsset(reqDTO).getCheckedData();
            case "DOCUMENT" -> assetApi.createDocumentAsset(reqDTO).getCheckedData();
            default -> assetApi.createAsset(reqDTO).getCheckedData();
        };
        generateRecordMapper
                .updateById(new AigcGenerateRecordDO().setId(record.getId()).setAssetIds("[" + asset.getId() + "]"));
        return asset.getId();
    }

    private String resolveProviderModel(AigcGenerateRecordDO record, AigcModelRespDTO model) {
        if (record.getProviderModel() != null) {
            return record.getProviderModel();
        }
        if (record.getProviderId() != null && model != null && model.getProviderId() != null
                && record.getProviderId().equals(model.getProviderId()) && model.getProviderModel() != null) {
            return model.getProviderModel();
        }
        return model == null ? null : model.getModel();
    }

    private String resolveProviderModel(AigcGenerateAttemptDO attempt, AigcModelRespDTO model) {
        if (attempt.getProviderModel() != null) {
            return attempt.getProviderModel();
        }
        if (attempt.getProviderId() != null && model != null && model.getProviderId() != null
                && attempt.getProviderId().equals(model.getProviderId()) && model.getProviderModel() != null) {
            return model.getProviderModel();
        }
        return model == null ? null : model.getModel();
    }

    private String buildPromptSnapshot(String prompt) {
        if (StrUtil.isBlank(prompt)) {
            return null;
        }
        return JsonUtils.toJsonString(Map.of("prompt", prompt));
    }

    private void fillDataUrlAssetFile(AigcAssetCreateReqDTO reqDTO, String dataUrl) {
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex <= 5 || !dataUrl.substring(0, commaIndex).contains(";base64")) {
            throw exception(GENERATE_PROVIDER_RESULT_INVALID);
        }
        String mimeType = dataUrl.substring("data:".length(), dataUrl.indexOf(";base64"));
        byte[] content;
        try {
            content = Base64.getDecoder().decode(dataUrl.substring(commaIndex + 1));
        } catch (IllegalArgumentException ex) {
            throw exception(GENERATE_PROVIDER_RESULT_INVALID);
        }
        if (content.length == 0) {
            throw exception(GENERATE_PROVIDER_RESULT_INVALID);
        }
        String fileExt = fileExtFromMimeType(mimeType);
        String fileUrl = fileApi.createFile(content, recordFileName(reqDTO, fileExt), "aigc/asset", mimeType);
        reqDTO.setFileUrl(fileUrl).setMimeType(mimeType).setFileExt(fileExt).setFileSize((long) content.length);
    }

    private String recordFileName(AigcAssetCreateReqDTO reqDTO, String fileExt) {
        String title = StrUtil.blankToDefault(reqDTO.getTitle(), "aigc-asset").replaceAll("[\\\\/:*?\"<>|\\s]+", "-");
        String bizId = StrUtil.blankToDefault(reqDTO.getBizId(), "task-" + reqDTO.getTaskId());
        return title + "-" + bizId + "-" + IdUtil.getSnowflakeNextIdStr() + "." + fileExt;
    }

    private String fileExtFromMimeType(String mimeType) {
        if (StrUtil.equalsIgnoreCase(mimeType, "image/jpeg")) {
            return "jpg";
        }
        if (StrUtil.startWithIgnoreCase(mimeType, "image/")) {
            return StrUtil.subAfter(mimeType, "image/", true);
        }
        if (StrUtil.startWithIgnoreCase(mimeType, "video/")) {
            return StrUtil.subAfter(mimeType, "video/", true);
        }
        if (StrUtil.startWithIgnoreCase(mimeType, "audio/")) {
            return StrUtil.subAfter(mimeType, "audio/", true);
        }
        return "bin";
    }

    private void failRecord(AigcGenerateRecordDO record, String failCode, String failReason) {
        generateRecordMapper.updateById(
                new AigcGenerateRecordDO().setId(record.getId()).setStatus(AigcGenerateStatusEnum.FAILED.getCode())
                        .setFailReason(failCode).setFailMessage(failReason).setFinishTime(LocalDateTime.now()));
        recordMetric(AigcGenerateMetricEnum.FAILED_TOTAL);
        markTaskFailedQuietly(record.getTaskId(), failCode, failReason);
        if (releaseFreezeQuietly(record.getFreezeId(), record.getTaskId(), failReason)) {
            markTaskRefundedQuietly(record.getTaskId());
        }
    }

    private void failRecordIfNotTerminal(AigcGenerateRecordDO record, String failCode, String failReason) {
        int updated = generateRecordMapper.updateByIdAndStatuses(new AigcGenerateRecordDO().setId(record.getId())
                .setStatus(AigcGenerateStatusEnum.FAILED.getCode())
                .setFailReason(failCode)
                .setFailMessage(failReason)
                .setCostAmount(sumAttemptCost(record.getId()))
                .setFinishTime(LocalDateTime.now()), WINNABLE_STATUSES);
        if (updated <= 0) {
            return;
        }
        recordMetric(AigcGenerateMetricEnum.FAILED_TOTAL);
        markTaskFailedQuietly(record.getTaskId(), failCode, failReason);
        if (releaseFreezeQuietly(record.getFreezeId(), record.getTaskId(), failReason)) {
            markTaskRefundedQuietly(record.getTaskId());
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

    private void recordMetric(AigcGenerateMetricEnum metric) {
        recordMetric(metric, 1D);
    }

    private void recordMetric(AigcGenerateMetricEnum metric, double amount) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry != null) {
            counter(meterRegistry, metric.getName()).increment(amount);
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

    private String buildRequestSummary(AigcGenerateRecordDO record) {
        JSONObject params = parseInputParamsJson(record.getInputParams());
        return new JSONObject()
                .set("generateMode", record.getGenerateMode())
                .set("modelCode", record.getModelCode())
                .set("prompt", maskPrompt(record.getPrompt()))
                .set("inputImageCount", countInputImages(params))
                .set("imagePayloadMode", resolveImagePayloadMode(record, params))
                .set("imagePayloadBytes", estimateInputImagePayloadBytes(params))
                .toString();
    }

    private String buildRequestSummary(AigcGenerateRecordDO record, AigcGenerateAttemptDO attempt) {
        JSONObject params = parseInputParamsJson(record.getInputParams());
        return new JSONObject()
                .set("generateMode", record.getGenerateMode())
                .set("modelCode", attempt.getModelCode())
                .set("attemptNo", attempt.getAttemptNo())
                .set("strategy", attempt.getStrategy())
                .set("channelId", attempt.getChannelId())
                .set("providerCode", attempt.getProviderCode())
                .set("prompt", maskPrompt(record.getPrompt()))
                .set("inputImageCount", countInputImages(params))
                .set("imagePayloadMode", resolveImagePayloadMode(record, attempt, params))
                .set("imagePayloadBytes", estimateInputImagePayloadBytes(params))
                .toString();
    }

    private JSONObject parseInputParamsJson(String inputParams) {
        if (StrUtil.isBlank(inputParams) || !JSONUtil.isTypeJSON(inputParams)) {
            return JSONUtil.createObj();
        }
        return JSONUtil.parseObj(inputParams);
    }

    private int countInputImages(JSONObject params) {
        List<String> images = collectInputImages(params);
        return images.size();
    }

    private String resolveImagePayloadMode(AigcGenerateRecordDO record, JSONObject params) {
        if (countInputImages(params) <= 0) {
            return null;
        }
        if ("grok".equalsIgnoreCase(record.getProviderCode())
                && "grok-imagine-image".equalsIgnoreCase(record.getModelCode())) {
            return "json.image";
        }
        if ("grok".equalsIgnoreCase(record.getProviderCode())
                && "IMAGE_TO_IMAGE".equalsIgnoreCase(record.getGenerateMode())) {
            return "multipart.image";
        }
        return "provider.reference";
    }

    private String resolveImagePayloadMode(AigcGenerateRecordDO record, AigcGenerateAttemptDO attempt,
            JSONObject params) {
        if (countInputImages(params) <= 0) {
            return null;
        }
        if ("grok".equalsIgnoreCase(attempt.getProviderCode())
                && "grok-imagine-image".equalsIgnoreCase(attempt.getModelCode())) {
            return "json.image";
        }
        if ("grok".equalsIgnoreCase(attempt.getProviderCode())
                && "IMAGE_TO_IMAGE".equalsIgnoreCase(record.getGenerateMode())) {
            return "multipart.image";
        }
        return "provider.reference";
    }

    private long estimateInputImagePayloadBytes(JSONObject params) {
        long total = 0L;
        for (String image : collectInputImages(params)) {
            if (StrUtil.startWithIgnoreCase(image, "data:")) {
                total += image.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            }
        }
        return total;
    }

    private List<String> collectInputImages(JSONObject params) {
        List<String> images = new ArrayList<>();
        addInputImage(images, params.getStr("image_url"));
        addInputImage(images, params.getStr("image"));
        addArrayImages(images, params, "referenceImages");
        addArrayImages(images, params, "inputImageUrls");
        JSONArray inputImages = params.getJSONArray("inputImages");
        if (inputImages != null) {
            for (Object item : inputImages) {
                JSONObject image = JSONUtil.parseObj(item);
                addInputImage(images, StrUtil.blankToDefault(image.getStr("url"), image.getStr("dataUrl")));
            }
        }
        return images;
    }

    private void addArrayImages(List<String> images, JSONObject params, String key) {
        JSONArray array = params.getJSONArray(key);
        if (array == null) {
            return;
        }
        for (Object item : array) {
            addInputImage(images, String.valueOf(item));
        }
    }

    private void addInputImage(List<String> images, String image) {
        if (StrUtil.isNotBlank(image) && !images.contains(image)) {
            images.add(image);
        }
    }

    private String buildResponseSummary(AigcProviderSubmitRespDTO resp) {
        return new JSONObject()
                .set("providerStatus", resp.getProviderStatus())
                .set("errorCode", resp.getErrorCode())
                .set("errorMessage", resp.getErrorMessage())
                .toString();
    }

    private String firstUrl(String outputUrls) {
        if (JSONUtil.isTypeJSONArray(outputUrls)) {
            return JSONUtil.parseArray(outputUrls).getStr(0, "");
        }
        return outputUrls.replace("[", "").replace("]", "").replace("\"", "").trim();
    }
}
