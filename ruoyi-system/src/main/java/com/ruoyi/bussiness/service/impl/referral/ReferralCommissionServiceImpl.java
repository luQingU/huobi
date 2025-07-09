package com.ruoyi.bussiness.service.impl.referral;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.bussiness.domain.TAppUser;
import com.ruoyi.bussiness.domain.TAppAsset;
import com.ruoyi.bussiness.domain.TAppWalletRecord;
import com.ruoyi.bussiness.domain.TMineOrder;
import com.ruoyi.bussiness.domain.referral.TReferralCommissionRecord;
import com.ruoyi.bussiness.domain.referral.TReferralCommissionConfig;
import com.ruoyi.bussiness.mapper.referral.TReferralCommissionRecordMapper;
import com.ruoyi.bussiness.mapper.referral.TReferralCommissionConfigMapper;
import com.ruoyi.bussiness.service.ITAppUserService;
import com.ruoyi.bussiness.service.ITAppAssetService;
import com.ruoyi.bussiness.service.ITAppWalletRecordService;
import com.ruoyi.bussiness.service.ITMineOrderService;
import com.ruoyi.bussiness.service.referral.IReferralCommissionService;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 佣金计算和分发服务实现
 * 
 * @author ruoyi
 * @date 2025-01-09
 */
@Slf4j
@Service
public class ReferralCommissionServiceImpl extends ServiceImpl<TReferralCommissionRecordMapper, TReferralCommissionRecord> 
        implements IReferralCommissionService {

    @Resource
    private ITAppUserService appUserService;

    @Resource
    private ITAppAssetService appAssetService;

    @Resource
    private ITAppWalletRecordService walletRecordService;

    @Resource
    private ITMineOrderService mineOrderService;

    @Resource
    private TReferralCommissionRecordMapper commissionRecordMapper;

    @Resource
    private TReferralCommissionConfigMapper commissionConfigMapper;

    // 佣金状态常量
    private static final String COMMISSION_STATUS_PENDING = "pending";
    private static final String COMMISSION_STATUS_PAID = "paid";
    private static final String COMMISSION_STATUS_FAILED = "failed";
    private static final String COMMISSION_STATUS_EXPIRED = "expired";
    private static final String COMMISSION_STATUS_CANCELLED = "cancelled";

    // 订单状态常量
    private static final Long ORDER_STATUS_PAID = 1L;
    private static final String USER_STATUS_ACTIVE = "0";
    private static final String DEFAULT_CURRENCY = "USDT";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean calculateAndDistributeCommission(TMineOrder order) {
        log.info("开始计算和分发佣金，订单ID: {}", order.getId());
        
        try {
            // 1. 验证订单状态
            if (!validateOrder(order)) {
                log.warn("订单验证失败，订单ID: {}", order.getId());
                return false;
            }

            // 2. 获取购买用户信息
            TAppUser buyerUser = appUserService.getById(order.getUserId());
            if (buyerUser == null) {
                log.warn("购买用户不存在，用户ID: {}", order.getUserId());
                return false;
            }

            // 3. 获取推荐关系链
            List<Long> referralChain = getUserReferralChain(buyerUser.getUserId());
            if (referralChain.isEmpty()) {
                log.info("用户无推荐关系，用户ID: {}", buyerUser.getUserId());
                markOrderAsProcessed(order);
                return true;
            }

            // 4. 获取佣金配置
            List<TReferralCommissionConfig> configs = getActiveCommissionConfigs();
            if (configs.isEmpty()) {
                log.warn("无激活的佣金配置");
                return false;
            }

            // 5. 计算和分发佣金
            List<TReferralCommissionRecord> commissionRecords = new ArrayList<>();
            for (int i = 0; i < Math.min(referralChain.size(), configs.size()); i++) {
                Long referrerUserId = referralChain.get(i);
                TReferralCommissionConfig config = configs.get(i);
                
                // 检查推荐人状态
                TAppUser referrerUser = appUserService.getById(referrerUserId);
                if (referrerUser == null || !USER_STATUS_ACTIVE.equals(referrerUser.getStatus())) {
                    log.warn("推荐人状态异常，用户ID: {}", referrerUserId);
                    continue;
                }

                // 创建佣金记录
                TReferralCommissionRecord record = createCommissionRecord(order, buyerUser, referrerUser, config);
                commissionRecords.add(record);

                // 更新用户资产
                updateUserAsset(referrerUser, config.getAmount(), config.getCurrency());

                // 生成钱包记录
                generateWalletRecord(referrerUser, config.getAmount(), config.getCurrency(), order, config.getLevel());

                log.info("佣金分发成功，接收人: {}, 级别: {}, 金额: {}", 
                        referrerUser.getUserId(), config.getLevel(), config.getAmount());
            }

            // 6. 批量保存佣金记录
            if (!commissionRecords.isEmpty()) {
                for (TReferralCommissionRecord record : commissionRecords) {
                    commissionRecordMapper.insert(record);
                }
            }

            // 7. 标记订单佣金已处理
            markOrderAsProcessed(order);

            log.info("佣金计算和分发完成，订单ID: {}, 分发记录数: {}", 
                    order.getId(), commissionRecords.size());
            
            return true;

        } catch (Exception e) {
            log.error("佣金计算和分发失败，订单ID: {}", order.getId(), e);
            throw new RuntimeException("佣金计算和分发失败", e);
        }
    }

    @Override
    public Map<String, Object> calculateCommission(TMineOrder order) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 验证订单状态
            if (!validateOrder(order)) {
                result.put("success", false);
                result.put("message", "订单状态异常");
                return result;
            }

            // 2. 获取购买用户信息
            TAppUser buyerUser = appUserService.getById(order.getUserId());
            if (buyerUser == null) {
                result.put("success", false);
                result.put("message", "购买用户不存在");
                return result;
            }

            // 3. 获取推荐关系链
            List<Long> referralChain = getUserReferralChain(buyerUser.getUserId());
            if (referralChain.isEmpty()) {
                result.put("success", true);
                result.put("message", "用户无推荐关系");
                result.put("commissionRecords", new ArrayList<>());
                result.put("totalAmount", BigDecimal.ZERO);
                return result;
            }

            // 4. 获取佣金配置
            List<TReferralCommissionConfig> configs = getActiveCommissionConfigs();
            if (configs.isEmpty()) {
                result.put("success", false);
                result.put("message", "无激活的佣金配置");
                return result;
            }

            // 5. 计算佣金
            List<Map<String, Object>> commissionDetails = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            for (int i = 0; i < Math.min(referralChain.size(), configs.size()); i++) {
                Long referrerUserId = referralChain.get(i);
                TReferralCommissionConfig config = configs.get(i);
                
                // 检查推荐人状态
                TAppUser referrerUser = appUserService.getById(referrerUserId);
                if (referrerUser == null || !USER_STATUS_ACTIVE.equals(referrerUser.getStatus())) {
                    continue;
                }

                Map<String, Object> detail = new HashMap<>();
                detail.put("level", config.getLevel());
                detail.put("referrerUserId", referrerUserId);
                detail.put("referrerUsername", referrerUser.getUsername());
                detail.put("commissionAmount", config.getAmount());
                detail.put("currency", config.getCurrency());
                
                commissionDetails.add(detail);
                totalAmount = totalAmount.add(config.getAmount());
            }

            result.put("success", true);
            result.put("message", "计算成功");
            result.put("commissionRecords", commissionDetails);
            result.put("totalAmount", totalAmount);
            result.put("currency", DEFAULT_CURRENCY);
            
            return result;

        } catch (Exception e) {
            log.error("佣金计算失败，订单ID: {}", order.getId(), e);
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public List<TReferralCommissionRecord> getUserCommissionRecords(Long userId, Date startDate, Date endDate) {
        try {
            QueryWrapper<TReferralCommissionRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("recipient_user_id", userId);
            
            if (startDate != null) {
                wrapper.ge("created_at", startDate);
            }
            if (endDate != null) {
                wrapper.le("created_at", endDate);
            }
            
            wrapper.orderByDesc("created_at");
            
            return commissionRecordMapper.selectList(wrapper);
        } catch (Exception e) {
            log.error("查询用户佣金记录失败，用户ID: {}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> getUserCommissionStatistics(Long userId, Date startDate, Date endDate) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询用户佣金记录
            List<TReferralCommissionRecord> records = getUserCommissionRecords(userId, startDate, endDate);
            
            // 计算统计信息
            BigDecimal totalEarned = records.stream()
                .filter(r -> COMMISSION_STATUS_PAID.equals(r.getStatus()))
                .map(TReferralCommissionRecord::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            BigDecimal pendingAmount = records.stream()
                .filter(r -> COMMISSION_STATUS_PENDING.equals(r.getStatus()))
                .map(TReferralCommissionRecord::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            BigDecimal failedAmount = records.stream()
                .filter(r -> COMMISSION_STATUS_FAILED.equals(r.getStatus()))
                .map(TReferralCommissionRecord::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 按级别统计
            Map<Integer, BigDecimal> levelStats = records.stream()
                .filter(r -> COMMISSION_STATUS_PAID.equals(r.getStatus()))
                .collect(Collectors.groupingBy(
                    TReferralCommissionRecord::getCommissionLevel,
                    Collectors.mapping(
                        TReferralCommissionRecord::getCommissionAmount,
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                    )
                ));

            result.put("totalEarned", totalEarned);
            result.put("pendingAmount", pendingAmount);
            result.put("failedAmount", failedAmount);
            result.put("commissionsByLevel", levelStats);
            result.put("recordCount", records.size());
            result.put("currency", DEFAULT_CURRENCY);
            
            return result;
            
        } catch (Exception e) {
            log.error("查询用户佣金统计失败，用户ID: {}", userId, e);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    public Map<Integer, BigDecimal> getUserCommissionByLevel(Long userId, Date startDate, Date endDate) {
        try {
            List<TReferralCommissionRecord> records = getUserCommissionRecords(userId, startDate, endDate);
            
            return records.stream()
                .filter(r -> COMMISSION_STATUS_PAID.equals(r.getStatus()))
                .collect(Collectors.groupingBy(
                    TReferralCommissionRecord::getCommissionLevel,
                    Collectors.mapping(
                        TReferralCommissionRecord::getCommissionAmount,
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                    )
                ));
        } catch (Exception e) {
            log.error("查询用户按级别佣金失败，用户ID: {}", userId, e);
            return new HashMap<>();
        }
    }

    @Override
    public List<Long> getUserReferralChain(Long userId) {
        try {
            TAppUser user = appUserService.getById(userId);
            if (user == null || StringUtils.isEmpty(user.getAppParentIds())) {
                return new ArrayList<>();
            }
            
            String[] parentIds = user.getAppParentIds().split(",");
            List<Long> referralChain = new ArrayList<>();
            
            // 逆序添加（最近的推荐人在前）
            for (int i = parentIds.length - 1; i >= 0; i--) {
                String parentId = parentIds[i].trim();
                if (StringUtils.isNotEmpty(parentId)) {
                    try {
                        referralChain.add(Long.parseLong(parentId));
                    } catch (NumberFormatException e) {
                        log.warn("无效的推荐人ID: {}", parentId);
                    }
                }
            }
            
            return referralChain;
        } catch (Exception e) {
            log.error("获取用户推荐关系链失败，用户ID: {}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isOrderCommissionProcessed(Long orderId) {
        try {
            TMineOrder order = mineOrderService.getById(orderId);
            return order != null && order.getCommissionProcessed();
        } catch (Exception e) {
            log.error("检查订单佣金状态失败，订单ID: {}", orderId, e);
            return false;
        }
    }

    @Override
    public TReferralCommissionRecord getCommissionRecordDetail(Long recordId) {
        try {
            return commissionRecordMapper.selectById(recordId);
        } catch (Exception e) {
            log.error("查询佣金记录详情失败，记录ID: {}", recordId, e);
            return null;
        }
    }

    // 私有方法
    
    private boolean validateOrder(TMineOrder order) {
        if (order == null) {
            return false;
        }
        
        // 检查订单状态
        if (!ORDER_STATUS_PAID.equals(order.getStatus())) {
            return false;
        }
        
        // 检查是否已处理佣金
        if (order.getCommissionProcessed()) {
            return false;
        }
        
        return true;
    }

    private List<TReferralCommissionConfig> getActiveCommissionConfigs() {
        QueryWrapper<TReferralCommissionConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("active", true);
        wrapper.orderByAsc("level");
        return commissionConfigMapper.selectList(wrapper);
    }

    private TReferralCommissionRecord createCommissionRecord(TMineOrder order, TAppUser buyerUser, 
                                                           TAppUser referrerUser, TReferralCommissionConfig config) {
        TReferralCommissionRecord record = new TReferralCommissionRecord();
        record.setOrderId(order.getId());
        record.setRecipientUserId(referrerUser.getUserId());
        record.setPayerUserId(buyerUser.getUserId());
        record.setCommissionAmount(config.getAmount());
        record.setCommissionLevel(config.getLevel());
        record.setCommissionRate(config.getAmount());
        record.setStatus(COMMISSION_STATUS_PAID);
        record.setCurrency(config.getCurrency());
        record.setCreatedAt(DateUtils.getNowDate());
        record.setProcessedAt(DateUtils.getNowDate());
        record.setReferralPath(buyerUser.getAppParentIds());
        record.setVersion(1L);
        record.setDeleted(0);
        
        return record;
    }

    private void updateUserAsset(TAppUser user, BigDecimal amount, String currency) {
        appAssetService.updateUserAsset(user.getUserId(), currency, amount);
    }

    private void generateWalletRecord(TAppUser user, BigDecimal amount, String currency, 
                                    TMineOrder order, Integer level) {
        TAppWalletRecord record = new TAppWalletRecord();
        record.setUserId(user.getUserId());
        record.setAmount(amount);
        record.setSymbol(currency);
        record.setType("COMMISSION_REWARD");
        record.setRemark(String.format("L%d推荐佣金奖励，订单号: %s", level, order.getId()));
        record.setCreatedTime(DateUtils.getNowDate());
        
        walletRecordService.generateRecord(record);
    }

    private void markOrderAsProcessed(TMineOrder order) {
        order.setCommissionProcessed(true);
        mineOrderService.updateById(order);
    }

    // 其他方法的默认实现
    
    @Override
    public boolean reprocessFailedCommission(Long recordId) {
        // TODO: 实现重新处理失败佣金的逻辑
        return false;
    }

    @Override
    public int processPendingCommissions(int limit) {
        // TODO: 实现批量处理待处理佣金的逻辑
        return 0;
    }

    @Override
    public int processExpiredCommissions(Date expireDate) {
        // TODO: 实现处理过期佣金的逻辑
        return 0;
    }

    @Override
    public boolean validateCommissionDataConsistency(Long orderId) {
        // TODO: 实现数据一致性验证逻辑
        return true;
    }

    @Override
    public Map<String, Object> getSystemCommissionStatistics() {
        // TODO: 实现系统佣金统计逻辑
        return new HashMap<>();
    }

    @Override
    public boolean cancelCommissionRecord(Long recordId, String reason) {
        // TODO: 实现取消佣金记录逻辑
        return false;
    }
}