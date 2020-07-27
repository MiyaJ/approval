package com.ezy.approval.controller;


import com.ezy.approval.model.template.ApprovalTemplateAddDTO;
import com.ezy.approval.service.IApprovalTemplateService;
import com.ezy.common.model.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.stereotype.Controller;

/**
 * <p>
 * 审批模板  前端控制器
 * </p>
 *
 * @author CaiXiaowei
 * @since 2020-07-27
 */
@Controller
@RequestMapping("/approval-template")
public class ApprovalTemplateController {

    @Autowired
    private IApprovalTemplateService templateService;

    /**
     * 新增审批模板
     * @description
     * @author Caixiaowei
     * @param approvalTemplateAddDTO: ApprovalTemplateAddDTO 审批模板新增dto
     * @updateTime 2020/7/27 15:02
     * @return CommonResult
     */
    @PostMapping(value = "/add")
    private CommonResult add(@RequestBody ApprovalTemplateAddDTO approvalTemplateAddDTO) {
        return templateService.add(approvalTemplateAddDTO);
    }
}

