package com.ezy.approval.service;

import com.ezy.approval.entity.ApprovalApply;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ezy.approval.model.apply.ApprovalApplyDTO;
import com.ezy.common.model.CommonResult;

/**
 * <p>
 *  审批申请记录 服务类
 * </p>
 *
 * @author CaiXiaowei
 * @since 2020-07-27
 */
public interface IApprovalApplyService extends IService<ApprovalApply> {

    /**
     * 审批申请
     * @description
     * @author Caixiaowei
     * @param approvalApplyDTO ApprovalApplyDTO 申请数据
     * @updateTime 2020/7/29 15:19
     * @return
     */
    CommonResult apply(ApprovalApplyDTO approvalApplyDTO);

    /**
     * 根据单号查询审批单据
     * @description
     * @author Caixiaowei
     * @param systemCode string 调用方系统标识
     * @param spNo string 审批单号
     * @updateTime 2020/7/31 11:28
     * @return
     */
    CommonResult detail(String systemCode, String spNo);
}
