package com.ezy.approval.controller;


import cn.hutool.core.util.StrUtil;
import com.ezy.approval.model.apply.ApprovalApplyDTO;
import com.ezy.approval.service.IApprovalApplyService;
import com.ezy.common.model.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  审批申请记录 前端控制器
 * </p>
 *
 * @author CaiXiaowei
 * @since 2020-07-27
 */
@RestController
@RequestMapping("/approval")
public class ApprovalApplyController {

    @Autowired
    private IApprovalApplyService approvalApplyService;


    /**
     * 审批申请
     * @description
     * @author Caixiaowei
     * @param approvalApplyDTO ApprovalApplyDTO 申请数据
     * @updateTime 2020/7/29 15:18
     * @return
     */
    @PostMapping("/apply")
    public CommonResult apply(@RequestBody ApprovalApplyDTO approvalApplyDTO) {
        return approvalApplyService.apply(approvalApplyDTO);
    }
    
    /**
     * 根据单号查询审批单据
     * @description
     * @author Caixiaowei
     * @param spNo string 审批单号
     * @updateTime 2020/7/31 11:26 
     * @return 
     */
    @GetMapping("/detail")
    public CommonResult detail(String systemCode, String spNo) {
        return approvalApplyService.detail(systemCode, spNo);
    }

    /**
     *
     * @description 条件查询审批单据列表
     * @author Caixiaowei
     * @param systemCode string 系统表示
     * @param startDate string 开始日期
     * @param endDate string 结束日期
     * @updateTime 2020/8/3 11:00
     * @return
     */
    @GetMapping("/listBySystemCode")
    public CommonResult listBySystemCode(String systemCode, String startDate, String endDate) {
        if (StrUtil.isEmpty(systemCode)) {
            return CommonResult.failed("参数异常, 请稍后再试!");
        }
        return approvalApplyService.listBySystemCode(systemCode, startDate, endDate);
    }
}
