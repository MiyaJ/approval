package com.ezy.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ezy.approval.model.apply.ApprovalErrorListVO;
import com.ezy.approval.model.apply.ApprovalQueryDTO;
import com.ezy.approval.service.IApprovalApplyService;
import com.ezy.common.model.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Caixiaowei
 * @ClassName ErrorController
 * @Description 异常单据
 * @createTime 2020/9/22$ 10:50$
 */
@RestController
@RequestMapping("/error")
public class ErrorController {

    @Autowired
    private IApprovalApplyService approvalApplyService;

    @GetMapping("/list")
    public CommonResult list(ApprovalQueryDTO approvalQueryDTO) {
        IPage<ApprovalErrorListVO> page = approvalApplyService.errorList(approvalQueryDTO);
        return CommonResult.success(page);
    }

}
