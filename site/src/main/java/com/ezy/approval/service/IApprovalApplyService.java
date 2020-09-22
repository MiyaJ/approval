package com.ezy.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ezy.approval.entity.ApprovalApply;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ezy.approval.model.apply.ApprovalApplyDTO;
import com.ezy.approval.model.apply.ApprovalErrorListVO;
import com.ezy.approval.model.apply.ApprovalQueryDTO;
import com.ezy.common.model.CommonResult;

import java.util.List;

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

    /**
     * 查询系统应用的审批单
     * @description
     * @author Caixiaowei
     * @param systemCode string 系统标识
     * @param startDate string 开始日期
     * @param endDate string 结束日期
     * @updateTime 2020/8/3 9:33
     * @return
     */
    CommonResult listBySystemCode(String systemCode, String startDate, String endDate);

    /**
     * 根据单号查询审批单据
     *
     * @param spNo 审批单编号
     * @return ApprovalApply
     * @author Caixiaowei
     * @updateTime 2020/9/22 10:55
     */
    ApprovalApply getApprovalApply(String spNo);

    /**
     * 异常审批单列表
     *
     * @param approvalQueryDTO ApprovalQueryDTO 查询参数
     * @return
     * @author Caixiaowei
     * @updateTime 2020/9/22 16:36
     */
    IPage<ApprovalErrorListVO> errorList(ApprovalQueryDTO approvalQueryDTO);
}
