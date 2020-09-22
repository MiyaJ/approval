package com.ezy.approval.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezy.approval.entity.*;
import com.ezy.approval.mapper.ApprovalApplyMapper;
import com.ezy.approval.model.apply.*;
import com.ezy.approval.model.sys.EmpInfo;
import com.ezy.approval.service.*;
import com.ezy.approval.utils.DateUtil;
import com.ezy.common.enums.ApprovalStatusEnum;
import com.ezy.common.model.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  审批申请记录 服务实现类
 * </p>
 *
 * @author CaiXiaowei
 * @since 2020-07-27
 */
@Service
@Slf4j
public class ApprovalApplyServiceImpl extends ServiceImpl<ApprovalApplyMapper, ApprovalApply> implements IApprovalApplyService {

    @Autowired
    private IApprovalTemplateService templateService;
    @Autowired
    private IApprovalTemplateSystemService templateSystemService;
    @Autowired
    private IApprovalService approvalService;
    @Autowired
    private IApprovalSpNotifyerService spNotifyerService;
    @Autowired
    private IApprovalSpRecordService spRecordService;
    @Autowired
    private IApprovalSpCommentService spCommentService;
    @Autowired
    private IApprovalSpRecordDetailService spRecordDetailService;
    @Autowired
    private ICommonService commonService;
    @Autowired
    private ApprovalApplyMapper approvalApplyMapper;
    

    /**
     * 审批申请
     *
     * @param approvalApplyDTO ApprovalApplyDTO 申请数据
     * @return
     * @description
     * @author Caixiaowei
     * @updateTime 2020/7/29 15:19
     */
    @Override
    public CommonResult apply(ApprovalApplyDTO approvalApplyDTO) {

        String templateId = approvalApplyDTO.getTemplateId();
        String systemCode = approvalApplyDTO.getSystemCode();
        Integer useTemplateApprover = approvalApplyDTO.getUseTemplateApprover();

        // 校验模板是否注册
        QueryWrapper<ApprovalTemplateSystem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("template_id", templateId)
                .eq("system_code", systemCode);
        ApprovalTemplateSystem templateSystem = templateSystemService.getOne(queryWrapper);
        if (templateSystem == null) {
            return CommonResult.failed("没有绑定这个模板, 请联系管理员.");
        }
        String callbackUrl = templateSystem.getCallbackUrl();

        ApprovalTemplate template = templateService.getByTemplateId(templateId);
        Boolean isEnable = template.getIsEnable();
        if (!isEnable) {
            return CommonResult.failed("模板已停用, 请联系管理员!");
        }

        ApprovalApply apply = new ApprovalApply();
        apply.setTemplateId(templateId);
        apply.setSystemCode(systemCode);
        apply.setSpName(template.getTemplateName());
        apply.setStatus(ApprovalStatusEnum.IN_REVIEW.getStatus());
        apply.setUseTemplateApprover(useTemplateApprover);
        apply.setCallbackUrl(callbackUrl);
        // TODO: 2020/7/29 申请人员工信息
        EmpInfo empInfo = commonService.getEmpByUserId("xiaowei");
        apply.setEmpId(empInfo.getEmpId());
        apply.setEmpName(empInfo.getEmpName());
        apply.setWxUserId(empInfo.getQwUserId());
        // TODO: 2020/7/29 转换审批数据
        apply.setApplyData(JSONObject.toJSONString(approvalApplyDTO.getApplyData()));
        apply.setApplyTime(DateUtil.localDateTimeToSecond(LocalDateTime.now()));
        apply.setCreateTime(LocalDateTime.now());
        this.save(apply);

        // 调用企业微信, 发起审批申请
        JSONObject event = approvalService.applyEvent(approvalApplyDTO);
        int errcode = event.getIntValue("errcode");
        String errmsg = event.getString("errmsg");
        String spNo = event.getString("sp_no");
        if (errcode != 0) {
            // 申请失败
            apply.setStatus(ApprovalStatusEnum.FAIL.getStatus());
            apply.setErrorReason(errmsg);
            apply.setSpNo(spNo);
            this.updateById(apply);

            return CommonResult.failed(errmsg);
        }
        // 更新审批单号等信息
        apply.setErrorReason(errmsg);
        apply.setSpNo(spNo);
        this.updateById(apply);

        return CommonResult.success("审批申请成功!");
    }

    /**
     * 根据单号查询审批单据
     *
     * @param systemCode string 调用方系统标识
     * @param spNo string 审批单号
     * @return
     * @description
     * @author Caixiaowei
     * @updateTime 2020/7/31 11:28
     */
    @Override
    public CommonResult detail(String systemCode, String spNo) {
        ApprovalApply apply = getApprovalApply(spNo);

        if (apply == null ) {
            return CommonResult.failed("审批单据不存在, 请检查审批单号");
        }
//        String applySystemCode = apply.getSystemCode();
//        if (StrUtil.isEmpty(systemCode) || !systemCode.equals(applySystemCode)) {
//            return CommonResult.failed("无权限查看此审批单据");
//        }

        // 查询审批单据抄送人
        List<SpNotifyerVO> spNotifyerVOS = getSpNotifyerBySpNo(spNo);

        // 查询审批单据流程
        List<ApprovalSpRecord> spRecords = getSpRecordBySpNo(spNo);

        // 查询审批备注
        List<ApprovalSpComment> spComments = getSpCommentsBySpNo(spNo);

        ApprovalDetailVO detailVO = new ApprovalDetailVO();
        BeanUtil.copyProperties(apply, detailVO);

        detailVO.setSpRecords(spRecords);
        detailVO.setNotifyers(spNotifyerVOS);
        detailVO.setSpComments(spComments);

        return CommonResult.success(detailVO);
    }

    @Override
    public ApprovalApply getApprovalApply(String spNo) {
        QueryWrapper<ApprovalApply> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sp_no", spNo);
        return this.getOne(queryWrapper);
    }

    /**
     * 异常审批单列表
     *
     * @param approvalQueryDTO ApprovalQueryDTO 查询参数
     * @return
     * @author Caixiaowei
     * @updateTime 2020/9/22 16:36
     */
    @Override
    public IPage<ApprovalErrorListVO> errorList(ApprovalQueryDTO approvalQueryDTO) {
        IPage<ApprovalErrorListVO> page = new Page<>();
        page.setPages(approvalQueryDTO.getPageNum());
        page.setSize(approvalQueryDTO.getPageSize());

        approvalQueryDTO.setCallbackStatus(ApprovalApply.CALL_BACK_FAIL);
        page = approvalApplyMapper.pageErrorList(page, approvalQueryDTO);
        log.info("data --->{}", JSONObject.toJSONString(page));
        return page;
    }

    /**
     * 查询系统应用的审批单
     *
     * @param systemCode string 系统标识
     * @param startDate string 开始日期
     * @param endDate string 结束日期
     * @return
     * @description
     * @author Caixiaowei
     * @updateTime 2020/8/3 9:33
     */
    @Override
    @GetMapping("/listBySystemCode")
    public CommonResult listBySystemCode(String systemCode, String startDate, String endDate) {
        QueryWrapper<ApprovalApply> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("system_code", systemCode);
        queryWrapper.ge(StrUtil.isNotEmpty(startDate), "create_time", startDate);
        queryWrapper.le(StrUtil.isNotEmpty(endDate), "create_time", endDate);
        List<ApprovalApply> approvalApplyList = this.list(queryWrapper);
        List<ApprovalDetailVO> list = approvalApplyList.stream().map(a -> {
            ApprovalDetailVO vo = new ApprovalDetailVO();
            BeanUtil.copyProperties(a, vo);
            return vo;
        }).collect(Collectors.toList());
        return CommonResult.success(list);
    }

    /********************** 私有方法 ********************************/

    /**
     * 查询审批单据的抄送人
     * @description
     * @author Caixiaowei
     * @param spNo String 审批单号
     * @updateTime 2020/7/31 13:47
     * @return List<ApprovalSpNotifyer>
     */
    private List<SpNotifyerVO> getSpNotifyerBySpNo(String spNo) {
        QueryWrapper<ApprovalSpNotifyer> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sp_no", spNo);
        List<ApprovalSpNotifyer> spNotifyers = spNotifyerService.list(queryWrapper);
        List<SpNotifyerVO> spNotifyerVOS = spNotifyers.stream().map(n -> {
            SpNotifyerVO vo = new SpNotifyerVO();
            BeanUtil.copyProperties(n, vo);
            return vo;
        }).collect(Collectors.toList());
        return spNotifyerVOS;

    }

    /**
     * 单号查询审批单据流程节点信息
     * @description
     * @author Caixiaowei
     * @param spNo string 审批单号
     * @updateTime 2020/7/31 17:07
     * @return List<ApprovalSpRecord>
     */
    private List<ApprovalSpRecord> getSpRecordBySpNo(String spNo) {
        QueryWrapper<ApprovalSpRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sp_no", spNo);
        List<ApprovalSpRecord> list = spRecordService.list(queryWrapper);
        if (CollectionUtil.isNotEmpty(list)) {
            for (ApprovalSpRecord approvalSpRecord : list) {
                Long spRecordId = approvalSpRecord.getId();
                QueryWrapper<ApprovalSpRecordDetail> queryWrapper2 = new QueryWrapper<>();
                queryWrapper2.eq("sp_record_id", spRecordId);
                List<ApprovalSpRecordDetail> recordDetails = spRecordDetailService.list(queryWrapper2);

                approvalSpRecord.setRecordDetails(recordDetails);
            }
        }

        return list;
    }

    /**
     * 根据审批单号查询备注信息
     *
     * @param spNo 审批单编号
     * @return List<ApprovalSpComment>
     * @author Caixiaowei
     * @updateTime 2020/9/21 14:14
     */
    private List<ApprovalSpComment> getSpCommentsBySpNo(String spNo) {
        QueryWrapper<ApprovalSpComment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sp_no", spNo);
        queryWrapper.eq("is_deleted", false);
        queryWrapper.orderByAsc("comment_time");
        List<ApprovalSpComment> list = spCommentService.list(queryWrapper);
        return list;
    }
}
