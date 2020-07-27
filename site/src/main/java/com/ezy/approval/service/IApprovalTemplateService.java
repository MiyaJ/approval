package com.ezy.approval.service;

import com.ezy.approval.entity.ApprovalTemplate;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ezy.approval.model.template.ApprovalTemplateAddDTO;
import com.ezy.common.model.CommonResult;

/**
 * <p>
 * 审批模板  服务类
 * </p>
 *
 * @author CaiXiaowei
 * @since 2020-07-27
 */
public interface IApprovalTemplateService extends IService<ApprovalTemplate> {

    /**
     * 新增审批模板
     * @description
     * @author Caixiaowei
     * @param approvalTemplateAddDTO: ApprovalTemplateAddDTO 审批模板新增dto
     * @updateTime 2020/7/27 15:02
     * @return CommonResult
     */
    CommonResult add(ApprovalTemplateAddDTO approvalTemplateAddDTO);
}
