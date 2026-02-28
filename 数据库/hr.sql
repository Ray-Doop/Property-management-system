/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : hr

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 27/02/2026 17:25:31
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for access_log
-- ----------------------------
DROP TABLE IF EXISTS `access_log`;
CREATE TABLE `access_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL,
  `admin_id` bigint NULL DEFAULT NULL,
  `employee_id` bigint NULL DEFAULT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `role` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `query` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status_code` int NULL DEFAULT NULL,
  `duration_ms` bigint NULL DEFAULT NULL,
  `error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 467 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `admin_id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员唯一ID（主键）',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录用户名（唯一）',
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'BCrypt加密密码',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'OPERATOR' COMMENT '角色：SUPER_ADMIN/ADMIN/OPERATOR',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态：0-禁用，1-启用',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系手机号（唯一）',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系邮箱（唯一）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '管理员备注信息',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像存储路径（如OSS地址）',
  PRIMARY KEY (`admin_id`) USING BTREE,
  UNIQUE INDEX `idx_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `idx_phone`(`phone` ASC) USING BTREE,
  UNIQUE INDEX `idx_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for employee
-- ----------------------------
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee`  (
  `employee_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '员工唯一标识ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '员工登录用的用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '员工登录密码（建议加密存储）',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '员工最后一次登录系统的时间',
  `register_time` datetime NOT NULL COMMENT '员工账号注册的时间',
  `permission` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '员工的权限等级（如管理员、普通员工）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '账号的状态（如正常、禁用、锁定）',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '员工的昵称',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '员工头像图片的存储路径',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '员工的联系手机号',
  `specialty` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '擅长邻域',
  PRIMARY KEY (`employee_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 669 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '员工信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fee_bill
-- ----------------------------
DROP TABLE IF EXISTS `fee_bill`;
CREATE TABLE `fee_bill`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账单编号（业务唯一，如 FB202508180001）',
  `residence_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '住宅ID（楼栋-单元-房号，例如 1-2-302）',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账单标题（如 2025年8月物业费）',
  `unit_price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '单价（元/㎡·月等，便于审计）',
  `area_snapshot` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '计费面积快照（下单时记录，避免后续面积变动影响）',
  `period_start` date NOT NULL COMMENT '计费周期开始日期',
  `period_end` date NOT NULL COMMENT '计费周期结束日期',
  `amount` decimal(10, 2) NOT NULL COMMENT '应付金额（元）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '账单状态：0-已取消，1-待支付，2-已支付，3-已关闭',
  `pay_order_id` bigint NULL DEFAULT NULL COMMENT '最近一次关联的支付订单ID（便于追溯）',
  `due_date` date NULL DEFAULT NULL COMMENT '到期日（逾期可加提醒或滞纳金策略）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_bill_no`(`bill_no` ASC) USING BTREE,
  INDEX `idx_residence_status`(`residence_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 64 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '物业费用账单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fee_detailed
-- ----------------------------
DROP TABLE IF EXISTS `fee_detailed`;
CREATE TABLE `fee_detailed`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `residence_id` bigint NOT NULL COMMENT '住户ID',
  `order_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `water_fee` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '水费金额',
  `water_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '水费备注',
  `electricity_fee` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '电费金额',
  `electricity_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '电费备注',
  `property_fee` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '物业管理费金额',
  `property_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '物业管理费备注',
  `parking_fee` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '停车费金额',
  `parking_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '停车费备注',
  `visitor_parking_fee` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '外来车辆停车费金额',
  `visitor_parking_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '外来车辆停车费备注',
  `other_fee` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '其他费用金额',
  `other_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '其他费用备注',
  `total_amount` decimal(12, 2) GENERATED ALWAYS AS ((((((`water_fee` + `electricity_fee`) + `property_fee`) + `parking_fee`) + `visitor_parking_fee`) + `other_fee`)) STORED COMMENT '总计金额' NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '住户费用明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_attachment
-- ----------------------------
DROP TABLE IF EXISTS `forum_attachment`;
CREATE TABLE `forum_attachment`  (
  `attachment_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '附件ID（主键）',
  `post_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '所属帖子ID（关联forum_post.post_id）',
  `comment_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '所属评论ID（关联forum_comment.comment_id）',
  `file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '存储路径',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '上传人ID（关联user.user_id）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`attachment_id`) USING BTREE,
  INDEX `idx_post_id`(`post_id` ASC) USING BTREE,
  INDEX `idx_comment_id`(`comment_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_attachment_comment` FOREIGN KEY (`comment_id`) REFERENCES `forum_comment` (`comment_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_attachment_post` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`post_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_attachment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '邻里交流附件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_collect
-- ----------------------------
DROP TABLE IF EXISTS `forum_collect`;
CREATE TABLE `forum_collect`  (
  `user_id` int NOT NULL,
  `post_id` int NULL DEFAULT NULL
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_comment
-- ----------------------------
DROP TABLE IF EXISTS `forum_comment`;
CREATE TABLE `forum_comment`  (
  `comment_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID（主键）',
  `post_id` bigint UNSIGNED NOT NULL COMMENT '所属帖子ID（关联forum_post.post_id）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '评论人ID（关联user.user_id）',
  `parent_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '父评论ID（关联forum_comment.comment_id，用于回复评论）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-删除，1-正常',
  `like_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '追评数',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`comment_id`) USING BTREE,
  INDEX `idx_post_id`(`post_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  CONSTRAINT `fk_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `forum_comment` (`comment_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`post_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '邻里交流评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_post
-- ----------------------------
DROP TABLE IF EXISTS `forum_post`;
CREATE TABLE `forum_post`  (
  `post_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '帖子ID（主键）',
  `section_id` int UNSIGNED NOT NULL COMMENT '所属板块ID（关联forum_section.section_id）',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '帖子标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '帖子内容',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '发帖人ID（关联user.user_id）',
  `is_top` tinyint NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
  `is_essence` tinyint NOT NULL DEFAULT 0 COMMENT '是否精华：0-否，1-是',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-删除，1-正常，2-加精，3-置顶',
  `collect_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `comment_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '评论数',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_comment_time` datetime NULL DEFAULT NULL COMMENT '最后评论时间',
  `section_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`post_id`) USING BTREE,
  INDEX `idx_section_id`(`section_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_is_top`(`is_top` ASC) USING BTREE,
  INDEX `idx_created_time`(`created_time` ASC) USING BTREE,
  INDEX `idx_last_comment_time`(`last_comment_time` ASC) USING BTREE,
  CONSTRAINT `fk_post_section` FOREIGN KEY (`section_id`) REFERENCES `forum_section` (`section_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 113 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '邻里交流帖子表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for forum_section
-- ----------------------------
DROP TABLE IF EXISTS `forum_section`;
CREATE TABLE `forum_section`  (
  `section_id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '板块ID（主键）',
  `section_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '板块名称',
  `section_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '板块描述',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序字段（数值越小越靠前）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_by` bigint UNSIGNED NOT NULL COMMENT '创建人（关联user_id）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`section_id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 41 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '邻里交流板块表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for login_log
-- ----------------------------
DROP TABLE IF EXISTS `login_log`;
CREATE TABLE `login_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL,
  `admin_id` bigint NULL DEFAULT NULL,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `success` tinyint NOT NULL DEFAULT 0,
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_agent` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_key_time`(`login_time` ASC) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE,
  INDEX `idx_role_success`(`role` ASC, `success` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
  `notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告唯一ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告内容',
  `publish_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '公告发布时间',
  `publisher_id` bigint NOT NULL COMMENT '发布管理员ID',
  `publisher_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发布者名称（管理员用户名或昵称）',
  `target_residence_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标小区/住户范围（为空则全体可见）',
  `status` tinyint NULL DEFAULT 1 COMMENT '公告状态：0-草稿，1-已发布，2-已撤回',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公告备注',
  PRIMARY KEY (`notice_id`) USING BTREE,
  INDEX `fk_notice_admin`(`publisher_id` ASC) USING BTREE,
  CONSTRAINT `fk_notice_admin` FOREIGN KEY (`publisher_id`) REFERENCES `admin` (`admin_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for notice_attachment
-- ----------------------------
DROP TABLE IF EXISTS `notice_attachment`;
CREATE TABLE `notice_attachment`  (
  `attachment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件唯一ID',
  `notice_id` bigint NOT NULL COMMENT '所属公告ID',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '附件文件URL（图片或其他文件）',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'image' COMMENT '附件类型：image/video/file等',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`attachment_id`) USING BTREE,
  INDEX `fk_attachment_notice`(`notice_id` ASC) USING BTREE,
  CONSTRAINT `fk_attachment_notice` FOREIGN KEY (`notice_id`) REFERENCES `notice` (`notice_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '公告附件表（支持多张图片或文件）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for notice_read
-- ----------------------------
DROP TABLE IF EXISTS `notice_read`;
CREATE TABLE `notice_read`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `residence_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '住户唯一ID（每户）',
  `read_status` tinyint NULL DEFAULT 0 COMMENT '阅读状态：0-未读，1-已读',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_notice_residence`(`notice_id` ASC, `residence_id` ASC) USING BTREE,
  CONSTRAINT `fk_notice_read_notice` FOREIGN KEY (`notice_id`) REFERENCES `notice` (`notice_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 151 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '公告阅读状态表（按户为单位）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pay_order
-- ----------------------------
DROP TABLE IF EXISTS `pay_order`;
CREATE TABLE `pay_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商户订单号（业务唯一，如 PO202508180001）',
  `residence_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '住宅ID（楼栋-单元-房号，例如 1-2-302）',
  `bill_id` bigint NOT NULL COMMENT '对应的账单ID（fee_bill.id）',
  `subject` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单标题（支付宝页面展示）',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '订单金额（元）',
  `pay_channel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ALIPAY' COMMENT '支付渠道：ALIPAY',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '支付状态：0-已关闭，1-待支付，2-支付成功，3-支付失败，4-已退款',
  `trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付宝交易号（支付成功后回填）',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付成功时间',
  `notify_time` datetime NULL DEFAULT NULL COMMENT '最近一次异步通知时间',
  `client_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '下单客户端IP',
  `extra` json NULL COMMENT '扩展信息（如网关返回字段快照）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_bill_status`(`bill_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付订单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for repair_assignment
-- ----------------------------
DROP TABLE IF EXISTS `repair_assignment`;
CREATE TABLE `repair_assignment`  (
  `assignment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '指派记录ID',
  `order_id` bigint NOT NULL COMMENT '报修单ID',
  `worker_id` bigint NOT NULL COMMENT '维修人员ID',
  `assigned_by` bigint NOT NULL COMMENT '指派人ID（物业管理员）',
  `assigned_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  `visiting_time` datetime NULL DEFAULT NULL COMMENT '指派上门时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '已指派' COMMENT '指派状态（已指派/已接受/已拒绝/已完成）',
  PRIMARY KEY (`assignment_id`) USING BTREE,
  INDEX `order_id`(`order_id` ASC) USING BTREE,
  INDEX `worker_id`(`worker_id` ASC) USING BTREE,
  CONSTRAINT `repair_assignment_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `repair_order` (`order_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for repair_category
-- ----------------------------
DROP TABLE IF EXISTS `repair_category`;
CREATE TABLE `repair_category`  (
  `category_id` bigint NOT NULL AUTO_INCREMENT COMMENT '类别ID',
  `category_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类别名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '类别描述',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`category_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for repair_evaluation
-- ----------------------------
DROP TABLE IF EXISTS `repair_evaluation`;
CREATE TABLE `repair_evaluation`  (
  `eval_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '维修评价唯一ID（主键）',
  `assignment_id` bigint UNSIGNED NOT NULL COMMENT '关联维修表的维修ID（用于业务关联，无外键约束）',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '评价人ID（关联报修表的报修用户ID，用于业务关联，无外键约束）',
  `eval_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价提交时间',
  `score` int NOT NULL COMMENT '维修服务评分（1-5分）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '评价详细内容',
  `is_anonymous` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '否' COMMENT '是否匿名评价（是/否）',
  `reply_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '维修方/管理员的回复内容',
  `reply_time` datetime NULL DEFAULT NULL COMMENT '回复提交时间',
  PRIMARY KEY (`eval_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '维修服务评价表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for repair_file
-- ----------------------------
DROP TABLE IF EXISTS `repair_file`;
CREATE TABLE `repair_file`  (
  `file_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `order_id` bigint NOT NULL COMMENT '所属报修单ID',
  `file_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件路径或URL',
  `file_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件类型（image/video/other）',
  `uploaded_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `user_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`file_id`) USING BTREE,
  INDEX `order_id`(`order_id` ASC) USING BTREE,
  CONSTRAINT `repair_file_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `repair_order` (`order_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for repair_order
-- ----------------------------
DROP TABLE IF EXISTS `repair_order`;
CREATE TABLE `repair_order`  (
  `order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '报修单ID',
  `user_id` bigint NOT NULL COMMENT '报修人ID（住户）',
  `category_id` bigint NOT NULL COMMENT '报修类别ID',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '报修描述',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '待处理' COMMENT '状态（待处理/已指派/维修中/已完成/已取消）',
  `priority` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '普通' COMMENT '优先级（普通/紧急）',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报修时间',
  `appointment_time` datetime NULL DEFAULT NULL COMMENT '预约上门时间',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `assigned_worker` bigint NULL DEFAULT NULL COMMENT '维修人员ID（如果已指派）',
  `finished_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `building_no` int NULL DEFAULT NULL,
  `unit_no` int NULL DEFAULT NULL,
  `room_no` int NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`order_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for repair_worker
-- ----------------------------
DROP TABLE IF EXISTS `repair_worker`;
CREATE TABLE `repair_worker`  (
  `worker_id` bigint NOT NULL AUTO_INCREMENT COMMENT '维修人员ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系电话',
  `specialty` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '擅长领域（水电/空调/电梯等）',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入职时间',
  PRIMARY KEY (`worker_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1005 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for reply_count_cache
-- ----------------------------
DROP TABLE IF EXISTS `reply_count_cache`;
CREATE TABLE `reply_count_cache`  (
  `comment_id` bigint NOT NULL,
  `reply_count` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`comment_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for travel_pass_record
-- ----------------------------
DROP TABLE IF EXISTS `travel_pass_record`;
CREATE TABLE `travel_pass_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `has_vehicle` tinyint(1) NULL DEFAULT 0 COMMENT '是否有车辆',
  `plate_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '车牌号',
  `paid` tinyint(1) NULL DEFAULT 0 COMMENT '是否支付通行费（住户费）',
  `issue_time` datetime NOT NULL COMMENT '二维码生成时间',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '二维码过期时间',
  `entry_time` datetime NULL DEFAULT NULL COMMENT '入场时间',
  `exit_time` datetime NULL DEFAULT NULL COMMENT '出场时间',
  `fee` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '停车费',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'ISSUED' COMMENT '状态：ISSUED已生成，ENTERED已入场，EXITED已出场',
  `employee_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '出行码申请与使用记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `user_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户唯一ID（主键）',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名（登录用，唯一）',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（BCrypt哈希值）',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称（展示用）',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像存储路径（如OSS地址）',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号（登录/通知用）',
  `id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号（实名认证用）',
  `vehicle_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '绑定车辆信息（如\"沪A·12345\"）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态：0-未激活，1-正常，2-禁言，3-封禁',
  `register_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `last_login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后登录时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注（管理员添加）',
  `building_no` int NULL DEFAULT NULL COMMENT '楼栋号',
  `unit_no` int NULL DEFAULT NULL COMMENT '单元号',
  `room_no` int NULL DEFAULT NULL COMMENT '房间号',
  `area` int NULL DEFAULT NULL COMMENT '房产面积',
  `residence_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS (concat_ws(_utf8mb4'-',`building_no`,`unit_no`,`room_no`)) STORED COMMENT '住宅ID = 楼栋号-单元号-房间号' NULL,
  PRIMARY KEY (`user_id` DESC) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_residence_id`(`residence_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 220 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
