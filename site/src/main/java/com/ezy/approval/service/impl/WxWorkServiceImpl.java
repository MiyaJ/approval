package com.ezy.approval.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ezy.approval.service.IWxWorkService;
import com.ezy.approval.utils.OkHttpClientUtil;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Caixiaowei
 * @ClassName WxWorkServiceImpl.java
 * @Description 企业微信接口实现
 * @createTime 2020年06月24日 16:30:00
 */
@Service
@Slf4j
public class WxWorkServiceImpl implements IWxWorkService {

    @Value("${qywx.corpid}")
    private String CORPID;

    private String ACCESS_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";

    /**
     * 获取应用接口token
     *
     * @param corpsecret 应用的凭证密钥
     * @return String 接口凭证token
     * @description
     * @author Caixiaowei
     * @updateTime 2020/6/24 16:37
     */
    @Override
    public String getAccessToken(String corpsecret) {
        String accessToken = StringUtils.EMPTY;
        Map<String, String> params = Maps.newHashMap();
        params.put("corpid", this.CORPID);
        params.put("corpsecret", corpsecret);
        try {
            String result = OkHttpClientUtil.doGet(ACCESS_TOKEN_URL, null, params);
            JSONObject json = JSONObject.parseObject(result);
            if (json.getInteger("errcode") != null && json.getInteger("errcode") == 0) {
                accessToken = json.getString("access_token");
            }
        } catch (Exception e) {
            log.error("获取应用access_token 异常--->{}", e);
        }
        return accessToken;
    }
}
