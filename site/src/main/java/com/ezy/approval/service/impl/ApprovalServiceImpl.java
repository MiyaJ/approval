package com.ezy.approval.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.ezy.approval.service.IApprovalService;
import com.ezy.approval.service.RedisService;
import com.ezy.approval.utils.OkHttpClientUtil;
import com.ezy.common.enums.RedisConstans;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Caixiaowei
 * @ClassName ApprovalServiceImpl.java
 * @Description 审批应用接口实现
 * @createTime 2020年07月27日 15:18:00
 */
@Service
@Slf4j
public class ApprovalServiceImpl extends WxWorkServiceImpl implements IApprovalService {

    @Value("${qywx.approval-corpsecret}")
    private String APPROVAL_SECRET;

    @Autowired
    private RedisService redisService;

    /**
     * 获取审批应用token
     *
     * @return String
     * @description 每个应用有独立的secret，获取到的access_token只能本应用使用，所以每个应用的access_token应该分开来获取
     * @author Caixiaowei
     * @updateTime 2020/7/27 15:19
     */
    @Override
    public String getAccessToken() {
        String accessToken = String.valueOf(redisService.get(RedisConstans.QYWX_ACCESS_TOKEN_KEY_APPROVAL));
        if (StrUtil.isEmpty(accessToken)) {
            try {
                accessToken = super.getAccessToken(this.APPROVAL_SECRET);
                if (StringUtils.isNotBlank(accessToken)) {
                    redisService.set(RedisConstans.QYWX_ACCESS_TOKEN_KEY_APPROVAL, accessToken, RedisConstans.QYWX_ACCESS_TOKEN_EXPIRATION);
                }
            } catch (Exception e) {
                log.error("获取审批应用access_token 异常--->{}", e);
            }

        }
        return accessToken;
    }

    /**
     * 获取审批模板详情
     *
     * @param templateId : String 模板id
     * @return JSONObject
     * @description 企业可通过审批应用或自建应用Secret调用本接口，获取企业微信“审批应用”内指定审批模板的详情
     * @author Caixiaowei
     * @updateTime 2020/7/27 16:20
     */
    @Override
    public JSONObject getTemplateDetail(String templateId) {
        String url = "https://qyapi.weixin.qq.com/cgi-bin/oa/gettemplatedetail?access_token=" + this.getAccessToken();
        Map<String, Object> params = new HashMap<>();
        params.put("template_id", templateId);
        String result = OkHttpClientUtil.doPost(url, null, params);
        JSONObject data = JSONObject.parseObject(result);
        return data;
    }
}
