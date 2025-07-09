package com.ruoyi.bussiness.service.impl.referral;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.bussiness.domain.referral.TReferralCommissionConfig;
import com.ruoyi.bussiness.mapper.referral.TReferralCommissionConfigMapper;
import com.ruoyi.bussiness.service.referral.IReferralCommissionConfigService;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 佣金配置管理服务实现
 * 
 * @author ruoyi
 * @date 2025-01-09
 */
@Slf4j
@Service
public class ReferralCommissionConfigServiceImpl extends ServiceImpl<TReferralCommissionConfigMapper, TReferralCommissionConfig>
        implements IReferralCommissionConfigService {

    @Resource
    private TReferralCommissionConfigMapper commissionConfigMapper;

    private static final String DEFAULT_CURRENCY = "USDT";
    private static final int MAX_COMMISSION_LEVEL = 3;
    private static final int MIN_COMMISSION_LEVEL = 1;

    @Override
    public List<TReferralCommissionConfig> getActiveCommissionConfigs() {
        try {
            QueryWrapper<TReferralCommissionConfig> wrapper = new QueryWrapper<>();
            wrapper.eq("active", true);
            wrapper.eq("deleted", 0);
            wrapper.orderByAsc("level");
            return commissionConfigMapper.selectList(wrapper);
        } catch (Exception e) {
            log.error("获取激活佣金配置失败", e);
            throw new RuntimeException("获取激活佣金配置失败", e);
        }
    }

    @Override
    public TReferralCommissionConfig getCommissionConfigByLevel(Integer level) {
        try {
            QueryWrapper<TReferralCommissionConfig> wrapper = new QueryWrapper<>();
            wrapper.eq("level", level);
            wrapper.eq("deleted", 0);
            return commissionConfigMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.error("按级别获取佣金配置失败，级别: {}", level, e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createCommissionConfig(TReferralCommissionConfig config) {
        try {
            // 验证配置
            if (!validateCommissionConfig(config)) {
                return false;
            }

            // 检查级别是否已存在
            if (isLevelExists(config.getLevel())) {
                log.warn("佣金级别已存在，级别: {}", config.getLevel());
                return false;
            }

            // 设置默认值
            config.setCreatedAt(DateUtils.getNowDate());
            config.setUpdatedAt(DateUtils.getNowDate());
            config.setDeleted(0);
            config.setVersion(1L);
            
            if (StringUtils.isEmpty(config.getCurrency())) {
                config.setCurrency(DEFAULT_CURRENCY);
            }
            
            if (config.getActive() == null) {
                config.setActive(true);
            }

            int result = commissionConfigMapper.insert(config);
            return result > 0;
        } catch (Exception e) {
            log.error("创建佣金配置失败", e);
            throw new RuntimeException("创建佣金配置失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCommissionConfig(TReferralCommissionConfig config) {
        try {
            // 验证配置
            if (!validateCommissionConfig(config)) {
                return false;
            }

            config.setUpdatedAt(DateUtils.getNowDate());
            int result = commissionConfigMapper.updateById(config);
            return result > 0;
        } catch (Exception e) {
            log.error("更新佣金配置失败，配置ID: {}", config.getId(), e);
            throw new RuntimeException("更新佣金配置失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCommissionConfig(Integer id) {
        try {
            // 软删除
            TReferralCommissionConfig config = new TReferralCommissionConfig();
            config.setId(id);
            config.setDeleted(1);
            config.setUpdatedAt(DateUtils.getNowDate());
            
            int result = commissionConfigMapper.updateById(config);
            return result > 0;
        } catch (Exception e) {
            log.error("删除佣金配置失败，配置ID: {}", id, e);
            return false;
        }
    }

    @Override
    public boolean validateCommissionConfig(TReferralCommissionConfig config) {
        if (config == null) {
            return false;
        }

        // 检查级别
        if (config.getLevel() == null || config.getLevel() < MIN_COMMISSION_LEVEL || config.getLevel() > MAX_COMMISSION_LEVEL) {
            return false;
        }

        // 检查金额
        if (config.getAmount() == null || config.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        // 检查货币类型
        if (StringUtils.isEmpty(config.getCurrency())) {
            return false;
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateCommissionConfigs(List<TReferralCommissionConfig> configs) {
        try {
            if (configs == null || configs.isEmpty()) {
                return false;
            }

            for (TReferralCommissionConfig config : configs) {
                if (!updateCommissionConfig(config)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("批量更新佣金配置失败", e);
            throw new RuntimeException("批量更新佣金配置失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetToDefaultConfigs() {
        try {
            // 删除所有现有配置
            QueryWrapper<TReferralCommissionConfig> wrapper = new QueryWrapper<>();
            wrapper.eq("deleted", 0);
            List<TReferralCommissionConfig> existingConfigs = commissionConfigMapper.selectList(wrapper);
            
            for (TReferralCommissionConfig config : existingConfigs) {
                config.setDeleted(1);
                config.setUpdatedAt(DateUtils.getNowDate());
                commissionConfigMapper.updateById(config);
            }

            // 创建默认配置
            List<TReferralCommissionConfig> defaultConfigs = getDefaultCommissionConfigs();
            for (TReferralCommissionConfig config : defaultConfigs) {
                commissionConfigMapper.insert(config);
            }

            return true;
        } catch (Exception e) {
            log.error("重置为默认配置失败", e);
            throw new RuntimeException("重置为默认配置失败", e);
        }
    }

    @Override
    public boolean checkConfigCompleteness() {
        try {
            List<TReferralCommissionConfig> configs = getActiveCommissionConfigs();
            
            // 检查是否包含所有级别的配置
            Set<Integer> existingLevels = configs.stream()
                .map(TReferralCommissionConfig::getLevel)
                .collect(Collectors.toSet());
            
            for (int level = MIN_COMMISSION_LEVEL; level <= MAX_COMMISSION_LEVEL; level++) {
                if (!existingLevels.contains(level)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("检查配置完整性失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleCommissionConfig(Integer level, Boolean active) {
        try {
            TReferralCommissionConfig config = getCommissionConfigByLevel(level);
            if (config == null) {
                return false;
            }

            config.setActive(active);
            config.setUpdatedAt(DateUtils.getNowDate());
            
            int result = commissionConfigMapper.updateById(config);
            return result > 0;
        } catch (Exception e) {
            log.error("切换佣金配置状态失败，级别: {}", level, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getCommissionConfigStatistics() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<TReferralCommissionConfig> allConfigs = getActiveCommissionConfigs();
            
            int totalConfigs = allConfigs.size();
            int activeConfigs = (int) allConfigs.stream().filter(TReferralCommissionConfig::getActive).count();
            
            BigDecimal totalCommissionAmount = allConfigs.stream()
                .filter(TReferralCommissionConfig::getActive)
                .map(TReferralCommissionConfig::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            result.put("totalConfigs", totalConfigs);
            result.put("activeConfigs", activeConfigs);
            result.put("totalCommissionAmount", totalCommissionAmount);
            result.put("currency", DEFAULT_CURRENCY);
            
            return result;
        } catch (Exception e) {
            log.error("获取佣金配置统计失败", e);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    public List<TReferralCommissionConfig> getCommissionConfigsByCurrency(String currency) {
        try {
            QueryWrapper<TReferralCommissionConfig> wrapper = new QueryWrapper<>();
            wrapper.eq("currency", currency);
            wrapper.eq("deleted", 0);
            wrapper.orderByAsc("level");
            return commissionConfigMapper.selectList(wrapper);
        } catch (Exception e) {
            log.error("按货币类型获取佣金配置失败，货币: {}", currency, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Integer getMaxCommissionLevel() {
        return MAX_COMMISSION_LEVEL;
    }

    @Override
    public Integer getMinCommissionLevel() {
        return MIN_COMMISSION_LEVEL;
    }

    @Override
    public boolean isLevelExists(Integer level) {
        try {
            QueryWrapper<TReferralCommissionConfig> wrapper = new QueryWrapper<>();
            wrapper.eq("level", level);
            wrapper.eq("deleted", 0);
            return commissionConfigMapper.selectCount(wrapper) > 0;
        } catch (Exception e) {
            log.error("检查级别是否存在失败，级别: {}", level, e);
            return false;
        }
    }

    @Override
    public List<TReferralCommissionConfig> getDefaultCommissionConfigs() {
        List<TReferralCommissionConfig> configs = new ArrayList<>();
        
        // 一级佣金配置
        TReferralCommissionConfig level1 = new TReferralCommissionConfig();
        level1.setLevel(1);
        level1.setAmount(new BigDecimal("10.00000000"));
        level1.setCurrency(DEFAULT_CURRENCY);
        level1.setActive(true);
        level1.setCreatedAt(DateUtils.getNowDate());
        level1.setUpdatedAt(DateUtils.getNowDate());
        level1.setDeleted(0);
        level1.setVersion(1L);
        configs.add(level1);
        
        // 二级佣金配置
        TReferralCommissionConfig level2 = new TReferralCommissionConfig();
        level2.setLevel(2);
        level2.setAmount(new BigDecimal("5.00000000"));
        level2.setCurrency(DEFAULT_CURRENCY);
        level2.setActive(true);
        level2.setCreatedAt(DateUtils.getNowDate());
        level2.setUpdatedAt(DateUtils.getNowDate());
        level2.setDeleted(0);
        level2.setVersion(1L);
        configs.add(level2);
        
        // 三级佣金配置
        TReferralCommissionConfig level3 = new TReferralCommissionConfig();
        level3.setLevel(3);
        level3.setAmount(new BigDecimal("2.00000000"));
        level3.setCurrency(DEFAULT_CURRENCY);
        level3.setActive(true);
        level3.setCreatedAt(DateUtils.getNowDate());
        level3.setUpdatedAt(DateUtils.getNowDate());
        level3.setDeleted(0);
        level3.setVersion(1L);
        configs.add(level3);
        
        return configs;
    }

    @Override
    public BigDecimal calculateTotalCommissionAmount() {
        try {
            List<TReferralCommissionConfig> configs = getActiveCommissionConfigs();
            return configs.stream()
                .filter(TReferralCommissionConfig::getActive)
                .map(TReferralCommissionConfig::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.error("计算总佣金金额失败", e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    public String exportCommissionConfigs() {
        // TODO: 实现导出功能
        return "";
    }

    @Override
    public boolean importCommissionConfigs(String configData) {
        // TODO: 实现导入功能
        return false;
    }
}