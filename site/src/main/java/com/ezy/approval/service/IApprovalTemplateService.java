package com.ezy.approval.service;

import com.ezy.approval.entity.ApprovalTemplate;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ezy.approval.model.template.ApprovalTemplateAddDTO;
import com.ezy.approval.model.template.TemplateDetailVO;
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

    /**
     * 查询审批模板详情
     * @description 需要先校验调用方是否注册次模板
     * @author Caixiaowei
     * @param templateId: string 模板id
     * @param systemCode: string 调用方系统标识
     * @updateTime 2020/7/29 13:51
     * @return CommonResult
     */
    CommonResult detail(String templateId, String systemCode);

    /**
     * 根据模板id 查询审批模板
     * @description
     * @author Caixiaowei
     * @param templateId string 模板id
     * @updateTime 2020/7/29 15:52
     * @return ApprovalTemplate
     */
    ApprovalTemplate getByTemplateId(String templateId);
}
