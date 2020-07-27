package com.ezy.approval.service;

import com.alibaba.fastjson.JSONObject;

/**
 * @author Caixiaowei
 * @ClassName IApprovalService.java
 * @Description 审批应用业务接口
 * @createTime 2020年07月27日 15:17:00
 */
public interface IApprovalService {

    /**
     * 获取审批应用token
     * @description
     * @author Caixiaowei
     * @updateTime 2020/7/27 15:19
     * @return String
     */
    String getAccessToken();

    /**
     * 获取审批模板详情
     * @description 企业可通过审批应用或自建应用Secret调用本接口，获取企业微信“审批应用”内指定审批模板的详情
     * @author Caixiaowei
     * @param templateId: String 模板id
     * @updateTime 2020/7/27 16:20 
     * @return JSONObject
     */
    JSONObject getTemplateDetail(String templateId);
}