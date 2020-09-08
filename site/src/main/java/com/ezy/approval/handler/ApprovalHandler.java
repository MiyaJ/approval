package com.ezy.approval.handler;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ezy.approval.entity.ApprovalApply;
import com.ezy.approval.entity.ApprovalSpNotifyer;
import com.ezy.approval.entity.ApprovalSpRecord;
import com.ezy.approval.entity.ApprovalSpRecordDetail;
import com.ezy.approval.model.callback.approval.*;
import com.ezy.approval.model.sys.EmpInfo;
import com.ezy.approval.service.*;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Caixiaowei
 * @ClassName ApprovalHandler.java
 * @Description 审批处理
 * @createTime 2020年07月31日 13:59:00
 */
@Service
public class ApprovalHandler {

    @Autowired
    private IApprovalApplyService approvalApplyService;
    @Autowired
    private IApprovalSpNotifyerService approvalSpNotifyerService;
    @Autowired
    private IApprovalSpRecordService approvalSpRecordService;
    @Autowired
    private IApprovalSpRecordDetailService approvalSpRecordDetailService;
    @Autowired
    private ICommonService commonService;


    public void handle(ApprovalStatuChangeEvent approvalStatuChangeEvent) {
        ApprovalInfo approvalInfo = approvalStatuChangeEvent.getApprovalInfo();

        // 审批单号
        String spNo = approvalInfo.getSpNo();
        String spName = approvalInfo.getSpName();
        // 申请单状态：1-审批中；2-已通过；3-已驳回；4-已撤销；6-通过后撤销；7-已删除；10-已支付
        Integer spStatus = approvalInfo.getSpStatus();
        Applyer applyer = approvalInfo.getApplyer();
        // 审批申请提交时间,Unix时间戳 10位
        Long applyTime = approvalInfo.getApplyTime();
        // 备注信息
        List<Comments> comments = approvalInfo.getComments();
        // 审批申请状态变化类型：1-提单；2-同意；3-驳回；4-转审；5-催办；6-撤销；8-通过后撤销；10-添加备注
        Integer statuChangeEvent = approvalInfo.getStatuChangeEvent();
        // 抄送人信息
        List<Notifyer> notifyers = approvalInfo.getNotifyer();
        // 审批流程信息
        List<SpRecord> spRecords = approvalInfo.getSpRecord();

        // TODO: 2020/9/4 根据 statuChangeEvent 来进行后续业务处理：如催单发送消息等


        // 提单, 处理审批单的抄送人, 审批节点信息, 并入库
        // 消息发送时间, 时间戳, 精确到秒
        Long createTime = approvalStatuChangeEvent.getCreateTime();
        // 处理审批单基本信息
        boolean processApply = processApply(approvalInfo, createTime);
        if (processApply) {
            // 处理抄送人
            processNotifyer(spNo, notifyers);
            // 处理审批流程节点
            processSpRecord(spNo, spRecords);
        }
    }

    /**
     * 处理审批单基本信息
     *
     * @param approvalInfo 审批单信息
     * @return
     * @author Caixiaowei
     * @updateTime 2020/9/4 14:22
     */
    private boolean processApply(ApprovalInfo approvalInfo, Long createTime) {
        String spNo = approvalInfo.getSpNo();
        QueryWrapper<ApprovalApply> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sp_no", spNo);

        ApprovalApply apply = approvalApplyService.getOne(queryWrapper);
        if (apply != null && apply.getQwCallbackVersion() != null && apply.getQwCallbackVersion() >= createTime) {
            // mq callback 版本低于 MySQL 中版本, 不予更新
            return false;
        }

        String spName = approvalInfo.getSpName();
        Applyer applyer = approvalInfo.getApplyer();
        String userId = applyer.getUserId();
        String party = applyer.getParty();
        EmpInfo empInfo = commonService.getEmpByUserId(userId);
        Long empId = empInfo.getEmpId();
        String empName = empInfo.getEmpName();

        Long applyTime = approvalInfo.getApplyTime();
        Integer spStatus = approvalInfo.getSpStatus();
        String templateId = approvalInfo.getTemplateId();


        if (apply == null) {
            apply = new ApprovalApply();
            apply.setSpNo(spNo);
            apply.setCreateBy(empInfo.getEmpId());
            apply.setCreateTime(LocalDateTime.now());
            apply.setSpName(spName);
            apply.setTemplateId(templateId);

        }
        apply.setWxUserId(userId);
        apply.setWxPartyId(party);
        apply.setEmpId(empId);
        apply.setStatus(spStatus);
        apply.setApplyTime(applyTime);
        apply.setWxPartyId(party);

        boolean update = approvalApplyService.saveOrUpdate(apply);

        return update;
    }

    /**
     * 审批流程节点处理
     *
     * @param spNo 审批单号
     * @param spRecords 审批流程节点信息
     * @return
     * @author Caixiaowei
     * @updateTime 2020/9/2 14:29
     */
    public void processSpRecord(String spNo, List<SpRecord> spRecords) {
        if (CollectionUtil.isNotEmpty(spRecords)) {
            Integer step = 0;
            for (SpRecord spRecord : spRecords) {
                Integer approverAttr = spRecord.getApproverAttr();
                List<Details> details = spRecord.getDetails();
                Integer recordSpStatus = spRecord.getSpStatus();

                // 审批流程
                QueryWrapper<ApprovalSpRecord> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("sp_no", spNo);
                queryWrapper.eq("step", step);
                ApprovalSpRecord approvalSpRecord = approvalSpRecordService.getOne(queryWrapper);
                if (approvalSpRecord == null) {
                    approvalSpRecord = new ApprovalSpRecord();
                    approvalSpRecord.setSpNo(spNo);
                    approvalSpRecord.setStep(step);
                }

                approvalSpRecord.setApproverAttr(approverAttr);
                approvalSpRecord.setSpStatus(recordSpStatus);

                approvalSpRecordService.saveOrUpdate(approvalSpRecord);

                // 处理审批节点详情
                Long spRecordId = approvalSpRecord.getId();
                List<ApprovalSpRecordDetail> recordDetails = Lists.newArrayList();
                for (Details detail : details) {
                    Applyer approver = detail.getApprover();
                    String userId = approver.getUserId();

                    QueryWrapper<ApprovalSpRecordDetail> detailQueryWrapper = new QueryWrapper<>();
                    detailQueryWrapper.eq("sp_record_id", spRecordId);
                    detailQueryWrapper.eq("approver_user_id", userId);
                    ApprovalSpRecordDetail recordDetail = approvalSpRecordDetailService.getOne(detailQueryWrapper);
                    if (recordDetail == null) {
                        recordDetail = new ApprovalSpRecordDetail();
                        recordDetail.setSpRecordId(spRecordId);
                        recordDetail.setApproverUserId(userId);
                    }
                    // TODO: 2020/9/2 调用usercenter 通过企业微信userid 查询员工信息: empId, empName
                    EmpInfo empInfo = commonService.getEmpByUserId(userId);

                    String speech = detail.getSpeech();
                    Integer detailSpStatus = detail.getSpStatus();
                    Long spTime = detail.getSpTime();

                    recordDetail.setApproverEmpId(empInfo.getEmpId());
                    recordDetail.setApproverEmpName(empInfo.getEmpName());
                    recordDetail.setSpeech(speech);
                    recordDetail.setSpStatus(detailSpStatus);
                    recordDetail.setSpTime(spTime);

                    recordDetails.add(recordDetail);
                }
                approvalSpRecordDetailService.saveOrUpdateBatch(recordDetails);

                step++;

            }
        }
    }

    /**
     * 处理抄送人
     *
     * @param spNo 审批单号
     * @param spNo 抄送人信息
     * @return
     * @author Caixiaowei
     * @updateTime 2020/9/2 14:29
     */
    public void processNotifyer(String spNo, List<Notifyer> notifyers) {
        if (CollectionUtil.isNotEmpty(notifyers)) {
            List<ApprovalSpNotifyer> spNotifyers = Lists.newArrayList();
            int step = 0;
            for (Notifyer notifyer : notifyers) {
                String userId = notifyer.getUserId();
                // TODO: 2020/9/2 调用 usercenter,根据企业微信userid查询员工信息: empId, empName
                EmpInfo empInfo = commonService.getEmpByUserId(userId);

                QueryWrapper<ApprovalSpNotifyer> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("sp_no", spNo);
                queryWrapper.eq("step", step);
                ApprovalSpNotifyer spNotifyer = approvalSpNotifyerService.getOne(queryWrapper);
                if (spNotifyer == null) {
                    spNotifyer = new ApprovalSpNotifyer();
                    spNotifyer.setSpNo(spNo);
                    spNotifyer.setStep(step);
                }

                spNotifyer.setUserId(userId);
                spNotifyer.setEmpId(empInfo.getEmpId());
                spNotifyer.setEmpName(empInfo.getEmpName());

                spNotifyers.add(spNotifyer);
                step ++;
            }
            approvalSpNotifyerService.saveOrUpdateBatch(spNotifyers);
        }
    }

}
