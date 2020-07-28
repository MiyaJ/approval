package com.ezy.approval.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ezy.approval.entity.ApprovalTemplate;
import com.ezy.approval.entity.ApprovalTemplateSystem;
import com.ezy.approval.mapper.ApprovalTemplateMapper;
import com.ezy.approval.model.template.ApprovalTemplateAddDTO;
import com.ezy.approval.service.IApprovalService;
import com.ezy.approval.service.IApprovalTemplateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezy.approval.service.IApprovalTemplateSystemService;
import com.ezy.approval.utils.OkHttpClientUtil;
import com.ezy.common.model.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 审批模板  服务实现类
 * </p>
 *
 * @author CaiXiaowei
 * @since 2020-07-27
 */
@Service
@Slf4j
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApprovalTemplateServiceImpl extends ServiceImpl<ApprovalTemplateMapper, ApprovalTemplate> implements IApprovalTemplateService {

    @Autowired
    private IApprovalService approvalService;
    @Autowired
    private IApprovalTemplateSystemService templateSystemService;

    /**
     * 新增审批模板
     *
     * @param approvalTemplateAddDTO : ApprovalTemplateAddDTO 审批模板新增dto
     * @return CommonResult
     * @description
     * @author Caixiaowei
     * @updateTime 2020/7/27 15:02
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonResult add(ApprovalTemplateAddDTO approvalTemplateAddDTO) {
        String templateId = approvalTemplateAddDTO.getTemplateId();
        String systemCode = approvalTemplateAddDTO.getSystemCode();
        String callbackUrl = approvalTemplateAddDTO.getCallbackUrl();
        String patternImage = approvalTemplateAddDTO.getPatternImage();

        // 1. 根据 templateId 查询企微审批模板详情
        JSONObject detail = approvalService.getTemplateDetail(templateId);
        if (detail == null || detail.getIntValue("errcode") != 0) {
            return CommonResult.failed("模板查询异常, 请检查模板id 是否正确.");
        }

        // 2. 保存模板入库
        ApprovalTemplate template = new ApprovalTemplate();
        // 模板名称
        JSONArray templateNames = detail.getJSONArray("template_names");
        String templateName = templateNames.getJSONObject(0).getString("text");

        // 模板内容
        JSONObject templateContent = detail.getJSONObject("template_content");


        template.setTemplateId(templateId);
        template.setTemplateName(templateName);
        template.setIsDeleted(0);
        template.setContent(JSONObject.toJSONString(templateContent));
        template.setPatternImage(patternImage);
        template.setIsEnable(true);
        // TODO: 2020/7/27 模板出入参待完善
        template.setRequestParam("");
        template.setResponseParam("");
        template.setDescription("");
        // TODO: 2020/7/27 创建人/更新人 待完善
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());

        this.save(template);

        // 3. 维护模板与调用方关系
        ApprovalTemplateSystem approvalTemplateSystem = new ApprovalTemplateSystem();
        approvalTemplateSystem.setTemplateId(templateId);
        approvalTemplateSystem.setSystemCode(systemCode);
        approvalTemplateSystem.setCallbackUrl(callbackUrl);

        templateSystemService.save(approvalTemplateSystem);

        log.info("add template: {}", "新增审批模板成功");
        CommonResult<String> success = CommonResult.success("新增审批模板成功!");
        return success;
    }
}
