package com.ezy.approval.controller;


import com.ezy.approval.model.message.template.MessageTemplateDTO;
import com.ezy.approval.service.IMessageTemplateService;
import com.ezy.common.model.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 消息模板  前端控制器
 * </p>
 *
 * @author CaiXiaowei
 * @since 2020-07-27
 */
@RestController
@RequestMapping("/messageTemplate")
public class MessageTemplateController {

    @Autowired
    private IMessageTemplateService messageTemplateService;

    @PostMapping
    public CommonResult create(@RequestBody MessageTemplateDTO messageTemplateDTO) {

        return CommonResult.success(null);
    }
}

