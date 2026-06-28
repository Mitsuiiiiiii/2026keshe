-- =============================================================
--  CampusLink v3 扩容增量脚本（功能补全）
--  执行前提：v1 sql/campuslink.sql + v2 sql/v2_extension.sql 已执行
--  执行命令：mysql --default-character-set=utf8mb4 -uroot -p campuslink < sql/v3_extension.sql
--  原则：只新增/追加，不删除任何原表与原列
-- =============================================================

SET NAMES utf8mb4;
USE campuslink;

-- ---------------------------------------------------------
-- v1/v2 表追加列
-- ---------------------------------------------------------
-- 评价匿名切换
ALTER TABLE `evaluation`
  ADD COLUMN `anonymous` TINYINT NOT NULL DEFAULT 0 COMMENT '0实名/1匿名' AFTER `communication`;

-- 招募标签高亮 + 置顶规范化
ALTER TABLE `team_recruit`
  ADD COLUMN `tags`   VARCHAR(200) DEFAULT NULL COMMENT '招募标签(逗号分隔，前端高亮)',
  ADD COLUMN `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '0普通/1置顶';

-- 收藏订阅推送开关
ALTER TABLE `user_privacy`
  ADD COLUMN `push_favorite` TINYINT NOT NULL DEFAULT 1 COMMENT '收藏对象更新推送开关' AFTER `push_eval`;

-- ---------------------------------------------------------
-- 新表 1：reputation_log 信誉分变动记录
-- ---------------------------------------------------------
CREATE TABLE `reputation_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`      BIGINT       NOT NULL COMMENT '用户 id',
    `change_value` DECIMAL(5,2) NOT NULL COMMENT '本次变动值(可正负)',
    `before_value` DECIMAL(5,2) DEFAULT NULL COMMENT '变动前信誉分',
    `after_value`  DECIMAL(5,2) DEFAULT NULL COMMENT '变动后信誉分',
    `source_type`  VARCHAR(20)  NOT NULL COMMENT 'EVAL/ADMIN/SYSTEM',
    `reason`       VARCHAR(200) DEFAULT NULL COMMENT '变动原因/明细',
    `ref_id`       BIGINT       DEFAULT NULL COMMENT '关联业务 id(如 evaluation.id)',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    PRIMARY KEY (`id`),
    KEY `idx_replog_user` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信誉分变动记录表';

-- ---------------------------------------------------------
-- 新表 2：competition_schedule 赛事日程时间表
-- ---------------------------------------------------------
CREATE TABLE `competition_schedule` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `competition_id` BIGINT       NOT NULL COMMENT '竞赛 id',
    `stage`          VARCHAR(50)  NOT NULL COMMENT '阶段名(报名/初赛/复赛/决赛/颁奖)',
    `title`          VARCHAR(100) DEFAULT NULL COMMENT '日程标题',
    `start_time`     DATETIME     DEFAULT NULL COMMENT '开始时间',
    `end_time`       DATETIME     DEFAULT NULL COMMENT '结束时间',
    `location`       VARCHAR(100) DEFAULT NULL COMMENT '地点',
    `remark`         VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `sort_order`     INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    PRIMARY KEY (`id`),
    KEY `idx_sched_comp` (`competition_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛事日程时间表';

-- ---------------------------------------------------------
-- 新表 3：competition_award 赛事获奖公示
-- ---------------------------------------------------------
CREATE TABLE `competition_award` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `competition_id` BIGINT       NOT NULL COMMENT '竞赛 id',
    `team_id`        BIGINT       DEFAULT NULL COMMENT '获奖队伍 id(可空)',
    `team_name`      VARCHAR(100) NOT NULL COMMENT '获奖队伍名称(冗余)',
    `award_level`    VARCHAR(50)  NOT NULL COMMENT '奖项等级(国一/国二/省一等)',
    `award_year`     INT          NOT NULL COMMENT '获奖年份',
    `member_count`   INT          NOT NULL DEFAULT 0 COMMENT '获奖人数',
    `members`        VARCHAR(500) DEFAULT NULL COMMENT '获奖成员名单(逗号分隔)',
    `publisher_id`   BIGINT       DEFAULT NULL COMMENT '公示发布人(管理员)',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '公示时间',
    PRIMARY KEY (`id`),
    KEY `idx_award_comp` (`competition_id`),
    KEY `idx_award_year` (`award_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛事获奖公示表';

-- ---------------------------------------------------------
-- 新表 4：team_event 队伍关键事件时间线（动态墙）
-- ---------------------------------------------------------
CREATE TABLE `team_event` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `team_id`     BIGINT       NOT NULL COMMENT '队伍 id',
    `type`        VARCHAR(20)  NOT NULL COMMENT 'JOIN/TASK_DONE/AWARD/ARCHIVE/NOTICE/DEPUTY',
    `content`     VARCHAR(255) NOT NULL COMMENT '事件描述',
    `actor_id`    BIGINT       DEFAULT NULL COMMENT '触发人 id',
    `ref_id`      BIGINT       DEFAULT NULL COMMENT '关联业务 id',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    PRIMARY KEY (`id`),
    KEY `idx_event_team` (`team_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='队伍关键事件时间线表';

-- ---------------------------------------------------------
-- 字典/种子数据
-- ---------------------------------------------------------
INSERT INTO `dict` (`type`,`code`,`label`,`sort`) VALUES
('AWARD_LEVEL','NATIONAL_FIRST','国家级一等奖',1),
('AWARD_LEVEL','NATIONAL_SECOND','国家级二等奖',2),
('AWARD_LEVEL','PROVINCE_FIRST','省级一等奖',3),
('AWARD_LEVEL','PROVINCE_SECOND','省级二等奖',4),
('SCHEDULE_STAGE','SIGNUP','报名',1),
('SCHEDULE_STAGE','PRELIM','初赛',2),
('SCHEDULE_STAGE','SEMI','复赛',3),
('SCHEDULE_STAGE','FINAL','决赛',4),
('SCHEDULE_STAGE','AWARD','颁奖',5);

-- 获奖公示示例数据（用于排行榜/获奖人数统计演示）
INSERT INTO `competition_award`
  (`competition_id`,`team_id`,`team_name`,`award_level`,`award_year`,`member_count`,`members`) VALUES
(1, 1, '代码先锋队', '国家级一等奖', 2025, 4, 'Alice,Bob,Carol,Dave'),
(2, 2, '建模梦之队', '省级一等奖', 2025, 3, 'Alice,Eve,Frank'),
(1, NULL, '极客突击队', '国家级二等奖', 2024, 5, '历年获奖队伍示例');
