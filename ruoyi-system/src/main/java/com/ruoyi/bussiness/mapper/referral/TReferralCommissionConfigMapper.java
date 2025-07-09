package com.ruoyi.bussiness.mapper.referral;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.bussiness.domain.referral.TReferralCommissionConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 佣金配置Mapper接口
 * 
 * @author ruoyi
 * @date 2025-01-09
 */
@Mapper
public interface TReferralCommissionConfigMapper extends BaseMapper<TReferralCommissionConfig> {

    /**
     * 查询所有激活的佣金配置
     * 
     * @return 佣金配置列表
     */
    List<TReferralCommissionConfig> selectActiveConfigs();

    /**
     * 根据级别查询佣金配置
     * 
     * @param level 级别
     * @return 佣金配置
     */
    TReferralCommissionConfig selectByLevel(@Param("level") Integer level);

    /**
     * 根据级别查询激活的佣金配置
     * 
     * @param level 级别
     * @return 佣金配置
     */
    TReferralCommissionConfig selectActiveBylevel(@Param("level") Integer level);

    /**
     * 批量插入佣金配置
     * 
     * @param configs 佣金配置列表
     * @return 插入数量
     */
    Integer insertBatch(@Param("configs") List<TReferralCommissionConfig> configs);

    /**
     * 批量更新佣金配置
     * 
     * @param configs 佣金配置列表
     * @return 更新数量
     */
    Integer updateBatch(@Param("configs") List<TReferralCommissionConfig> configs);

    /**
     * 删除所有佣金配置
     * 
     * @return 删除数量
     */
    Integer deleteAll();

    /**
     * 查询总配置数量
     * 
     * @return 总数量
     */
    Integer countTotalConfigs();

    /**
     * 查询激活配置数量
     * 
     * @return 激活数量
     */
    Integer countActiveConfigs();

    /**
     * 查询指定货币的佣金配置
     * 
     * @param currency 货币类型
     * @return 佣金配置列表
     */
    List<TReferralCommissionConfig> selectByCurrency(@Param("currency") String currency);

    /**
     * 查询指定货币的激活配置
     * 
     * @param currency 货币类型
     * @return 佣金配置列表
     */
    List<TReferralCommissionConfig> selectActiveByCurrency(@Param("currency") String currency);

    /**
     * 按级别排序查询激活配置
     * 
     * @return 佣金配置列表
     */
    List<TReferralCommissionConfig> selectActiveConfigsOrderByLevel();

    /**
     * 更新配置激活状态
     * 
     * @param level 级别
     * @param active 激活状态
     * @return 更新数量
     */
    Integer updateActiveStatus(@Param("level") Integer level, @Param("active") Boolean active);

    /**
     * 检查级别是否存在
     * 
     * @param level 级别
     * @return 是否存在
     */
    Boolean existsByLevel(@Param("level") Integer level);

    /**
     * 查询最大级别
     * 
     * @return 最大级别
     */
    Integer selectMaxLevel();

    /**
     * 查询最小级别
     * 
     * @return 最小级别
     */
    Integer selectMinLevel();
}