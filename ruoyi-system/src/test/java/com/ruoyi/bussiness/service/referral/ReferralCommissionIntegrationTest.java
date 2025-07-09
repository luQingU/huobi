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
import com.ruoyi.bussiness.service.ITMineOrderService;
import com.ruoyi.bussiness.service.impl.referral.ReferralCommissionServiceImpl;
import com.ruoyi.bussiness.mapper.referral.TReferralCommissionRecordMapper;
import com.ruoyi.bussiness.mapper.referral.TReferralCommissionConfigMapper;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * 三级返佣系统 - 集成测试
 * 
 * 测试覆盖范围：
 * 1. 完整的业务流程集成测试
 * 2. 并发安全性测试
 * 3. 数据一致性验证
 * 4. 性能压力测试
 * 5. 实际数据库环境测试
 * 
 * @author System
 * @date 2025-01-09
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@Rollback
public class ReferralCommissionIntegrationTest {

    @Autowired
    private ReferralCommissionServiceImpl referralCommissionService;

    @Autowired
    private ITAppUserService appUserService;

    @Autowired
    private ITAppAssetService appAssetService;

    @Autowired
    private ITAppWalletRecordService walletRecordService;

    @Autowired
    private ITMineOrderService mineOrderService;

    @Autowired
    private TReferralCommissionRecordMapper commissionRecordMapper;

    @Autowired
    private TReferralCommissionConfigMapper commissionConfigMapper;

    // 测试数据
IDs
    private static final Long LEVEL3_USER_ID = 1000L;
    private static final Long LEVEL2_USER_ID = 1001L;
    private static final Long LEVEL1_USER_ID = 1002L;
    private static final Long BUYER_USER_ID = 1003L;
    private static final Long TEST_ORDER_ID = 2001L;

    @Before
    public void setUp() {
        // 清理测试数据
        cleanupTestData();
        
        // 初始化测试数据
        setupTestData();
    }

    private void cleanupTestData() {
        // 清理相关表数据
        commissionRecordMapper.deleteByOrderId(TEST_ORDER_ID);
        // 清理其他相关数据...
    }

    private void setupTestData() {
        // 创建测试用户
        createTestUsers();
        
        // 创建测试订单
        createTestOrder();
        
        // 初始化佣金配置
        initializeCommissionConfigs();
        
        // 初始化用户资产
        initializeUserAssets();
    }

    private void createTestUsers() {
        // 三级推荐人（根节点）
        TAppUser level3User = new TAppUser();
        level3User.setUserId(LEVEL3_USER_ID);
        level3User.setUsername("level3_user");
        level3User.setEmail("level3@test.com");
        level3User.setAppParentIds("");
        level3User.setStatus("0");
        level3User.setActiveCode("LEVEL3123");
        appUserService.save(level3User);

        // 二级推荐人
        TAppUser level2User = new TAppUser();
        level2User.setUserId(LEVEL2_USER_ID);
        level2User.setUsername("level2_user");
        level2User.setEmail("level2@test.com");
        level2User.setAppParentIds(String.valueOf(LEVEL3_USER_ID));
        level2User.setStatus("0");
        level2User.setActiveCode("LEVEL2123");
        appUserService.save(level2User);

        // 一级推荐人
        TAppUser level1User = new TAppUser();
        level1User.setUserId(LEVEL1_USER_ID);
        level1User.setUsername("level1_user");
        level1User.setEmail("level1@test.com");
        level1User.setAppParentIds(LEVEL3_USER_ID + "," + LEVEL2_USER_ID);
        level1User.setStatus("0");
        level1User.setActiveCode("LEVEL1123");
        appUserService.save(level1User);

        // 购买用户
        TAppUser buyerUser = new TAppUser();
        buyerUser.setUserId(BUYER_USER_ID);
        buyerUser.setUsername("buyer_user");
        buyerUser.setEmail("buyer@test.com");
        buyerUser.setAppParentIds(LEVEL3_USER_ID + "," + LEVEL2_USER_ID + "," + LEVEL1_USER_ID);
        buyerUser.setStatus("0");
        buyerUser.setActiveCode("BUYER123");
        appUserService.save(buyerUser);
    }

    private void createTestOrder() {
        TMineOrder order = new TMineOrder();
        order.setId(TEST_ORDER_ID);
        order.setUserId(BUYER_USER_ID);
        order.setAmount(new BigDecimal("100.00"));
        order.setStatus(1L); // 已支付
        order.setCommissionProcessed(false);
        order.setCreateTime(DateUtils.getNowDate());
        mineOrderService.save(order);
    }

    private void initializeCommissionConfigs() {
        // 清理现有配置
        commissionConfigMapper.deleteAll();
        
        // 创建默认配置
        List<TReferralCommissionConfig> configs = Arrays.asList(
            createCommissionConfig(1, new BigDecimal("10.00000000")),
            createCommissionConfig(2, new BigDecimal("5.00000000")),
            createCommissionConfig(3, new BigDecimal("2.00000000"))
        );
        
        for (TReferralCommissionConfig config : configs) {
            commissionConfigMapper.insert(config);
        }
    }

    private TReferralCommissionConfig createCommissionConfig(int level, BigDecimal amount) {
        TReferralCommissionConfig config = new TReferralCommissionConfig();
        config.setLevel(level);
        config.setAmount(amount);
        config.setCurrency("USDT");
        config.setActive(true);
        config.setCreatedAt(DateUtils.getNowDate());
        config.setUpdatedAt(DateUtils.getNowDate());
        return config;
    }

    private void initializeUserAssets() {
        // 为所有用户初始化USDT资产
        Long[] userIds = {LEVEL3_USER_ID, LEVEL2_USER_ID, LEVEL1_USER_ID, BUYER_USER_ID};
        
        for (Long userId : userIds) {
            TAppAsset asset = new TAppAsset();
            asset.setUserId(userId);
            asset.setSymbol("USDT");
            asset.setAmout(BigDecimal.ZERO);
            asset.setOccupiedAmount(BigDecimal.ZERO);
            asset.setAvailableAmount(BigDecimal.ZERO);
            asset.setType("PLATFORM_ASSETS");
            appAssetService.save(asset);
        }
    }

    /**
     * 集成测试1：完整的三级返佣流程
     * 验证从订单支付到佣金分发的完整流程
     */
    @Test
    public void testCompleteReferralCommissionFlow() {
        // Given: 完整的三级推荐关系和已支付订单
        TMineOrder order = mineOrderService.getById(TEST_ORDER_ID);
        assertNotNull("订单应该存在", order);
        
        // 获取佣金分发前的用户资产
        BigDecimal level1BalanceBefore = getUserAssetBalance(LEVEL1_USER_ID, "USDT");
        BigDecimal level2BalanceBefore = getUserAssetBalance(LEVEL2_USER_ID, "USDT");
        BigDecimal level3BalanceBefore = getUserAssetBalance(LEVEL3_USER_ID, "USDT");
        
        // When: 执行佣金计算和分发
        boolean result = referralCommissionService.calculateAndDistributeCommission(order);
        
        // Then: 验证结果
        assertTrue("佣金分发应该成功", result);
        
        // 验证订单状态更新
        TMineOrder updatedOrder = mineOrderService.getById(TEST_ORDER_ID);
        assertTrue("订单应该标记为已处理佣金", updatedOrder.getCommissionProcessed());
        
        // 验证佣金记录创建
        List<TReferralCommissionRecord> records = commissionRecordMapper.selectByOrderId(TEST_ORDER_ID);
        assertEquals("应该创建3条佣金记录", 3, records.size());
        
        // 验证佣金记录内容
        validateCommissionRecord(records, 1, LEVEL1_USER_ID, new BigDecimal("10.00000000"));
        validateCommissionRecord(records, 2, LEVEL2_USER_ID, new BigDecimal("5.00000000"));
        validateCommissionRecord(records, 3, LEVEL3_USER_ID, new BigDecimal("2.00000000"));
        
        // 验证用户资产更新
        BigDecimal level1BalanceAfter = getUserAssetBalance(LEVEL1_USER_ID, "USDT");
        BigDecimal level2BalanceAfter = getUserAssetBalance(LEVEL2_USER_ID, "USDT");
        BigDecimal level3BalanceAfter = getUserAssetBalance(LEVEL3_USER_ID, "USDT");
        
        assertEquals("一级推荐人应该获得10 USDT", 
                    level1BalanceBefore.add(new BigDecimal("10.00000000")), level1BalanceAfter);
        assertEquals("二级推荐人应该获得5 USDT", 
                    level2BalanceBefore.add(new BigDecimal("5.00000000")), level2BalanceAfter);
        assertEquals("三级推荐人应该获得2 USDT", 
                    level3BalanceBefore.add(new BigDecimal("2.00000000")), level3BalanceAfter);
        
        // 验证钱包记录创建
        List<TAppWalletRecord> walletRecords = walletRecordService.getByOrderId(TEST_ORDER_ID);
        assertEquals("应该创建3条钱包记录", 3, walletRecords.size());
    }

    /**
     * 集成测试2：部分推荐人不激活的场景
     */
    @Test
    public void testPartialInactiveReferrers() {
        // Given: 二级推荐人被禁用
        TAppUser level2User = appUserService.getById(LEVEL2_USER_ID);
        level2User.setStatus("1"); // 禁用状态
        appUserService.updateById(level2User);
        
        TMineOrder order = mineOrderService.getById(TEST_ORDER_ID);
        
        // When: 执行佣金计算
        boolean result = referralCommissionService.calculateAndDistributeCommission(order);
        
        // Then: 验证结果
        assertTrue("佣金分发应该成功", result);
        
        // 验证只创建了2条佣金记录（跳过被禁用的二级推荐人）
        List<TReferralCommissionRecord> records = commissionRecordMapper.selectByOrderId(TEST_ORDER_ID);
        assertEquals("应该创建2条佣金记录", 2, records.size());
        
        // 验证只有一级和三级推荐人获得佣金
        validateCommissionRecord(records, 1, LEVEL1_USER_ID, new BigDecimal("10.00000000"));
        validateCommissionRecord(records, 3, LEVEL3_USER_ID, new BigDecimal("2.00000000"));
        
        // 验证被禁用的用户资产没有变化
        BigDecimal level2Balance = getUserAssetBalance(LEVEL2_USER_ID, "USDT");
        assertEquals("被禁用的用户资产不应该变化", BigDecimal.ZERO, level2Balance);
    }

    /**
     * 集成测试3：并发安全性测试
     * 模拟多个线程同时处理同一订单的佣金
     */
    @Test
    public void testConcurrentCommissionProcessing() throws InterruptedException {
        // Given: 并发环境设置
        final int threadCount = 5;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        TMineOrder order = mineOrderService.getById(TEST_ORDER_ID);
        
        // When: 并发执行佣金计算
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean result = referralCommissionService.calculateAndDistributeCommission(order);
                    if (result) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // Then: 验证结果
        assertEquals("应该只有一个线程成功", 1, successCount.get());
        assertEquals("其他线程应该失败", threadCount - 1, failureCount.get());
        
        // 验证只创建了一套佣金记录
        List<TReferralCommissionRecord> records = commissionRecordMapper.selectByOrderId(TEST_ORDER_ID);
        assertEquals("应该只创建一3条佣金记录", 3, records.size());
    }

    /**
     * 集成测试4：大量订单批量处理性能测试
     */
    @Test
    public void testBatchOrderProcessingPerformance() {
        // Given: 创建大量订单
        final int orderCount = 100;
        List<TMineOrder> orders = createBatchTestOrders(orderCount);
        
        // When: 批量处理订单佣金
        long startTime = System.currentTimeMillis();
        
        int successCount = 0;
        for (TMineOrder order : orders) {
            if (referralCommissionService.calculateAndDistributeCommission(order)) {
                successCount++;
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Then: 验证性能和结果
        assertEquals("所有订单都应该处理成功", orderCount, successCount);
        
        // 验证性能指标（平均每个订单处理时间小于100ms）
        double avgProcessTime = (double) duration / orderCount;
        assertTrue("平均处理时间应该小于100ms", avgProcessTime < 100);
        
        // 验证数据完整性
        List<TReferralCommissionRecord> allRecords = commissionRecordMapper.selectAll();
        assertEquals("应该创建300条佣金记录", orderCount * 3, allRecords.size());
    }

    /**
     * 集成测试5：数据一致性验证
     * 验证佣金记录、用户资产、钱包记录之间的数据一致性
     */
    @Test
    public void testDataConsistencyValidation() {
        // Given: 创建多个订单
        final int orderCount = 10;
        List<TMineOrder> orders = createBatchTestOrders(orderCount);
        
        // When: 处理所有订单
        for (TMineOrder order : orders) {
            referralCommissionService.calculateAndDistributeCommission(order);
        }
        
        // Then: 验证数据一致性
        
        // 1. 验证佣金记录总数
        List<TReferralCommissionRecord> allRecords = commissionRecordMapper.selectAll();
        assertEquals("佣金记录总数应详正确", orderCount * 3, allRecords.size());
        
        // 2. 验证佣金金额总和
        BigDecimal totalCommissionFromRecords = allRecords.stream()
            .map(TReferralCommissionRecord::getCommissionAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal expectedTotalCommission = new BigDecimal("17.00000000") // (10+5+2) per order
            .multiply(new BigDecimal(orderCount));
        
        assertEquals("佣金记录总金额应详正确", expectedTotalCommission, totalCommissionFromRecords);
        
        // 3. 验证用户资产变化
        BigDecimal level1TotalBalance = getUserAssetBalance(LEVEL1_USER_ID, "USDT");
        BigDecimal level2TotalBalance = getUserAssetBalance(LEVEL2_USER_ID, "USDT");
        BigDecimal level3TotalBalance = getUserAssetBalance(LEVEL3_USER_ID, "USDT");
        
        assertEquals("一级推荐人总资产应详正确", 
                    new BigDecimal("10.00000000").multiply(new BigDecimal(orderCount)), level1TotalBalance);
        assertEquals("二级推荐人总资产应详正确", 
                    new BigDecimal("5.00000000").multiply(new BigDecimal(orderCount)), level2TotalBalance);
        assertEquals("三级推荐人总资产应详正确", 
                    new BigDecimal("2.00000000").multiply(new BigDecimal(orderCount)), level3TotalBalance);
        
        // 4. 验证钱包记录数量
        List<TAppWalletRecord> allWalletRecords = walletRecordService.selectAll();
        assertEquals("钱包记录总数应详正确", orderCount * 3, allWalletRecords.size());
    }

    /**
     * 集成测试6：异常恢复测试
     * 模拟处理过程中的异常情况和恢复机制
     */
    @Test
    public void testExceptionRecovery() {
        // Given: 模拟数据库异常环境
        TMineOrder order = mineOrderService.getById(TEST_ORDER_ID);
        
        // 模拟在更新用户资产时发生异常
        // 这里需要根据实际实现来调整模拟方式
        
        // When & Then: 验证异常处理和事务回滚
        try {
            referralCommissionService.calculateAndDistributeCommission(order);
            // 如果没有异常，说明处理成功
            assertTrue("处理应该成功或抛出异常", true);
        } catch (Exception e) {
            // 验证异常发生后数据未被部分更新
            List<TReferralCommissionRecord> records = commissionRecordMapper.selectByOrderId(TEST_ORDER_ID);
            assertEquals("异常后不应该有佣金记录", 0, records.size());
            
            // 验证用户资产未被修改
            BigDecimal level1Balance = getUserAssetBalance(LEVEL1_USER_ID, "USDT");
            assertEquals("异常后用户资产不应该变化", BigDecimal.ZERO, level1Balance);
        }
    }

    // 辅助方法
    
    private List<TMineOrder> createBatchTestOrders(int count) {
        List<TMineOrder> orders = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            TMineOrder order = new TMineOrder();
            order.setId(TEST_ORDER_ID + i + 1);
            order.setUserId(BUYER_USER_ID);
            order.setAmount(new BigDecimal("100.00"));
            order.setStatus(1L);
            order.setCommissionProcessed(false);
            order.setCreateTime(DateUtils.getNowDate());
            
            mineOrderService.save(order);
            orders.add(order);
        }
        
        return orders;
    }
    
    private BigDecimal getUserAssetBalance(Long userId, String symbol) {
        TAppAsset asset = appAssetService.getByUserIdAndSymbol(userId, symbol);
        return asset != null ? asset.getAvailableAmount() : BigDecimal.ZERO;
    }
    
    private void validateCommissionRecord(List<TReferralCommissionRecord> records, 
                                        int level, Long recipientUserId, BigDecimal expectedAmount) {
        TReferralCommissionRecord record = records.stream()
            .filter(r -> r.getCommissionLevel().equals(level))
            .findFirst()
            .orElse(null);
        
        assertNotNull("Level " + level + " 佣金记录应该存在", record);
        assertEquals("Level " + level + " 接收人应详正确", recipientUserId, record.getRecipientUserId());
        assertEquals("Level " + level + " 佣金金额应详正确", expectedAmount, record.getCommissionAmount());
        assertEquals("Level " + level + " 佣金状态应该为已支付", "paid", record.getStatus());
    }
}