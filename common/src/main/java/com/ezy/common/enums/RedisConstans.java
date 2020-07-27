package com.ezy.common.enums;

/**
 * @author Caixiaowei
 * @ClassName RedisConstans.java
 * @Description redis 常量
 * @createTime 2020年06月18日 11:26:00
 */
public class RedisConstans {

    public RedisConstans() {
    }

    public static final Long QYWX_ACCESS_TOKEN_EXPIRATION = 7200L;
    /**
     * 审批应用接口token
     */
    public static final String QYWX_ACCESS_TOKEN_KEY_APPROVAL = "qywx:access_token:approval";
    /**
     * 审批应用接口token
     */
    public static final String QYWX_ACCESS_TOKEN_KEY_MESSAGE = "qywx:access_token:message";
}
