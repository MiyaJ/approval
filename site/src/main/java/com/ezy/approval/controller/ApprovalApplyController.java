package com.ezy.approval.controller;


import com.ezy.approval.model.apply.ApprovalApplyDTO;
import com.ezy.approval.service.IApprovalApplyService;
import com.ezy.common.model.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  审批申请记录 前端控制器
 * </p>
 *
 * @author CaiXiaowei
 * @since 2020-07-27
 */
@RestController
@RequestMapping("/approval")
public class ApprovalApplyController {

    @Autowired
    private IApprovalApplyService approvalApplyService;


    /**
     * 审批申请
     * @description
     * @author Caixiaowei
     * @param approvalApplyDTO ApprovalApplyDTO 申请数据
     * @updateTime 2020/7/29 15:18
     * @return
     */
    @PostMapping("/apply")
    public CommonResult apply(@RequestBody ApprovalApplyDTO approvalApplyDTO) {
        return approvalApplyService.apply(approvalApplyDTO);
    }
}

