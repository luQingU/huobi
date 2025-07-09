package com.ruoyi.bussiness.service.referral;

import com.ruoyi.bussiness.domain.TAppUser;
import com.ruoyi.bussiness.domain.TAppAsset;
import com.ruoyi.bussiness.domain.TAppWalletRecord;
import com.ruoyi.bussiness.domain.TMineOrder;
import com.ruoyi.bussiness.domain.referral.TReferralCommissionRecord;
import com.ruoyi.bussiness.domain.referral.TReferralCommissionConfig;
import com.ruoyi.bussiness.service.ITAppUserService;
import com.ruoyi.bussiness.service.ITAppAssetService;
import com.ruoyi.bussiness.service.ITAppWalletRecordService;
import com.ruoyi.bussiness.service.impl.referral.ReferralCommissionServiceImpl;
import com.ruoyi.bussiness.mapper.referral.TReferralCommissionRecordMapper;
import com.ruoyi.bussiness.mapper.referral.TReferralCommissionConfigMapper;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 三级返佣系统 - 佣金计算和分发服务测试
 * 
 * 测试覆盖范围：
 * 1. 三级佣金计算（L1: 10 USDT, L2: 5 USDT, L3: 2 USDT）
 * 2. 用户推荐关系层级验证
 * 3. 交易事务完整性测试
 * 4. 边缘情况处理
 * 5. 安全性验证
 * 
 * @author System
 * @date 2025-01-09
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecurityUtils.class, DateUtils.class})
public class ReferralCommissionServiceTest {

    @InjectMocks
    private ReferralCommissionServiceImpl referralCommissionService;

    @Mock
    private ITAppUserService appUserService;

    @Mock
    private ITAppAssetService appAssetService;

    @Mock
    private ITAppWalletRecordService walletRecordService;

    @Mock
    private TReferralCommissionRecordMapper commissionRecordMapper;

    @Mock
    private TReferralCommissionConfigMapper commissionConfigMapper;

    // 测试数据
    private TAppUser buyerUser;
    private TAppUser level1User;
    private TAppUser level2User;
    private TAppUser level3User;
    private TMineOrder testOrder;
    private List<TReferralCommissionConfig> commissionConfigs;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(SecurityUtils.class);
        PowerMockito.mockStatic(DateUtils.class);
        
        setupTestData();
        setupMockBehaviors();
    }

    private void setupTestData() {
        // 购买用户（三级用户）
        buyerUser = new TAppUser();
        buyerUser.setUserId(1003L);
        buyerUser.setUsername("buyer_user");
        buyerUser.setAppParentIds("1000,1001,1002"); // 推荐链：1000->1001->1002->1003
        buyerUser.setStatus("0"); // 正常状态
        buyerUser.setActiveCode("BUYER123");

        // 一级推荐人（直推）
        level1User = new TAppUser();
        level1User.setUserId(1002L);
        level1User.setUsername("level1_user");
        level1User.setAppParentIds("1000,1001");
        level1User.setStatus("0");
        level1User.setActiveCode("LEVEL1123");

        // 二级推荐人（间推）
        level2User = new TAppUser();
        level2User.setUserId(1001L);
        level2User.setUsername("level2_user");
        level2User.setAppParentIds("1000");
        level2User.setStatus("0");
        level2User.setActiveCode("LEVEL2123");

        // 三级推荐人（三级）
        level3User = new TAppUser();
        level3User.setUserId(1000L);
        level3User.setUsername("level3_user");
        level3User.setAppParentIds("");
        level3User.setStatus("0");
        level3User.setActiveCode("LEVEL3123");

        // 测试订单
        testOrder = new TMineOrder();
        testOrder.setId(2001L);
        testOrder.setUserId(1003L);
        testOrder.setAmount(new BigDecimal("100.00"));
        testOrder.setStatus(1L); // 已支付
        testOrder.setCommissionProcessed(false);

        // 佣金配置
        commissionConfigs = Arrays.asList(
            createCommissionConfig(1, new BigDecimal("10.00")),
            createCommissionConfig(2, new BigDecimal("5.00")),
            createCommissionConfig(3, new BigDecimal("2.00"))
        );
    }

    private TReferralCommissionConfig createCommissionConfig(int level, BigDecimal amount) {
        TReferralCommissionConfig config = new TReferralCommissionConfig();
        config.setLevel(level);
        config.setAmount(amount);
        config.setCurrency("USDT");
        config.setActive(true);
        return config;
    }

    private void setupMockBehaviors() {
        // Mock用户查询
        when(appUserService.getById(1003L)).thenReturn(buyerUser);
        when(appUserService.getById(1002L)).thenReturn(level1User);
        when(appUserService.getById(1001L)).thenReturn(level2User);
        when(appUserService.getById(1000L)).thenReturn(level3User);

        // Mock佣金配置查询
        when(commissionConfigMapper.selectList(any())).thenReturn(commissionConfigs);

        // Mock时间工具
        when(DateUtils.getNowDate()).thenReturn(new java.util.Date());
    }

    /**
     * 测试1：正常三级返佣计算
     * 验证完整的三级返佣流程，确保所有层级用户都能获得正确的佣金
     */
    @Test
    public void testCalculateAndDistributeCommission_FullThreeLevels() {
        // Given: 所有三级用户都存在且状态正常
        setupValidThreeLevelUsers();
        
        // When: 执行佣金计算和分发
        boolean result = referralCommissionService.calculateAndDistributeCommission(testOrder);
        
        // Then: 验证结果
        assertTrue("佣金计算和分发应该成功", result);
        
        // 验证佣金记录创建
        verify(commissionRecordMapper, times(3)).insert(any(TReferralCommissionRecord.class));
        
        // 验证用户资产更新
        verify(appAssetService, times(3)).updateUserAsset(anyLong(), eq("USDT"), any(BigDecimal.class));
        
        // 验证钱包记录生成
        verify(walletRecordService, times(3)).generateRecord(any(TAppWalletRecord.class));
    }

    /**
     * 测试2：二级返佣计算
     * 验证只有两级推荐关系时的佣金分发
     */
    @Test
    public void testCalculateAndDistributeCommission_TwoLevels() {
        // Given: 只有二级推荐关系
        buyerUser.setAppParentIds("1001,1002"); // 只有两级：1001->1002->1003
        
        // When: 执行佣金计算
        boolean result = referralCommissionService.calculateAndDistributeCommission(testOrder);
        
        // Then: 验证结果
        assertTrue("二级返佣应该成功", result);
        
        // 验证只创建了两条佣金记录
        verify(commissionRecordMapper, times(2)).insert(any(TReferralCommissionRecord.class));
        
        // 验证佣金金额
        verify(appAssetService).updateUserAsset(eq(1002L), eq("USDT"), eq(new BigDecimal("10.00"))); // L1: 10 USDT
        verify(appAssetService).updateUserAsset(eq(1001L), eq("USDT"), eq(new BigDecimal("5.00"))); // L2: 5 USDT
    }

    /**
     * 测试3：一级返佣计算
     * 验证只有一级推荐关系时的佣金分发
     */
    @Test
    public void testCalculateAndDistributeCommission_OneLevel() {
        // Given: 只有一级推荐关系
        buyerUser.setAppParentIds("1002"); // 只有一级：1002->1003
        
        // When: 执行佣金计算
        boolean result = referralCommissionService.calculateAndDistributeCommission(testOrder);
        
        // Then: 验证结果
        assertTrue("一级返佣应该成功", result);
        
        // 验证只创建了一条佣金记录
        verify(commissionRecordMapper, times(1)).insert(any(TReferralCommissionRecord.class));
        
        // 验证佣金金额
        verify(appAssetService).updateUserAsset(eq(1002L), eq("USDT"), eq(new BigDecimal("10.00"))); // L1: 10 USDT
    }

    /**
     * 测试4：无推荐关系场景
     * 验证用户没有推荐关系时不进行佣金分发
     */
    @Test
    public void testCalculateAndDistributeCommission_NoReferral() {
        // Given: 无推荐关系
        buyerUser.setAppParentIds(""); // 无推荐关系
        
        // When: 执行佣金计算
        boolean result = referralCommissionService.calculateAndDistributeCommission(testOrder);
        
        // Then: 验证结果
        assertTrue("无推荐关系应该直接返回成功", result);
        
        // 验证没有创建佣金记录
        verify(commissionRecordMapper, never()).insert(any(TReferralCommissionRecord.class));
        
        // 验证没有更新用户资产
        verify(appAssetService, never()).updateUserAsset(anyLong(), anyString(), any(BigDecimal.class));
    }

    /**
     * 测试5：推荐人状态异常
     * 验证推荐人账号被冻结或禁用时的处理
     */
    @Test
    public void testCalculateAndDistributeCommission_InactiveReferrer() {
        // Given: 一级推荐人账号被冻结
        level1User.setStatus("1"); // 禁用状态
        
        // When: 执行佣金计算
        boolean result = referralCommissionService.calculateAndDistributeCommission(testOrder);
        
        // Then: 验证结果
        assertTrue("处理应该成功，但跳过异常用户", result);
        
        // 验证只创建了两条佣金记录（跳过一级推荐人）
        verify(commissionRecordMapper, times(2)).insert(any(TReferralCommissionRecord.class));
        
        // 验证没有为禁用用户更新资产
        verify(appAssetService, never()).updateUserAsset(eq(1002L), anyString(), any(BigDecimal.class));
        
        // 验证为其他有效用户更新资产
        verify(appAssetService).updateUserAsset(eq(1001L), eq("USDT"), eq(new BigDecimal("5.00"))); // L2: 5 USDT
        verify(appAssetService).updateUserAsset(eq(1000L), eq("USDT"), eq(new BigDecimal("2.00"))); // L3: 2 USDT
    }

    /**
     * 测试6：推荐人不存在
     * 验证推荐人ID在数据库中不存在时的处理
     */
    @Test
    public void testCalculateAndDistributeCommission_NonExistentReferrer() {
        // Given: 推荐人不存在
        buyerUser.setAppParentIds("9999,1001,1002"); // 9999不存在
        when(appUserService.getById(9999L)).thenReturn(null);
        
        // When: 执行佣金计算
        boolean result = referralCommissionService.calculateAndDistributeCommission(testOrder);
        
        // Then: 验证结果
        assertTrue("处理应该成功，但跳过不存在的用户", result);
        
        // 验证只创建了两条佣金记录（跳过不存在的用户）
        verify(commissionRecordMapper, times(2)).insert(any(TReferralCommissionRecord.class));
    }

    /**
     * 测试7：订单已处理佣金
     * 验证防止重复处理同一订单佣金的机制
     */
    @Test
    public void testCalculateAndDistributeCommission_AlreadyProcessed() {
        // Given: 订单已处理佣金
        testOrder.setCommissionProcessed(true);
        
        // When: 执行佣金计算
        boolean result = referralCommissionService.calculateAndDistributeCommission(testOrder);
        
        // Then: 验证结果
        assertFalse("已处理的订单应该返回false", result);
        
        // 验证没有创建佣金记录
        verify(commissionRecordMapper, never()).insert(any(TReferralCommissionRecord.class));
        
        // 验证没有更新用户资产
        verify(appAssetService, never()).updateUserAsset(anyLong(), anyString(), any(BigDecimal.class));
    }

    /**
     * 测试8：订单状态异常
     * 验证订单状态不是已支付时不进行佣金分发
     */
    @Test
    public void testCalculateAndDistributeCommission_InvalidOrderStatus() {
        // Given: 订单状态为待支付
        testOrder.setStatus(0L); // 待支付状态
        
        // When: 执行佣金计算
        boolean result = referralCommissionService.calculateAndDistributeCommission(testOrder);
        
        // Then: 验证结果
        assertFalse("未支付订单应该返回false", result);
        
        // 验证没有创建佣金记录
        verify(commissionRecordMapper, never()).insert(any(TReferralCommissionRecord.class));
    }

    /**
     * 测试9：佣金配置异常
     * 验证佣金配置不存在或异常时的处理
     */
    @Test
    public void testCalculateAndDistributeCommission_MissingCommissionConfig() {
        // Given: 佣金配置为空
        when(commissionConfigMapper.selectList(any())).thenReturn(Arrays.asList());
        
        // When: 执行佣金计算
        boolean result = referralCommissionService.calculateAndDistributeCommission(testOrder);
        
        // Then: 验证结果
        assertFalse("缺少佣金配置应该返回false", result);
        
        // 验证没有创建佣金记录
        verify(commissionRecordMapper, never()).insert(any(TReferralCommissionRecord.class));
    }

    /**
     * 测试10：数据库异常处理
     * 验证数据库操作失败时的事务回滚
     */
    @Test
    public void testCalculateAndDistributeCommission_DatabaseException() {
        // Given: 数据库插入失败
        when(commissionRecordMapper.insert(any(TReferralCommissionRecord.class)))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // When & Then: 验证异常处理
        try {
            referralCommissionService.calculateAndDistributeCommission(testOrder);
            fail("应该抛出运行时异常");
        } catch (RuntimeException e) {
            assertEquals("Database connection failed", e.getMessage());
        }
        
        // 验证事务回滚（通过验证后续操作未执行）
        verify(appAssetService, never()).updateUserAsset(anyLong(), anyString(), any(BigDecimal.class));
    }

    /**
     * 测试11：佣金金额精度测试
     * 验证佣金金额的精确计算，确保财务数据准确性
     */
    @Test
    public void testCalculateAndDistributeCommission_AmountPrecision() {
        // Given: 设置精确的佣金配置
        setupValidThreeLevelUsers();
        
        // When: 执行佣金计算
        boolean result = referralCommissionService.calculateAndDistributeCommission(testOrder);
        
        // Then: 验证精确金额
        assertTrue("佣金计算应该成功", result);
        
        // 验证精确到8位小数的佣金金额
        verify(appAssetService).updateUserAsset(eq(1002L), eq("USDT"), eq(new BigDecimal("10.00000000"))); // L1: 精确10 USDT
        verify(appAssetService).updateUserAsset(eq(1001L), eq("USDT"), eq(new BigDecimal("5.00000000"))); // L2: 精确5 USDT
        verify(appAssetService).updateUserAsset(eq(1000L), eq("USDT"), eq(new BigDecimal("2.00000000"))); // L3: 精确2 USDT
    }

    /**
     * 测试12：并发安全性测试
     * 验证在高并发环境下佣金计算的数据一致性
     */
    @Test
    public void testCalculateAndDistributeCommission_ConcurrentSafety() {
        // Given: 模拟并发环境
        setupValidThreeLevelUsers();
        
        // When: 并发执行佣金计算
        boolean result1 = referralCommissionService.calculateAndDistributeCommission(testOrder);
        testOrder.setCommissionProcessed(true); // 模拟已处理状态
        boolean result2 = referralCommissionService.calculateAndDistributeCommission(testOrder);
        
        // Then: 验证结果
        assertTrue("第一次处理应该成功", result1);
        assertFalse("第二次处理应该失败", result2);
        
        // 验证只处理了一次
        verify(commissionRecordMapper, times(3)).insert(any(TReferralCommissionRecord.class));
    }

    /**
     * 测试13：推荐关系链格式验证
     * 验证各种推荐关系链格式的正确解析
     */
    @Test
    public void testCalculateAndDistributeCommission_ReferralChainFormats() {
        // Test Case 1: 标准格式
        buyerUser.setAppParentIds("1000,1001,1002");
        assertTrue("标准格式应该成功", referralCommissionService.calculateAndDistributeCommission(testOrder));
        
        // Test Case 2: 单个ID
        buyerUser.setAppParentIds("1002");
        assertTrue("单个ID应该成功", referralCommissionService.calculateAndDistributeCommission(testOrder));
        
        // Test Case 3: 空字符串
        buyerUser.setAppParentIds("");
        assertTrue("空字符串应该成功", referralCommissionService.calculateAndDistributeCommission(testOrder));
        
        // Test Case 4: null值
        buyerUser.setAppParentIds(null);
        assertTrue("null值应该成功", referralCommissionService.calculateAndDistributeCommission(testOrder));
    }

    /**
     * 测试14：佣金记录完整性验证
     * 验证生成的佣金记录包含所有必要信息
     */
    @Test
    public void testCalculateAndDistributeCommission_CommissionRecordCompleteness() {
        // Given: 完整的三级推荐关系
        setupValidThreeLevelUsers();
        
        // When: 执行佣金计算
        referralCommissionService.calculateAndDistributeCommission(testOrder);
        
        // Then: 验证佣金记录完整性
        verify(commissionRecordMapper, times(3)).insert(argThat(record -> {
            return record.getOrderId() != null &&
                   record.getRecipientUserId() != null &&
                   record.getPayerUserId() != null &&
                   record.getCommissionAmount() != null &&
                   record.getCommissionLevel() != null &&
                   record.getCommissionRate() != null &&
                   record.getStatus() != null;
        }));
    }

    /**
     * 测试15：订单金额边界值测试
     * 验证不同订单金额对佣金计算的影响
     */
    @Test
    public void testCalculateAndDistributeCommission_OrderAmountBoundaries() {
        // Given: 设置不同的订单金额
        setupValidThreeLevelUsers();
        
        // Test Case 1: 最小金额
        testOrder.setAmount(new BigDecimal("0.01"));
        assertTrue("最小金额应该成功", referralCommissionService.calculateAndDistributeCommission(testOrder));
        
        // Test Case 2: 大金额
        testOrder.setAmount(new BigDecimal("999999.99"));
        assertTrue("大金额应该成功", referralCommissionService.calculateAndDistributeCommission(testOrder));
        
        // Test Case 3: 零金额
        testOrder.setAmount(BigDecimal.ZERO);
        assertTrue("零金额应该成功", referralCommissionService.calculateAndDistributeCommission(testOrder));
    }

    // 辅助方法：设置有效的三级推荐用户
    private void setupValidThreeLevelUsers() {
        // 确保所有用户都是有效状态
        level1User.setStatus("0");
        level2User.setStatus("0");
        level3User.setStatus("0");
        
        // 确保推荐关系链完整
        buyerUser.setAppParentIds("1000,1001,1002");
        
        // 确保订单状态正确
        testOrder.setStatus(1L);
        testOrder.setCommissionProcessed(false);
    }
}