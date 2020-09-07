package com.ezy.approval.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ezy.approval.entity.ApprovalApply;
import com.ezy.approval.handler.CompensateHandler;
import com.ezy.approval.service.IApprovalApplyService;
import com.ezy.approval.service.IApprovalTaskService;
import com.ezy.common.enums.ApprovalStatusEnum;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Caixiaowei
 * @ClassName ApprovalTaskService
 * @Description 审批定时任务业务层
 * @createTime 2020/9/7$ 16:02$
 */
@Service
@Slf4j
public class ApprovalTaskServiceImpl implements IApprovalTaskService {

    @Value("${approval.time.out:3600}")
    private Long APPROVAL_TIME_OUT;

    @Autowired
    private IApprovalApplyService approvalApplyService;
    @Autowired
    private CompensateHandler compensateHandler;

    /**
     * 补偿审批单据
     * @description 超过1h未审批的单据, 去查询企微获取最新审批信息, 同步更新到MySQL
     * @param
     * @return
     * @author Caixiaowei
     * @updateTime 2020/9/7 16:13
     */
    @Override
    public void compensateApproval(String spNo) {
        List<String> spNoList = Lists.newArrayList();
        if (StrUtil.isEmpty(spNo)) {
            // 超过1h未审批的单据, 去查询企微获取最新审批信息, 同步更新到MySQL
            LocalDateTime now = LocalDateTime.now();
            Long nowTimestamp = now.toEpochSecond(ZoneOffset.ofHours(8));
            Long timeOut = nowTimestamp - APPROVAL_TIME_OUT;

            QueryWrapper<ApprovalApply> queryWrapper = new QueryWrapper<>();
            queryWrapper.le("apply_time", timeOut);
            queryWrapper.notIn("status", ApprovalStatusEnum.APPROVED.getStatus(), ApprovalStatusEnum.DISMISSED.getStatus());
            List<ApprovalApply> list = approvalApplyService.list(queryWrapper);

            spNoList = list.stream().map(ApprovalApply::getSpNo).collect(Collectors.toList());
        } else {
            spNoList.add(spNo);
        }

        if (CollectionUtil.isNotEmpty(spNoList)) {
            log.info("超过1h未审批的单据--->{}", spNoList);
            for (String sp : spNoList) {
                compensateHandler.compensateApprovalDetail(sp);
            }
        }
    }
}
