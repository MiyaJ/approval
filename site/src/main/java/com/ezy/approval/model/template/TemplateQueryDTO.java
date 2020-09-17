package com.ezy.approval.model.template;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Caixiaowei
 * @ClassName TemplateQueryDTO
 * @Description
 * @createTime 2020/9/16$ 14:13$
 */
@Data
public class TemplateQueryDTO implements Serializable {
    private static final long serialVersionUID = -2594483985795894116L;

    /**
     * 系统编码
     */
    private String systemCode;

    /**
     * 模板id
     */
    private String templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 是否启用
     */
    private Boolean isEnable;
}
