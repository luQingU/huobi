package com.ruoyi.bussiness.service.referral;

import com.ruoyi.bussiness.domain.referral.TReferralCommissionConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 佣金配置管理服务接口
 * 
 * @author ruoyi
 * @date 2025-01-09
 */
public interface IReferralCommissionConfigService {

    /**
     * 获取所有激活的佣金配置
     * 
     * @return 佣金配置列表
     */
    List<TReferralCommissionConfig> getActiveCommissionConfigs();

    /**
     * 按级别获取佣金配置
     * 
     * @param level 级别
     * @return 佣金配置
     */
    TReferralCommissionConfig getCommissionConfigByLevel(Integer level);

    /**
     * 创建佣金配置
     * 
     * @param config 佣金配置
     * @return 是否成功
     */
    boolean createCommissionConfig(TReferralCommissionConfig config);

    /**
     * 更新佣金配置
     * 
     * @param config 佣金配置
     * @return 是否成功
     */
    boolean updateCommissionConfig(TReferralCommissionConfig config);

    /**
     * 删除佣金配置
     * 
     * @param id 配置ID
     * @return 是否成功
     */
    boolean deleteCommissionConfig(Integer id);

    /**
     * 验证佣金配置
     * 
     * @param config 佣金配置
     * @return 是否有效
     */
    boolean validateCommissionConfig(TReferralCommissionConfig config);

    /**
     * 批量更新佣金配置
     * 
     * @param configs 佣金配置列表
     * @return 是否成功
     */
    boolean batchUpdateCommissionConfigs(List<TReferralCommissionConfig> configs);

    /**
     * 重置为默认配置
     * 
     * @return 是否成功
     */
    boolean resetToDefaultConfigs();

    /**
     * 检查配置完整性
     * 
     * @return 是否完整
     */
    boolean checkConfigCompleteness();

    /**
     * 切换佣金配置的激活状态
     * 
     * @param level 级别
     * @param active 激活状态
     * @return 是否成功
     */
    boolean toggleCommissionConfig(Integer level, Boolean active);

    /**
     * 获取佣金配置统计信息
     * 
     * @return 统计信息
     */
    Map<String, Object> getCommissionConfigStatistics();

    /**
     * 获取指定货币的佣金配置
     * 
     * @param currency 货币类型
     * @return 佣金配置列表
     */
    List<TReferralCommissionConfig> getCommissionConfigsByCurrency(String currency);

    /**
     * 获取最大支持的佣金级别
     * 
     * @return 最大级别
     */
    Integer getMaxCommissionLevel();

    /**
     * 获取最小支持的佣金级别
     * 
     * @return 最小级别
     */
    Integer getMinCommissionLevel();

    /**
     * 检查级别是否存在
     * 
     * @param level 级别
     * @return 是否存在
     */
    boolean isLevelExists(Integer level);

    /**
     * 获取默认佣金配置
     * 
     * @return 默认佣金配置列表
     */
    List<TReferralCommissionConfig> getDefaultCommissionConfigs();

    /**
     * 计算总佣金金额
     * 
     * @return 总佣金金额
     */
    BigDecimal calculateTotalCommissionAmount();

    /**
     * 导出佣金配置
     * 
     * @return 导出数据
     */
    String exportCommissionConfigs();

    /**
     * 导入佣金配置
     * 
     * @param configData 配置数据
     * @return 是否成功
     */
    boolean importCommissionConfigs(String configData);
}