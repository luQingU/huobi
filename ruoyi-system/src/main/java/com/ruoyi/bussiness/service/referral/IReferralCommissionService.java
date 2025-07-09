package com.ruoyi.bussiness.service.referral;

import com.ruoyi.bussiness.domain.TMineOrder;
import com.ruoyi.bussiness.domain.referral.TReferralCommissionRecord;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 佣金计算和分发服务接口
 * 
 * @author ruoyi
 * @date 2025-01-09
 */
public interface IReferralCommissionService {

    /**
     * 计算和分发佣金
     * 
     * @param order 订单信息
     * @return 是否成功
     */
    boolean calculateAndDistributeCommission(TMineOrder order);

    /**
     * 计算佣金金额（不实际分发）
     * 
     * @param order 订单信息
     * @return 佣金计算结果
     */
    Map<String, Object> calculateCommission(TMineOrder order);

    /**
     * 查询用户佣金记录
     * 
     * @param userId 用户ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 佣金记录列表
     */
    List<TReferralCommissionRecord> getUserCommissionRecords(Long userId, Date startDate, Date endDate);

    /**
     * 查询用户佣金统计信息
     * 
     * @param userId 用户ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 佣金统计信息
     */
    Map<String, Object> getUserCommissionStatistics(Long userId, Date startDate, Date endDate);

    /**
     * 查询用户按级别的佣金统计
     * 
     * @param userId 用户ID
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 按级别的佣金统计
     */
    Map<Integer, BigDecimal> getUserCommissionByLevel(Long userId, Date startDate, Date endDate);

    /**
     * 重新处理失败的佣金记录
     * 
     * @param recordId 佣金记录ID
     * @return 是否成功
     */
    boolean reprocessFailedCommission(Long recordId);

    /**
     * 批量处理待处理的佣金记录
     * 
     * @param limit 处理数量限制
     * @return 处理成功数量
     */
    int processPendingCommissions(int limit);

    /**
     * 处理过期的佣金记录
     * 
     * @param expireDate 过期时间
     * @return 处理数量
     */
    int processExpiredCommissions(Date expireDate);

    /**
     * 验证佣金记录的数据一致性
     * 
     * @param orderId 订单ID
     * @return 验证结果
     */
    boolean validateCommissionDataConsistency(Long orderId);

    /**
     * 获取系统佣金统计信息
     * 
     * @return 系统佣金统计信息
     */
    Map<String, Object> getSystemCommissionStatistics();

    /**
     * 获取用户的推荐关系链
     * 
     * @param userId 用户ID
     * @return 推荐关系链
     */
    List<Long> getUserReferralChain(Long userId);

    /**
     * 检查订单是否已处理佣金
     * 
     * @param orderId 订单ID
     * @return 是否已处理
     */
    boolean isOrderCommissionProcessed(Long orderId);

    /**
     * 获取佣金记录详情
     * 
     * @param recordId 佣金记录ID
     * @return 佣金记录详情
     */
    TReferralCommissionRecord getCommissionRecordDetail(Long recordId);

    /**
     * 取消佣金记录（仅限待处理状态）
     * 
     * @param recordId 佣金记录ID
     * @param reason 取消原因
     * @return 是否成功
     */
    boolean cancelCommissionRecord(Long recordId, String reason);
}