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
 * 佣金记录对象 t_referral_commission_record
 * 
 * @author ruoyi
 * @date 2025-01-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_referral_commission_record")
public class TReferralCommissionRecord extends BaseEntity {
    
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 触发订单ID */
    @Excel(name = "触发订单ID")
    private Long orderId;

    /** 接收佣金用户ID */
    @Excel(name = "接收佣金用户ID")
    private Long recipientUserId;

    /** 产生佣金用户ID */
    @Excel(name = "产生佣金用户ID")
    private Long payerUserId;

    /** 佣金金额 */
    @Excel(name = "佣金金额")
    private BigDecimal commissionAmount;

    /** 佣金层级 */
    @Excel(name = "佣金层级")
    private Integer commissionLevel;

    /** 佣金标准（对应层级的固定金额） */
    @Excel(name = "佣金标准")
    private BigDecimal commissionRate;

    /** 佣金状态（pending-待处理，paid-已支付，failed-失败，expired-过期） */
    @Excel(name = "佣金状态")
    private String status;

    /** 交易ID */
    @Excel(name = "交易ID")
    private String transactionId;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "处理时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date processedAt;

    /** 货币类型 */
    @Excel(name = "货币类型")
    private String currency;

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

    /** 推荐路径 */
    @Excel(name = "推荐路径")
    private String referralPath;

    /** 数据版本号（乐观锁） */
    private Long version;

    /** 是否删除（0-未删除，1-已删除） */
    private Integer deleted;
}