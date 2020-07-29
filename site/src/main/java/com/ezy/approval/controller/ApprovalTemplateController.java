package com.ezy.approval.controller;


import com.ezy.approval.model.template.ApprovalTemplateAddDTO;
import com.ezy.approval.model.template.TemplateDetailVO;
import com.ezy.approval.service.IApprovalTemplateService;
import com.ezy.common.model.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 审批模板  前端控制器
 * </p>
 *
 * @author CaiXiaowei
 * @since 2020-07-27
 */
@RestController
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
        CommonResult add = templateService.add(approvalTemplateAddDTO);
        return add;
    }

    /**
     * 获取模板详情
     * @description
     * @author Caixiaowei
     * @param templateId: string 模板id
     * @param systemCode: string 调用方系统标识
     * @updateTime 2020/7/29 13:48
     * @return TemplateDetailVO 模板详情
     */
    @GetMapping("/detail")
    public CommonResult detail(String templateId, String systemCode) {
        return templateService.detail(templateId, systemCode);
    }

    /**
     * 注册模板
     * @description 调用方系统注册模板, 未注册无法使用
     * @author Caixiaowei
     * @param approvalTemplateAddDTO ApprovalTemplateAddDTO 模板注册信息
     * @updateTime 2020/7/29 15:24
     * @return
     */
    @PostMapping("/register")
    public CommonResult register(ApprovalTemplateAddDTO approvalTemplateAddDTO) {
        // TODO: 2020/7/29 调用方系统注册模板: 暂时审批管理员操作
        return CommonResult.success("注册成功!");
    }


    public CommonResult list() {

        // TODO: 2020/7/29 审批管理后台使用
        return CommonResult.success("查询模板列表成功!");
    }
}

