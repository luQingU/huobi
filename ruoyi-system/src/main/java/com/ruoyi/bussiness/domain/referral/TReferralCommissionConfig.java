package com.ruoyi.bussiness.domain.referral;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 佣金配置对象 t_referral_commission_config
 * 
 * @author ruoyi
 * @date 2025-01-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_referral_commission_config")
public class TReferralCommissionConfig extends BaseEntity {
    
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 层级（1-一级，2-二级，3-三级） */
    @Excel(name = "层级")
    private Integer level;

    /** 佣金金额 */
    @Excel(name = "佣金金额")
    private BigDecimal amount;

    /** 货币类型 */
    @Excel(name = "货币类型")
    private String currency;

    /** 是否启用（true-启用，false-停用） */
    @Excel(name = "是否启用")
    private Boolean active;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "更新时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;

    /** 备注信息 */
    @Excel(name = "备注信息")
    private String remarks;

    /** 数据版本号（乐观锁） */
    private Long version;

    /** 是否删除（0-未删除，1-已删除） */
    private Integer deleted;

    /** 最小订单金额（达到该金额才能触发佣金） */
    @Excel(name = "最小订单金额")
    private BigDecimal minOrderAmount;

    /** 最大佣金金额（单次佣金上限） */
    @Excel(name = "最大佣金金额")
    private BigDecimal maxCommissionAmount;

    /** 有效期开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "有效期开始时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date validFrom;

    /** 有效期结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "有效期结束时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date validTo;

    /** 排序顺序 */
    @Excel(name = "排序顺序")
    private Integer sortOrder;
}