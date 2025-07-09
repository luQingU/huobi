-- 三级返佣系统数据库表结构
-- 根据设计文档创建相关表结构
-- 日期：2025-01-09

-- 1. 佣金配置表
CREATE TABLE `t_referral_commission_config` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `level` int NOT NULL COMMENT '层级（1-一级，2-二级，3-三级）',
    `amount` decimal(20,8) NOT NULL COMMENT '佣金金额',
    `currency` varchar(10) NOT NULL DEFAULT 'USDT' COMMENT '货币类型',
    `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用（1-启用，0-停用）',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remarks` varchar(255) DEFAULT NULL COMMENT '备注信息',
    `version` bigint NOT NULL DEFAULT 1 COMMENT '数据版本号（乐观锁）',
    `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0-未删除，1-已删除）',
    `min_order_amount` decimal(20,8) DEFAULT NULL COMMENT '最小订单金额（达到该金额才能触发佣金）',
    `max_commission_amount` decimal(20,8) DEFAULT NULL COMMENT '最大佣金金额（单次佣金上限）',
    `valid_from` timestamp NULL DEFAULT NULL COMMENT '有效期开始时间',
    `valid_to` timestamp NULL DEFAULT NULL COMMENT '有效期结束时间',
    `sort_order` int DEFAULT 0 COMMENT '排序顺序',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_level` (`level`, `deleted`),
    KEY `idx_active` (`active`),
    KEY `idx_currency` (`currency`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='佣金配置表';

-- 2. 佣金记录表
CREATE TABLE `t_referral_commission_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `order_id` bigint NOT NULL COMMENT '触发订单ID',
    `recipient_user_id` bigint NOT NULL COMMENT '接收佣金用户ID',
    `payer_user_id` bigint NOT NULL COMMENT '产生佣金用户ID',
    `commission_amount` decimal(20,8) NOT NULL COMMENT '佣金金额',
    `commission_level` int NOT NULL COMMENT '佣金层级',
    `commission_rate` decimal(20,8) NOT NULL COMMENT '佣金标准（对应层级的固定金额）',
    `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '佣金状态（pending-待处理，paid-已支付，failed-失败，expired-过期）',
    `transaction_id` varchar(100) DEFAULT NULL COMMENT '交易ID',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `processed_at` timestamp NULL DEFAULT NULL COMMENT '处理时间',
    `currency` varchar(10) NOT NULL DEFAULT 'USDT' COMMENT '货币类型',
    `remarks` varchar(255) DEFAULT NULL COMMENT '备注信息',
    `referral_path` varchar(255) DEFAULT NULL COMMENT '推荐路径',
    `version` bigint NOT NULL DEFAULT 1 COMMENT '数据版本号（乐观锁）',
    `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_recipient_level` (`order_id`, `recipient_user_id`, `commission_level`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_recipient_user_id` (`recipient_user_id`),
    KEY `idx_payer_user_id` (`payer_user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_commission_level` (`commission_level`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_processed_at` (`processed_at`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='佣金记录表';

-- 3. 为已有订单表添加佣金处理标识字段
ALTER TABLE `t_mine_order` 
ADD COLUMN `commission_processed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '推荐佣金是否已处理（0-未处理，1-已处理）',
ADD KEY `idx_commission_processed` (`commission_processed`);

-- 4. 插入默认佣金配置
INSERT INTO `t_referral_commission_config` (`level`, `amount`, `currency`, `active`, `created_at`, `updated_at`) VALUES
(1, 10.00000000, 'USDT', 1, NOW(), NOW()),
(2, 5.00000000, 'USDT', 1, NOW(), NOW()),
(3, 2.00000000, 'USDT', 1, NOW(), NOW());

-- 5. 创建索引优化查询性能
-- 推荐关系查询优化索引（基于现有用户表的 app_parent_ids 字段）
CREATE INDEX `idx_app_parent_ids` ON `t_app_user` (`app_parent_ids`);

-- 佣金记录复合索引
CREATE INDEX `idx_recipient_date` ON `t_referral_commission_record` (`recipient_user_id`, `created_at`);
CREATE INDEX `idx_payer_date` ON `t_referral_commission_record` (`payer_user_id`, `created_at`);
CREATE INDEX `idx_status_date` ON `t_referral_commission_record` (`status`, `created_at`);

-- 6. 创建用于统计的视图
CREATE VIEW `v_commission_statistics` AS
SELECT 
    r.recipient_user_id,
    u.username,
    r.commission_level,
    COUNT(*) as record_count,
    SUM(r.commission_amount) as total_amount,
    SUM(CASE WHEN r.status = 'paid' THEN r.commission_amount ELSE 0 END) as paid_amount,
    SUM(CASE WHEN r.status = 'pending' THEN r.commission_amount ELSE 0 END) as pending_amount,
    SUM(CASE WHEN r.status = 'failed' THEN r.commission_amount ELSE 0 END) as failed_amount
FROM `t_referral_commission_record` r
LEFT JOIN `t_app_user` u ON r.recipient_user_id = u.user_id
WHERE r.deleted = 0
GROUP BY r.recipient_user_id, r.commission_level;

-- 7. 创建用于监控的视图
CREATE VIEW `v_commission_summary` AS
SELECT 
    DATE(created_at) as date,
    commission_level,
    status,
    COUNT(*) as record_count,
    SUM(commission_amount) as total_amount,
    AVG(commission_amount) as avg_amount
FROM `t_referral_commission_record`
WHERE deleted = 0
GROUP BY DATE(created_at), commission_level, status
ORDER BY date DESC, commission_level;

-- 8. 创建处理失败佣金的存储过程
DELIMITER $$
CREATE PROCEDURE `proc_retry_failed_commissions`(IN limit_count INT)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE record_id BIGINT;
    DECLARE cur CURSOR FOR 
        SELECT id FROM t_referral_commission_record 
        WHERE status = 'failed' AND deleted = 0 
        ORDER BY created_at ASC 
        LIMIT limit_count;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO record_id;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- 重置为待处理状态
        UPDATE t_referral_commission_record 
        SET status = 'pending', processed_at = NULL 
        WHERE id = record_id;
        
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

-- 9. 创建清理过期佣金的存储过程
DELIMITER $$
CREATE PROCEDURE `proc_expire_old_commissions`(IN expire_days INT)
BEGIN
    UPDATE t_referral_commission_record 
    SET status = 'expired', processed_at = NOW()
    WHERE status = 'pending' 
    AND created_at < DATE_SUB(NOW(), INTERVAL expire_days DAY)
    AND deleted = 0;
END$$
DELIMITER ;

-- 10. 创建数据一致性检查存储过程
DELIMITER $$
CREATE PROCEDURE `proc_check_commission_consistency`()
BEGIN
    -- 检查订单和佣金记录的一致性
    SELECT 
        o.id as order_id,
        o.user_id,
        o.commission_processed,
        COUNT(r.id) as commission_records
    FROM t_mine_order o
    LEFT JOIN t_referral_commission_record r ON o.id = r.order_id AND r.deleted = 0
    WHERE o.status = 1 AND o.commission_processed = 1
    GROUP BY o.id
    HAVING commission_records = 0;
END$$
DELIMITER ;

-- 11. 创建触发器确保数据一致性
DELIMITER $$
CREATE TRIGGER `tr_commission_record_insert` 
BEFORE INSERT ON `t_referral_commission_record`
FOR EACH ROW
BEGIN
    -- 确保佣金层级在有效范围内
    IF NEW.commission_level < 1 OR NEW.commission_level > 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Commission level must be between 1 and 3';
    END IF;
    
    -- 确保佣金金额为正数
    IF NEW.commission_amount <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Commission amount must be positive';
    END IF;
    
    -- 设置默认值
    IF NEW.created_at IS NULL THEN
        SET NEW.created_at = NOW();
    END IF;
END$$
DELIMITER ;

-- 12. 创建性能监控表
CREATE TABLE `t_commission_performance_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `order_id` bigint NOT NULL COMMENT '订单ID',
    `process_start_time` timestamp NOT NULL COMMENT '处理开始时间',
    `process_end_time` timestamp NULL COMMENT '处理结束时间',
    `processing_duration` int NULL COMMENT '处理时长（毫秒）',
    `commission_count` int NOT NULL DEFAULT 0 COMMENT '处理的佣金记录数',
    `success` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否成功（0-失败，1-成功）',
    `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_success` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='佣金处理性能日志表';