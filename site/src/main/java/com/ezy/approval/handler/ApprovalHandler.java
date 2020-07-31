package com.ezy.approval.handler;

import com.ezy.approval.model.callback.approval.ApprovalInfo;
import com.ezy.approval.model.callback.approval.Notifyer;
import com.ezy.approval.model.callback.approval.SpRecord;
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

    public void handle(ApprovalInfo approvalInfo) {
        String spNo = approvalInfo.getSpNo();
        List<Notifyer> notifyer = approvalInfo.getNotifyer();
        List<SpRecord> spRecord = approvalInfo.getSpRecord();
    }
}
