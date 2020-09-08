package com.ezy.approval.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ezy.approval.entity.ApprovalApply;
import com.ezy.approval.model.callback.approval.Applyer;
import com.ezy.approval.model.callback.approval.Notifyer;
import com.ezy.approval.model.callback.approval.SpRecord;
import com.ezy.approval.model.sys.EmpInfo;
import com.ezy.approval.service.IApprovalApplyService;
import com.ezy.approval.service.IApprovalService;
import com.ezy.approval.service.ICommonService;
import com.ezy.approval.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Caixiaowei
 * @ClassName CompensateHandler
 * @Description 审批补偿处理
 * @createTime 2020/9/7$ 13:58$
 */
@Service
@Slf4j
public class CompensateHandler {

    @Autowired
    private IApprovalService approvalService;
    @Autowired
    private IApprovalApplyService approvalApplyService;
    @Autowired
    private ICommonService commonService;
    @Autowired
    private ApprovalHandler approvalHandler;

    /**
     * 补偿审批单据
     *
     * @param spNo 审批单编号
     * @return
     * @author Caixiaowei
     * @updateTime 2020/9/7 14:50
     */
    public void compensateApprovalDetail(String spNo) {
        log.info("补偿审批单据 : {}", spNo);
        JSONObject approvalDetail = approvalService.getApprovalDetail(spNo);

        // 审批备注
        JSONArray comments = approvalDetail.getJSONArray("comments");

        processApproval(spNo, approvalDetail);

        // 审批节点
        JSONArray spRecords = approvalDetail.getJSONArray("sp_record");
        List<SpRecord> spRecordList = JSONObject.parseArray(spRecords.toJSONString(), SpRecord.class);
        approvalHandler.processSpRecord(spNo, spRecordList);

        // 审批抄送人
        JSONArray notifyers = approvalDetail.getJSONArray("notifyer");
        List<Notifyer> notifyerList = JSONObject.parseArray(notifyers.toJSONString(), Notifyer.class);
        approvalHandler.processNotifyer(spNo, notifyerList);
    }

    private void processApproval(String spNo, JSONObject approvalDetail) {

        String spName = approvalDetail.getString("sp_name");
        Integer spStatus = approvalDetail.getInteger("sp_status");
        String templateId = approvalDetail.getString("template_id");
        Long applyTime = approvalDetail.getLong("apply_time");
        // 审批申请数据
        JSONObject applyData = approvalDetail.getJSONObject("apply_data");

        // 审批申请人
        Applyer applyer = approvalDetail.getObject("applyer", Applyer.class);
        String userId = applyer.getUserId();
        String party = applyer.getParty();
        QueryWrapper<ApprovalApply> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sp_no", spNo);
        ApprovalApply apply = approvalApplyService.getOne(queryWrapper);
        EmpInfo empInfo = commonService.getEmpByUserId(userId);
        if (apply == null) {
            apply = new ApprovalApply();
            apply.setSpNo(spNo);
            apply.setCreateBy(empInfo.getEmpId());
            apply.setCreateTime(DateUtil.secondToLocalDateTime(applyTime));

        }
        apply.setTemplateId(templateId);
        apply.setSpName(spName);
        apply.setStatus(spStatus);
        apply.setApplyTime(applyTime);
        apply.setEmpId(empInfo.getEmpId());
        apply.setWxUserId(userId);
        apply.setWxPartyId(party);
        apply.setApplyData(applyData.toString());
        apply.setCreateBy(empInfo.getEmpId());

        LocalDateTime now = LocalDateTime.now();
        apply.setUpdateTime(now);
        apply.setQwCallbackVersion(DateUtil.localDateTimeToSecond(now));
        approvalApplyService.saveOrUpdate(apply);
    }
}
