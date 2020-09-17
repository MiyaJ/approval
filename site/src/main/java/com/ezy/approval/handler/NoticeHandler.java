package com.ezy.approval.handler;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ezy.approval.entity.ApprovalApply;
import com.ezy.approval.entity.ApprovalTemplateSystem;
import com.ezy.approval.model.message.MsgVO;
import com.ezy.approval.model.message.personal.TextMsg;
import com.ezy.approval.service.IApprovalApplyService;
import com.ezy.approval.service.IApprovalTemplateSystemService;
import com.ezy.approval.service.IMessageService;
import com.ezy.approval.utils.OkHttpClientUtil;
import com.ezy.common.enums.ApprovalStatusEnum;
import com.ezy.common.enums.MsgTypeEnum;
import com.ezy.common.model.CommonResult;
import com.ezy.common.model.ResultCode;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Caixiaowei
 * @ClassName NoticeHandler
 * @Description 通知处理器
 * @createTime 2020/9/17$ 10:12$
 */
@Service
@Slf4j
public class NoticeHandler {

    @Value("${qywx.msg-agentid:1000002}")
    private int MESSAGE_AGENT_ID;

    @Autowired
    private IApprovalApplyService approvalApplyService;
    @Autowired
    private IApprovalTemplateSystemService approvalTemplateSystemService;
    @Autowired
    private IMessageService messageService;

    /**
     * 审批结果回调通知调用方
     *
     * @param spNo 审批单编号
     * @return void
     * @author Caixiaowei
     * @updateTime 2020/9/14 15:04
     */
    @Async
    public void approvalResultCallback(String spNo) {
        String callbackUrl = StrUtil.EMPTY;
        String qwContactPerson = StrUtil.EMPTY;
        Integer status = null;

        QueryWrapper<ApprovalApply> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sp_no", spNo);
        ApprovalApply apply = approvalApplyService.getOne(queryWrapper);
        if (apply != null) {
            String templateId = apply.getTemplateId();
            String systemCode = apply.getSystemCode();
            status = apply.getStatus();

            QueryWrapper<ApprovalTemplateSystem> wrapper = new QueryWrapper<>();
            wrapper.eq("system_code", systemCode);
            wrapper.eq("template_id", templateId);
            ApprovalTemplateSystem approvalTemplateSystem = approvalTemplateSystemService.getOne(wrapper);
            if (approvalTemplateSystem != null) {
                callbackUrl = approvalTemplateSystem.getCallbackUrl();
                qwContactPerson = approvalTemplateSystem.getQwContactPerson();
            }
        } else {
            return;
        }

        if (StrUtil.isNotEmpty(callbackUrl) && StrUtil.isNotEmpty(qwContactPerson)) {
            try {
                Map<String, String> params = Maps.newHashMap();
                params.put("status", String.valueOf(status));
                String doGet = OkHttpClientUtil.doGet(callbackUrl, null, params);

                apply.setAck(doGet);
                approvalApplyService.updateById(apply);

                CommonResult commonResult = JSONObject.parseObject(doGet, CommonResult.class);
                if (commonResult == null || commonResult.getCode() != ResultCode.SUCCESS.getCode()) {
                    // 回调失败, 企微通知对应系统联系人
                    TextMsg textMsg = new TextMsg();
                    textMsg.setTouser(qwContactPerson);
                    textMsg.setToparty(StringUtils.EMPTY);
                    textMsg.setTotag(StringUtils.EMPTY);
                    textMsg.setMsgtype(MsgTypeEnum.TEXT.getValue());
                    textMsg.setAgentid(MESSAGE_AGENT_ID);
                    String content = "单据编号: " + spNo + "\n"
                            + "审批结果: " + ApprovalStatusEnum.getDesc(status);
                    textMsg.setText(new TextMsg.TextBean(content));
                    textMsg.setSafe(0);
                    textMsg.setEnable_id_trans(0);
                    textMsg.setEnable_duplicate_check(0);
                    textMsg.setDuplicate_check_interval(1800);

                    MsgVO msgVO = this.messageService.sendTextMsg(textMsg);
                }
            } catch (Exception e) {
                log.error("审批结果回调通知调用方 错误, 审批单号: {}, 异常:{}", spNo, e);
            }
        }
    }
}
