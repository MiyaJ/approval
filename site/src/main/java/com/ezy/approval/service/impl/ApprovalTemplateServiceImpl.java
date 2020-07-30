package com.ezy.approval.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ezy.approval.entity.ApprovalTemplate;
import com.ezy.approval.entity.ApprovalTemplateSystem;
import com.ezy.approval.mapper.ApprovalTemplateMapper;
import com.ezy.approval.model.apply.ApplyDataContent;
import com.ezy.approval.model.template.*;
import com.ezy.approval.service.IApprovalService;
import com.ezy.approval.service.IApprovalTemplateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezy.approval.service.IApprovalTemplateSystemService;
import com.ezy.approval.utils.OkHttpClientUtil;
import com.ezy.common.enums.ApprovalControlEnum;
import com.ezy.common.model.CommonResult;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
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

        // 验证是否已存在
        QueryWrapper<ApprovalTemplate> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("template_id", templateId);
        List<ApprovalTemplate> list = this.list(queryWrapper);
        if (CollectionUtil.isNotEmpty(list)) {
            return CommonResult.failed("此审批模板已存在!");
        }

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
//        List<TemplateControl> templateControls = buildTemplateRequestParams(templateContent);
//        String requestParam = JSONObject.toJSONString(templateControls);

        List<ApplyDataContent> applyDataContents = buildTemplateRequestParams2(templateContent);
        String requestParam = JSONObject.toJSONString(applyDataContents);


        template.setTemplateId(templateId);
        template.setTemplateName(templateName);
        template.setIsDeleted(0);
        template.setContent(JSONObject.toJSONString(templateContent));
        template.setPatternImage(patternImage);
        template.setIsEnable(true);
        // TODO: 2020/7/27 模板出入参待完善
        template.setRequestParam(requestParam);
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

    /**
     * 查询审批模板详情
     *
     * @param templateId : string 模板id
     * @param systemCode : string 调用方系统标识
     * @return TemplateDetailVO
     * @description 需要先校验调用方是否注册次模板
     * @author Caixiaowei
     * @updateTime 2020/7/29 13:51
     */
    @Override
    public CommonResult detail(String templateId, String systemCode) {
        TemplateDetailVO detailVO = new TemplateDetailVO();

        // 校验调用方是否注册次模板
        QueryWrapper<ApprovalTemplateSystem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("template_id", templateId)
                .eq("system_code", systemCode);
        ApprovalTemplateSystem one = templateSystemService.getOne(queryWrapper);
        if (one == null) {
            return CommonResult.success("没有注册这个模板, 请联系管理员.");
        }

        ApprovalTemplate template = getByTemplateId(templateId);
        // 校验模板
        String errMsg = checkTemplate(template);
        if (StrUtil.isNotEmpty(errMsg)) {
            return CommonResult.failed(errMsg);
        }

        // build 模板详情
        detailVO = TemplateDetailVO.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getTemplateName())
                .description(template.getDescription())
                .patternImage(template.getPatternImage())
                .build();
        String requestParam = template.getRequestParam();
        List<TemplateControl> templateControls = JSONObject.parseArray(requestParam, TemplateControl.class);
        detailVO.setRequestParam(templateControls);

        return CommonResult.success(detailVO);
    }

    /*********************************** 私有方法 *************************************/

    /**
     * 构建模板请求参数
     * @description
     * @author Caixiaowei
     * @param templateContent: JSONObject 控件信息
     * @updateTime 2020/7/29 11:01
     * @return
     */
    private List<TemplateControl> buildTemplateRequestParams(JSONObject templateContent) {
        List<TemplateControl> controlList = Lists.newArrayList();
        // 所有控件
        JSONArray controls = templateContent.getJSONArray("controls");
        if (controls != null && controls.size() > 0) {
            for (Object controlObject : controls) {
                JSONObject controlJson = (JSONObject) controlObject;
                JSONObject property = controlJson.getJSONObject("property");
                JSONObject config = controlJson.getJSONObject("config");

                TemplateControl templateControl = new TemplateControl();
                TemplateControlProperty controlProperty = new TemplateControlProperty();
                // 控件信息
                if (property != null) {
                    String control = property.getString("control");
                    String id = property.getString("id");
                    JSONObject titleJson = property.getJSONArray("title").getJSONObject(0);
                    String title = titleJson.getString("text");
                    JSONObject placeholderJson = property.getJSONArray("placeholder").getJSONObject(0);
                    String placeholder = placeholderJson.getString("text");
                    Integer require = property.getInteger("require");
                    Integer unPrint = property.getInteger("un_print");

                    controlProperty = TemplateControlProperty.builder()
                            .control(control)
                            .id(id)
                            .title(title)
                            .placeholder(placeholder)
                            .require(require)
                            .unPrint(unPrint)
                            .build();

                    templateControl.setProperty(controlProperty);
                }

                // 模板控件配置, 包含了部分控件类型的附加类型、属性. 目前有配置信息的控件类型有：
                // Date-日期/日期+时间；Selector-单选/多选；Contact-成员/部门；Table-明细；Attendance-假勤组件（请假、外出、出差、加班）
                if (config != null) {
                    templateControl.setConfig(config);
                }
                controlList.add(templateControl);
            }
        }
        return controlList;
    }

    /**
     * 构建审批内容参数
     * @description
     * @author Caixiaowei
     * @param templateContent: JSONObject 内容组件
     * @updateTime 2020/7/30 13:50
     * @return List<ApplyDataContent>
     */
    private List<ApplyDataContent> buildTemplateRequestParams2(JSONObject templateContent) {
        List<ApplyDataContent> contents = Lists.newArrayList();
        // 所有控件
        JSONArray controls = templateContent.getJSONArray("controls");
        if (controls != null && controls.size() > 0) {
            for (Object controlObject : controls) {
                JSONObject controlJson = (JSONObject) controlObject;
                JSONObject property = controlJson.getJSONObject("property");
                JSONObject config = controlJson.getJSONObject("config");

                // 控件属性
                String control = property.getString("control");
                String id = property.getString("id");
                List<TextProperty> title = JSONArray.parseArray(property.getString("title"), TextProperty.class);
                JSONObject value = new JSONObject();

                ApplyDataContent content = ApplyDataContent.builder()
                        .control(control)
                        .id(id)
                        .title(title)
                        .build();
                // 根据控件类型来组装 value
                if (control.equalsIgnoreCase(ApprovalControlEnum.TEXT.getControl())
            || control.equalsIgnoreCase(ApprovalControlEnum.TEXTAREA.getControl())) {
                    // 文本控件 , 多行文本
                    value.put("text", "");
                } else if (control.equalsIgnoreCase(ApprovalControlEnum.NUMBER.getControl())) {
                    // 数字控件
                    value.put("new_number", "");
                } else if (control.equalsIgnoreCase(ApprovalControlEnum.MONEY.getControl())) {
                    // 金钱控件
                    value.put("new_money", "");
                } else if (control.equalsIgnoreCase(ApprovalControlEnum.DATE.getControl())) {
                    // 日期/日期+时间控件
                    JSONObject date = config.getJSONObject("date");
                    String type = date.getString("type");
                    date.put("s_timestamp", "");
                    value.put("date", date);
                } else if (control.equalsIgnoreCase(ApprovalControlEnum.DATE_RANGE.getControl())) {
                    // 时长
                    JSONObject dateRange = config.getJSONObject("date_range");
                    Long perday_duration = dateRange.getLong("perday_duration");
                    dateRange.put("new_begin", "");
                    dateRange.put("new_end", "");
                    dateRange.put("new_duration", perday_duration);

                    value.put("date_range", dateRange);

                } else if (control.equalsIgnoreCase(ApprovalControlEnum.SELECTOR.getControl())) {
                    // 选择
                    JSONObject selector = config.getJSONObject("selector");
                    String type = selector.getString("type");

                    JSONObject option = new JSONObject();
                    option.put("key", "");

                    JSONArray optionsValue = new JSONArray();
                    optionsValue.add(new TextProperty());
                    option.put("value", optionsValue);

                    JSONArray options = new JSONArray();
                    options.add(option);

                    JSONObject newSelector = new JSONObject();
                    newSelector.put("type", type);
                    newSelector.put("options", options);

                    value.put("selector", newSelector);
                } else if (control.equalsIgnoreCase(ApprovalControlEnum.CONTACT.getControl())) {
                    // 成员/部门
                    JSONObject selector = config.getJSONObject("contact");
                    String type = selector.getString("type");
                    String mode = selector.getString("mode");
                    if ("user".equalsIgnoreCase(mode)) {
                        // 成员
                        JSONObject member = new JSONObject();
                        member.put("userid", "");
                        member.put("name", "");

                        JSONArray members = new JSONArray();
                        members.add(member);

                        value.put("members", members);
                    } else if ("department".equalsIgnoreCase(mode)) {
                        // 部门
                        JSONObject department = new JSONObject();
                        department.put("openapi_id", "");
                        department.put("name", "");

                        JSONArray departments = new JSONArray();
                        departments.add(department);

                        value.put("departments", departments);
                    }
                } else if (control.equalsIgnoreCase(ApprovalControlEnum.LOCATION.getControl())) {
                    // 位置
                    JSONObject location = new JSONObject();
                    location.put("latitude", "");
                    location.put("longitude", "");
                    location.put("title", "");
                    location.put("address", "");
                    location.put("time", "");

                    value.put("location", location);

                } else if (control.equalsIgnoreCase(ApprovalControlEnum.LOCATION.getControl())) {

                    JSONObject file = new JSONObject();
                    file.put("file_id", "");

                    JSONArray files = new JSONArray();
                    files.add(file);

                    value.put("files", files);
                }

                content.setValue(value);
                contents.add(content);
            }
        }
        return contents;
    }

    /**
     * 根据模板id查询模板
     * @description
     * @author Caixiaowei
     * @param templateId: string 模板id
     * @updateTime 2020/7/29 14:06
     * @return ApprovalTemplate
     */
    @Override
    public ApprovalTemplate getByTemplateId(String templateId) {
        QueryWrapper<ApprovalTemplate> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("template_id", templateId);
        return this.getOne(queryWrapper);
    }

    /**
     * check 模板
     * @description
     * @author Caixiaowei
     * @param template: ApprovalTemplate审批模板
     * @updateTime 2020/7/29 14:38
     * @return String check 信息
     */
    private String checkTemplate(ApprovalTemplate template) {
        String errMsg = StrUtil.EMPTY;
        if (template == null) {
            return "模板不存在, 请联系审批管理";
        }
        Integer isDeleted = template.getIsDeleted();
        if (isDeleted == 1) {
            return "模板已删除, 请联系审批管理";
        }
        Boolean isEnable = template.getIsEnable();
        if (!isEnable) {
            return "模板已删除, 请联系审批管理";
        }
        return errMsg;
    }

}
