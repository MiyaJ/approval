package com.ezy.approval.handler;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ezy.approval.entity.ApprovalSpNotifyer;
import com.ezy.approval.entity.ApprovalSpRecord;
import com.ezy.approval.entity.ApprovalSpRecordDetail;
import com.ezy.approval.model.callback.approval.*;
import com.ezy.approval.model.sys.EmpInfo;
import com.ezy.approval.service.IApprovalSpNotifyerService;
import com.ezy.approval.service.IApprovalSpRecordDetailService;
import com.ezy.approval.service.IApprovalSpRecordService;
import com.ezy.common.enums.ApprovalStatuChangeEventEnum;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private IApprovalSpNotifyerService approvalSpNotifyerService;
    @Autowired
    private IApprovalSpRecordService approvalSpRecordService;
    @Autowired
    private IApprovalSpRecordDetailService approvalSpRecordDetailService;


    public void handle(ApprovalInfo approvalInfo) {

        String templateId = approvalInfo.getTemplateId();
        // 审批单号
        String spNo = approvalInfo.getSpNo();
        String spName = approvalInfo.getSpName();
        // 申请单状态：1-审批中；2-已通过；3-已驳回；4-已撤销；6-通过后撤销；7-已删除；10-已支付
        Integer spStatus = approvalInfo.getSpStatus();
        Applyer applyer = approvalInfo.getApplyer();
        // 审批申请提交时间,Unix时间戳 10位
        Integer applyTime = approvalInfo.getApplyTime();
        // 备注信息
        List<Comments> comments = approvalInfo.getComments();
        // 审批申请状态变化类型：1-提单；2-同意；3-驳回；4-转审；5-催办；6-撤销；8-通过后撤销；10-添加备注
        Integer statuChangeEvent = approvalInfo.getStatuChangeEvent();
        // 抄送人信息
        List<Notifyer> notifyers = approvalInfo.getNotifyer();
        // 审批流程信息
        List<SpRecord> spRecords = approvalInfo.getSpRecord();

        if (statuChangeEvent.equals(ApprovalStatuChangeEventEnum.APPLY.getStatus())) {

            // 提单, 处理审批单的抄送人, 审批节点信息, 并入库
            // 处理抄送人
            processNotifyer(spNo, notifyers);

            // 审批流程节点处理
            processSpRecord(spNo, spRecords);

        }
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
    private void processUpdateSpRecord(String spNo, List<SpRecord> spRecords) {
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
                }
                approvalSpRecord.setSpNo(spNo);
                approvalSpRecord.setApproverAttr(approverAttr);
                approvalSpRecord.setSpStatus(recordSpStatus);
                approvalSpRecord.setStep(step);

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
                    }
                    // TODO: 2020/9/2 调用usercenter 通过企业微信userid 查询员工信息: empId, empName
                    EmpInfo empInfo = getEmpByUserId(userId);

                    String speech = detail.getSpeech();
                    Integer detailSpStatus = detail.getSpStatus();
                    Long spTime = detail.getSpTime();

                    recordDetail.setSpRecordId(spRecordId);
                    recordDetail.setApproverUserId(userId);
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
     * 审批流程节点处理
     *
     * @param spNo 审批单号
     * @param spRecords 审批流程节点信息
     * @return
     * @author Caixiaowei
     * @updateTime 2020/9/2 14:29
     */
    private void processSpRecord(String spNo, List<SpRecord> spRecords) {
        if (CollectionUtil.isNotEmpty(spRecords)) {
            Integer step = 0;
            for (SpRecord spRecord : spRecords) {
                // 审批流程
                ApprovalSpRecord approvalSpRecord = new ApprovalSpRecord();

                Integer approverAttr = spRecord.getApproverAttr();
                List<Details> details = spRecord.getDetails();
                Integer recordSpStatus = spRecord.getSpStatus();

                approvalSpRecord.setSpNo(spNo);
                approvalSpRecord.setApproverAttr(approverAttr);
                approvalSpRecord.setSpStatus(recordSpStatus);
                approvalSpRecord.setStep(step);

                approvalSpRecordService.saveOrUpdate(approvalSpRecord);

                List<ApprovalSpRecordDetail> recordDetails = Lists.newArrayList();
                for (Details detail : details) {
                    // 审批节点详情
                    ApprovalSpRecordDetail approvalSpRecordDetail = new ApprovalSpRecordDetail();

                    Applyer approver = detail.getApprover();
                    String userId = approver.getUserId();
                    String party = approver.getParty();
                    // TODO: 2020/9/2 调用usercenter 通过企业微信userid 查询员工信息: empId, empName
                    EmpInfo empInfo = getEmpByUserId(userId);

                    String speech = detail.getSpeech();
                    Integer detailSpStatus = detail.getSpStatus();
                    Long spTime = detail.getSpTime();

                    approvalSpRecordDetail.setApproverEmpId(empInfo.getEmpId());
                    approvalSpRecordDetail.setApproverEmpName(empInfo.getEmpName());
                    approvalSpRecordDetail.setApproverUserId(userId);
                    approvalSpRecordDetail.setSpeech(speech);
                    approvalSpRecordDetail.setSpStatus(detailSpStatus);
                    approvalSpRecordDetail.setSpTime(spTime);
                    approvalSpRecordDetail.setSpRecordId(approvalSpRecord.getId());

                    recordDetails.add(approvalSpRecordDetail);
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
    private void processNotifyer(String spNo, List<Notifyer> notifyers) {
        if (CollectionUtil.isNotEmpty(notifyers)) {
            List<ApprovalSpNotifyer> spNotifyers = Lists.newArrayList();

            for (Notifyer notifyer : notifyers) {
                String userId = notifyer.getUserId();
                // TODO: 2020/9/2 调用 usercenter,根据企业微信userid查询员工信息: empId, empName
                EmpInfo empInfo = getEmpByUserId(userId);

                ApprovalSpNotifyer spNotifyer = new ApprovalSpNotifyer();
                spNotifyer.setSpNo(spNo);
                spNotifyer.setUserId(userId);
                spNotifyer.setEmpId(empInfo.getEmpId());
                spNotifyer.setEmpName(empInfo.getEmpName());

                spNotifyers.add(spNotifyer);
            }
            approvalSpNotifyerService.saveOrUpdateBatch(spNotifyers);
        }
    }

    /**
     * 根据企业微信用户id查询员工信息
     *
     * @param userId 业微信用户id
     * @return EmpInfo
     * @author Caixiaowei
     * @updateTime 2020/9/2 14:34
     */
    private EmpInfo getEmpByUserId(String userId) {
        EmpInfo empInfo = new EmpInfo();

        // TODO: 2020/9/2 待usercenter 开发完成，实现具体逻辑
        empInfo.setQwUserId(userId);
        empInfo.setEmpId(1L);
        empInfo.setEmpName("张三");

        return empInfo;
    }
}
