package com.ruoyi.bussiness.mapper.referral;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.bussiness.domain.referral.TReferralCommissionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 佣金记录Mapper接口
 * 
 * @author ruoyi
 * @date 2025-01-09
 */
@Mapper
public interface TReferralCommissionRecordMapper extends BaseMapper<TReferralCommissionRecord> {

    /**
     * 根据订单ID查询佣金记录
     * 
     * @param orderId 订单ID
     * @return 佣金记录列表
     */
    List<TReferralCommissionRecord> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据接收用户ID查询佣金记录
     * 
     * @param recipientUserId 接收用户ID
     * @return 佣金记录列表
     */
    List<TReferralCommissionRecord> selectByRecipientUserId(@Param("recipientUserId") Long recipientUserId);

    /**
     * 根据产生佣金用户ID查询佣金记录
     * 
     * @param payerUserId 产生佣金用户ID
     * @return 佣金记录列表
     */
    List<TReferralCommissionRecord> selectByPayerUserId(@Param("payerUserId") Long payerUserId);

    /**
     * 根据状态查询佣金记录
     * 
     * @param status 状态
     * @return 佣金记录列表
     */
    List<TReferralCommissionRecord> selectByStatus(@Param("status") String status);

    /**
     * 根据佣金级别查询佣金记录
     * 
     * @param commissionLevel 佣金级别
     * @return 佣金记录列表
     */
    List<TReferralCommissionRecord> selectByCommissionLevel(@Param("commissionLevel") Integer commissionLevel);

    /**
     * 根据时间范围查询佣金记录
     * 
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 佣金记录列表
     */
    List<TReferralCommissionRecord> selectByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 查询用户的佣金统计信息
     * 
     * @param recipientUserId 接收用户ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 佣金统计信息
     */
    BigDecimal selectCommissionSumByUser(@Param("recipientUserId") Long recipientUserId, 
                                        @Param("startDate") Date startDate, 
                                        @Param("endDate") Date endDate);

    /**
     * 查询用户按级别的佣金统计
     * 
     * @param recipientUserId 接收用户ID
     * @param commissionLevel 佣金级别
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 佣金统计信息
     */
    BigDecimal selectCommissionSumByUserAndLevel(@Param("recipientUserId") Long recipientUserId,
                                               @Param("commissionLevel") Integer commissionLevel,
                                               @Param("startDate") Date startDate,
                                               @Param("endDate") Date endDate);

    /**
     * 查询用户的佣金记录数量
     * 
     * @param recipientUserId 接收用户ID
     * @param status 状态
     * @return 记录数量
     */
    Integer countByUserAndStatus(@Param("recipientUserId") Long recipientUserId, @Param("status") String status);

    /**
     * 批量插入佣金记录
     * 
     * @param records 佣金记录列表
     * @return 插入数量
     */
    Integer insertBatch(@Param("records") List<TReferralCommissionRecord> records);

    /**
     * 批量更新佣金记录状态
     * 
     * @param ids 记录ID列表
     * @param status 新状态
     * @return 更新数量
     */
    Integer updateStatusBatch(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 根据订单ID删除佣金记录
     * 
     * @param orderId 订单ID
     * @return 删除数量
     */
    Integer deleteByOrderId(@Param("orderId") Long orderId);

    /**
     * 查询所有佣金记录
     * 
     * @return 佣金记录列表
     */
    List<TReferralCommissionRecord> selectAll();

    /**
     * 查询系统佣金统计信息
     * 
     * @return 统计信息
     */
    List<java.util.Map<String, Object>> selectCommissionStatistics();

    /**
     * 查询待处理的佣金记录
     * 
     * @param limit 限制数量
     * @return 佣金记录列表
     */
    List<TReferralCommissionRecord> selectPendingRecords(@Param("limit") Integer limit);

    /**
     * 查询过期的佣金记录
     * 
     * @param expireDate 过期时间
     * @return 佣金记录列表
     */
    List<TReferralCommissionRecord> selectExpiredRecords(@Param("expireDate") Date expireDate);
}