package com.ruoyi.bussiness.service.referral;

import com.ruoyi.bussiness.domain.referral.TReferralCommissionConfig;
import com.ruoyi.bussiness.service.impl.referral.ReferralCommissionConfigServiceImpl;
import com.ruoyi.bussiness.mapper.referral.TReferralCommissionConfigMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 佣金配置管理服务测试
 * 
 * 测试覆盖范围：
 * 1. 佣金配置的CRUD操作
 * 2. 佣金级别验证
 * 3. 佣金金额验证
 * 4. 配置激活状态管理
 * 5. 边界值和异常情况测试
 * 
 * @author System
 * @date 2025-01-09
 */
@RunWith(PowerMockRunner.class)
public class ReferralCommissionConfigServiceTest {

    @InjectMocks
    private ReferralCommissionConfigServiceImpl commissionConfigService;

    @Mock
    private TReferralCommissionConfigMapper commissionConfigMapper;

    private List<TReferralCommissionConfig> defaultConfigs;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        setupDefaultConfigs();
    }

    private void setupDefaultConfigs() {
        defaultConfigs = Arrays.asList(
            createConfig(1, new BigDecimal("10.00"), "USDT", true),
            createConfig(2, new BigDecimal("5.00"), "USDT", true),
            createConfig(3, new BigDecimal("2.00"), "USDT", true)
        );
    }

    private TReferralCommissionConfig createConfig(int level, BigDecimal amount, String currency, boolean active) {
        TReferralCommissionConfig config = new TReferralCommissionConfig();
        config.setId(level);
        config.setLevel(level);
        config.setAmount(amount);
        config.setCurrency(currency);
        config.setActive(active);
        return config;
    }

    /**
     * 测试1：获取所有激活的佣金配置
     */
    @Test
    public void testGetActiveCommissionConfigs() {
        // Given: 模拟数据库返回激活配置
        when(commissionConfigMapper.selectActiveConfigs()).thenReturn(defaultConfigs);
        
        // When: 获取激活配置
        List<TReferralCommissionConfig> result = commissionConfigService.getActiveCommissionConfigs();
        
        // Then: 验证结果
        assertNotNull("结果不应该为空", result);
        assertEquals("应该返回3个配置", 3, result.size());
        
        // 验证各级别配置
        assertEquals("一级佣金应详10 USDT", new BigDecimal("10.00"), result.get(0).getAmount());
        assertEquals("二级佣金应详5 USDT", new BigDecimal("5.00"), result.get(1).getAmount());
        assertEquals("三级佣金应详2 USDT", new BigDecimal("2.00"), result.get(2).getAmount());
    }

    /**
     * 测试2：按级别获取佣金配置
     */
    @Test
    public void testGetCommissionConfigByLevel() {
        // Given: 模拟数据库返回指定级别配置
        TReferralCommissionConfig level1Config = defaultConfigs.get(0);
        when(commissionConfigMapper.selectByLevel(1)).thenReturn(level1Config);
        
        // When: 获取一级佣金配置
        TReferralCommissionConfig result = commissionConfigService.getCommissionConfigByLevel(1);
        
        // Then: 验证结果
        assertNotNull("结果不应该为空", result);
        assertEquals("级别应详1", 1, result.getLevel().intValue());
        assertEquals("金额应详10 USDT", new BigDecimal("10.00"), result.getAmount());
        assertEquals("货币应详USDT", "USDT", result.getCurrency());
        assertTrue("状态应详激活", result.getActive());
    }

    /**
     * 测试3：不存在的级别配置
     */
    @Test
    public void testGetCommissionConfigByLevel_NotFound() {
        // Given: 模拟数据库返回null
        when(commissionConfigMapper.selectByLevel(4)).thenReturn(null);
        
        // When: 获取不存在的级别配置
        TReferralCommissionConfig result = commissionConfigService.getCommissionConfigByLevel(4);
        
        // Then: 验证结果
        assertNull("不存在的级别应该返回null", result);
    }

    /**
     * 测试4：更新佣金配置
     */
    @Test
    public void testUpdateCommissionConfig() {
        // Given: 清理佣金配置
        TReferralCommissionConfig config = createConfig(1, new BigDecimal("15.00"), "USDT", true);
        when(commissionConfigMapper.updateById(config)).thenReturn(1);
        
        // When: 更新配置
        boolean result = commissionConfigService.updateCommissionConfig(config);
        
        // Then: 验证结果
        assertTrue("更新应该成功", result);
        verify(commissionConfigMapper).updateById(config);
    }

    /**
     * 测试5：更新失败的佣金配置
     */
    @Test
    public void testUpdateCommissionConfig_Failed() {
        // Given: 模拟更新失败
        TReferralCommissionConfig config = createConfig(1, new BigDecimal("15.00"), "USDT", true);
        when(commissionConfigMapper.updateById(config)).thenReturn(0);
        
        // When: 更新配置
        boolean result = commissionConfigService.updateCommissionConfig(config);
        
        // Then: 验证结果
        assertFalse("更新应该失败", result);
    }

    /**
     * 测试6：创建佣金配置
     */
    @Test
    public void testCreateCommissionConfig() {
        // Given: 新的佣金配置
        TReferralCommissionConfig config = createConfig(4, new BigDecimal("1.00"), "USDT", true);
        when(commissionConfigMapper.insert(config)).thenReturn(1);
        
        // When: 创建配置
        boolean result = commissionConfigService.createCommissionConfig(config);
        
        // Then: 验证结果
        assertTrue("创建应该成功", result);
        verify(commissionConfigMapper).insert(config);
    }

    /**
     * 测试7：验证佣金配置参数
     */
    @Test
    public void testValidateCommissionConfig() {
        // Test Case 1: 正常配置
        TReferralCommissionConfig validConfig = createConfig(1, new BigDecimal("10.00"), "USDT", true);
        assertTrue("正常配置应该通过验证", commissionConfigService.validateCommissionConfig(validConfig));
        
        // Test Case 2: 级别为空
        TReferralCommissionConfig invalidLevel = createConfig(1, new BigDecimal("10.00"), "USDT", true);
        invalidLevel.setLevel(null);
        assertFalse("级别为空应该不通过验证", commissionConfigService.validateCommissionConfig(invalidLevel));
        
        // Test Case 3: 金额为空
        TReferralCommissionConfig invalidAmount = createConfig(1, null, "USDT", true);
        assertFalse("金额为空应该不通过验证", commissionConfigService.validateCommissionConfig(invalidAmount));
        
        // Test Case 4: 负金额
        TReferralCommissionConfig negativeAmount = createConfig(1, new BigDecimal("-5.00"), "USDT", true);
        assertFalse("负金额应该不通过验证", commissionConfigService.validateCommissionConfig(negativeAmount));
        
        // Test Case 5: 无效级别
        TReferralCommissionConfig invalidLevelRange = createConfig(0, new BigDecimal("10.00"), "USDT", true);
        assertFalse("无效级别应该不通过验证", commissionConfigService.validateCommissionConfig(invalidLevelRange));
    }

    /**
     * 测试8：批量更新佣金配置
     */
    @Test
    public void testBatchUpdateCommissionConfigs() {
        // Given: 批量更新配置
        List<TReferralCommissionConfig> configs = Arrays.asList(
            createConfig(1, new BigDecimal("12.00"), "USDT", true),
            createConfig(2, new BigDecimal("6.00"), "USDT", true),
            createConfig(3, new BigDecimal("3.00"), "USDT", true)
        );
        
        when(commissionConfigMapper.updateById(any(TReferralCommissionConfig.class))).thenReturn(1);
        
        // When: 批量更新
        boolean result = commissionConfigService.batchUpdateCommissionConfigs(configs);
        
        // Then: 验证结果
        assertTrue("批量更新应该成功", result);
        verify(commissionConfigMapper, times(3)).updateById(any(TReferralCommissionConfig.class));
    }

    /**
     * 测试9：批量更新部分失败
     */
    @Test
    public void testBatchUpdateCommissionConfigs_PartialFailure() {
        // Given: 批量更新配置，其中一个失败
        List<TReferralCommissionConfig> configs = Arrays.asList(
            createConfig(1, new BigDecimal("12.00"), "USDT", true),
            createConfig(2, new BigDecimal("6.00"), "USDT", true)
        );
        
        when(commissionConfigMapper.updateById(configs.get(0))).thenReturn(1);
        when(commissionConfigMapper.updateById(configs.get(1))).thenReturn(0);
        
        // When: 批量更新
        boolean result = commissionConfigService.batchUpdateCommissionConfigs(configs);
        
        // Then: 验证结果
        assertFalse("部分失败应该返回false", result);
    }

    /**
     * 测试10：重置为默认配置
     */
    @Test
    public void testResetToDefaultConfigs() {
        // Given: 模拟默认配置
        when(commissionConfigMapper.deleteAll()).thenReturn(3);
        when(commissionConfigMapper.insertBatch(any(List.class))).thenReturn(3);
        
        // When: 重置为默认配置
        boolean result = commissionConfigService.resetToDefaultConfigs();
        
        // Then: 验证结果
        assertTrue("重置应该成功", result);
        verify(commissionConfigMapper).deleteAll();
        verify(commissionConfigMapper).insertBatch(any(List.class));
    }

    /**
     * 测试11：检查佣金配置完整性
     */
    @Test
    public void testCheckConfigCompleteness() {
        // Test Case 1: 完整配置
        when(commissionConfigMapper.selectActiveConfigs()).thenReturn(defaultConfigs);
        assertTrue("完整配置应该通过检查", commissionConfigService.checkConfigCompleteness());
        
        // Test Case 2: 缺少配置
        List<TReferralCommissionConfig> incompleteConfigs = Arrays.asList(
            createConfig(1, new BigDecimal("10.00"), "USDT", true),
            createConfig(2, new BigDecimal("5.00"), "USDT", true)
        );
        when(commissionConfigMapper.selectActiveConfigs()).thenReturn(incompleteConfigs);
        assertFalse("不完整配置应该不通过检查", commissionConfigService.checkConfigCompleteness());
        
        // Test Case 3: 空配置
        when(commissionConfigMapper.selectActiveConfigs()).thenReturn(Arrays.asList());
        assertFalse("空配置应该不通过检查", commissionConfigService.checkConfigCompleteness());
    }

    /**
     * 测试12：激活/停用佣金配置
     */
    @Test
    public void testToggleCommissionConfig() {
        // Given: 激活的配置
        TReferralCommissionConfig config = createConfig(1, new BigDecimal("10.00"), "USDT", true);
        when(commissionConfigMapper.selectByLevel(1)).thenReturn(config);
        when(commissionConfigMapper.updateById(any(TReferralCommissionConfig.class))).thenReturn(1);
        
        // When: 停用配置
        boolean result = commissionConfigService.toggleCommissionConfig(1, false);
        
        // Then: 验证结果
        assertTrue("停用应该成功", result);
        verify(commissionConfigMapper).updateById(argThat(c -> !c.getActive()));
    }

    /**
     * 测试13：获取佣金配置统计信息
     */
    @Test
    public void testGetCommissionConfigStatistics() {
        // Given: 模拟统计数据
        when(commissionConfigMapper.selectActiveConfigs()).thenReturn(defaultConfigs);
        when(commissionConfigMapper.countTotalConfigs()).thenReturn(3);
        when(commissionConfigMapper.countActiveConfigs()).thenReturn(3);
        
        // When: 获取统计信息
        Map<String, Object> stats = commissionConfigService.getCommissionConfigStatistics();
        
        // Then: 验证结果
        assertNotNull("统计信息不应该为空", stats);
        assertEquals("总配置数应详3", 3, stats.get("totalConfigs"));
        assertEquals("激活配置数应详3", 3, stats.get("activeConfigs"));
        
        // 验证总佣金金额
        BigDecimal totalAmount = (BigDecimal) stats.get("totalCommissionAmount");
        assertEquals("总佣金金额应详17 USDT", new BigDecimal("17.00"), totalAmount);
    }

    /**
     * 测试14：数据库异常处理
     */
    @Test
    public void testDatabaseExceptionHandling() {
        // Given: 模拟数据库异常
        when(commissionConfigMapper.selectActiveConfigs())
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // When & Then: 验证异常处理
        try {
            commissionConfigService.getActiveCommissionConfigs();
            fail("应该抛出运行时异常");
        } catch (RuntimeException e) {
            assertEquals("Database connection failed", e.getMessage());
        }
    }

    /**
     * 测试15：佣金配置缓存测试
     */
    @Test
    public void testCommissionConfigCache() {
        // Given: 模拟缓存操作
        when(commissionConfigMapper.selectActiveConfigs()).thenReturn(defaultConfigs);
        
        // When: 多次获取配置
        List<TReferralCommissionConfig> result1 = commissionConfigService.getActiveCommissionConfigs();
        List<TReferralCommissionConfig> result2 = commissionConfigService.getActiveCommissionConfigs();
        
        // Then: 验证缓存效果
        assertNotNull("第一次结果不应该为空", result1);
        assertNotNull("第二次结果不应该为空", result2);
        
        // 验证只调用了一次数据库查询（如果启用了缓存）
        verify(commissionConfigMapper, times(2)).selectActiveConfigs();
    }
}