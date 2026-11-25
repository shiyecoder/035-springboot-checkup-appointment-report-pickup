-- =============================================================
-- 体检预约与报告领取管理系统  数据库结构
-- MySQL 8 / utf8mb4
-- =============================================================
DROP DATABASE IF EXISTS `__DB__`;
CREATE DATABASE `__DB__` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `__DB__`;

-- ---------- 组织 ----------
CREATE TABLE `sys_org` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `org_name` VARCHAR(100) NOT NULL COMMENT '组织/企业名称',
  `org_type` VARCHAR(20) NOT NULL DEFAULT 'ENTERPRISE' COMMENT '类型 ENTERPRISE/HOSPITAL',
  `contact` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_name` (`org_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织表';

-- ---------- 用户 ----------
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
  `real_name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `gender` CHAR(1) DEFAULT 'M' COMMENT '性别 M/F',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `id_card_masked` VARCHAR(32) DEFAULT NULL COMMENT '身份证摘要（脱敏）',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像相对路径',
  `role_code` VARCHAR(30) NOT NULL COMMENT '角色编码',
  `org_id` BIGINT DEFAULT NULL COMMENT '所属组织',
  `dept` VARCHAR(50) DEFAULT NULL COMMENT '所属科室',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 正常 0 停用',
  `last_login_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------- 角色 ----------
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(30) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `data_scope` VARCHAR(20) NOT NULL DEFAULT 'SELF' COMMENT '数据范围 SELF/DEPT/ORG/ALL',
  `remark` VARCHAR(200) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ---------- 菜单 ----------
CREATE TABLE `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单',
  `menu_name` VARCHAR(50) NOT NULL,
  `path` VARCHAR(100) DEFAULT NULL COMMENT '前端路由',
  `icon` VARCHAR(50) DEFAULT NULL,
  `perm` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  `menu_type` CHAR(1) NOT NULL DEFAULT 'M' COMMENT 'M 目录 C 菜单 B 按钮',
  `sort` INT NOT NULL DEFAULT 0,
  `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '1 显示 0 隐藏',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

CREATE TABLE `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联';

-- ---------- 字典 ----------
CREATE TABLE `sys_dict` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `dict_type` VARCHAR(50) NOT NULL COMMENT '字典类型',
  `dict_label` VARCHAR(50) NOT NULL,
  `dict_value` VARCHAR(50) NOT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典';

-- ---------- 站内消息 ----------
CREATE TABLE `sys_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '接收人',
  `title` VARCHAR(100) NOT NULL,
  `content` VARCHAR(500) DEFAULT NULL,
  `biz_no` VARCHAR(40) DEFAULT NULL COMMENT '业务编号',
  `biz_type` VARCHAR(30) DEFAULT NULL COMMENT 'APPOINTMENT/REPORT/PAYMENT',
  `read_flag` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`,`read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息';

-- ---------- 操作日志 ----------
CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `username` VARCHAR(50) DEFAULT NULL,
  `module` VARCHAR(50) DEFAULT NULL,
  `action` VARCHAR(100) DEFAULT NULL,
  `biz_no` VARCHAR(40) DEFAULT NULL,
  `detail` VARCHAR(500) DEFAULT NULL,
  `ip` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志（含报告下载审计）';

-- ---------- 检查项目 ----------
CREATE TABLE `exam_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `item_no` VARCHAR(50) NOT NULL COMMENT '项目编号',
  `item_name` VARCHAR(100) NOT NULL COMMENT '项目名称',
  `dept` VARCHAR(50) NOT NULL COMMENT '执行科室',
  `pre_requirement` VARCHAR(200) DEFAULT NULL COMMENT '前置要求（空腹等）',
  `gender_limit` VARCHAR(10) NOT NULL DEFAULT 'ALL' COMMENT 'ALL/M/F',
  `age_min` INT NOT NULL DEFAULT 0,
  `age_max` INT NOT NULL DEFAULT 120,
  `ref_price` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '参考价',
  `duration_min` INT NOT NULL DEFAULT 10 COMMENT '执行时长(分钟)',
  `notice` VARCHAR(300) DEFAULT NULL COMMENT '注意事项',
  `device_code` VARCHAR(30) DEFAULT NULL COMMENT '设备编码（停用判断用）',
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_no` (`item_no`),
  KEY `idx_dept` (`dept`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查项目';

-- ---------- 体检套餐 ----------
CREATE TABLE `exam_package` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `package_no` VARCHAR(50) NOT NULL COMMENT '套餐编号',
  `package_name` VARCHAR(100) NOT NULL,
  `crowd` VARCHAR(50) NOT NULL COMMENT '适用人群',
  `dept_summary` VARCHAR(100) DEFAULT NULL COMMENT '执行科室汇总',
  `original_price` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `discount_price` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `duration_min` INT NOT NULL DEFAULT 60 COMMENT '预计时长',
  `item_count` INT NOT NULL DEFAULT 0 COMMENT '项目数（后端汇总计算）',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面相对路径',
  `booking_notice` VARCHAR(500) DEFAULT NULL COMMENT '预约说明',
  `shelf_start` DATE DEFAULT NULL,
  `shelf_end` DATE DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ON_SHELF' COMMENT 'ON_SHELF/DRAFT/OFF_SHELF',
  `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_package_no` (`package_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检套餐';

CREATE TABLE `exam_package_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `package_id` BIGINT NOT NULL,
  `item_id` BIGINT NOT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pkg_item` (`package_id`,`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐项目关联';

-- ---------- 时段容量 ----------
CREATE TABLE `appointment_slot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `slot_date` DATE NOT NULL COMMENT '日期',
  `period` VARCHAR(20) NOT NULL COMMENT '时段 AM_0730/AM_0900/AM_1030',
  `time_range` VARCHAR(30) NOT NULL COMMENT '时间范围',
  `personal_quota` INT NOT NULL DEFAULT 0 COMMENT '个人名额',
  `group_quota` INT NOT NULL DEFAULT 0 COMMENT '团体名额',
  `personal_booked` INT NOT NULL DEFAULT 0 COMMENT '个人已约',
  `group_booked` INT NOT NULL DEFAULT 0 COMMENT '团体已约',
  `dept_capacity` INT NOT NULL DEFAULT 0 COMMENT '科室最大接待量',
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_period` (`slot_date`,`period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时段容量';

-- ---------- 团检批次 ----------
CREATE TABLE `group_batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `batch_no` VARCHAR(40) NOT NULL,
  `org_id` BIGINT NOT NULL COMMENT '企业',
  `org_name` VARCHAR(100) NOT NULL,
  `package_id` BIGINT NOT NULL,
  `start_date` DATE NOT NULL,
  `end_date` DATE NOT NULL,
  `quota` INT NOT NULL DEFAULT 0,
  `joined_count` INT NOT NULL DEFAULT 0,
  `checked_count` INT NOT NULL DEFAULT 0,
  `contact_name` VARCHAR(50) DEFAULT NULL,
  `contact_phone` VARCHAR(20) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CHECKING/DONE',
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_no` (`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团检批次';

CREATE TABLE `group_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `batch_id` BIGINT NOT NULL,
  `member_name` VARCHAR(50) NOT NULL,
  `gender` CHAR(1) DEFAULT 'M',
  `age` INT DEFAULT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `id_card` VARCHAR(32) DEFAULT NULL,
  `package_id` BIGINT DEFAULT NULL,
  `import_status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED',
  `error_msg` VARCHAR(200) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团检人员名单';

-- ---------- 体检预约（核心过程单据） ----------
CREATE TABLE `exam_appointment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `app_no` VARCHAR(40) NOT NULL COMMENT '预约编号',
  `package_id` BIGINT NOT NULL,
  `package_name` VARCHAR(100) DEFAULT NULL,
  `user_id` BIGINT DEFAULT NULL COMMENT '预约用户',
  `batch_id` BIGINT DEFAULT NULL COMMENT '团检批次',
  `booking_date` DATE NOT NULL,
  `slot_id` BIGINT NOT NULL,
  `time_range` VARCHAR(30) DEFAULT NULL,
  `contact_name` VARCHAR(50) NOT NULL,
  `contact_phone` VARCHAR(20) NOT NULL,
  `id_card_digest` VARCHAR(40) DEFAULT NULL COMMENT '身份证摘要，不存明文',
  `special_need` VARCHAR(300) DEFAULT NULL,
  `amount_due` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '应收',
  `amount_paid` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '实收',
  `pay_status` VARCHAR(20) NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PARTIAL/PAID/REFUNDED',
  `pay_method` VARCHAR(20) DEFAULT NULL COMMENT 'CASH/WECHAT/ALIPAY/CARD/GROUP',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING_CONFIRM' COMMENT '见状态机',
  `checkin_time` DATETIME DEFAULT NULL,
  `report_status` VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/PENDING/READY/PICKED',
  `pickup_method` VARCHAR(20) DEFAULT NULL COMMENT 'SELF/ONLINE/ENTRUST',
  `crowd_score_id` BIGINT DEFAULT NULL COMMENT '关联拥挤度计算',
  `owner_id` BIGINT DEFAULT NULL COMMENT '当前责任人（前台）',
  `remark` VARCHAR(500) DEFAULT NULL,
  `version` INT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_no` (`app_no`),
  KEY `idx_status` (`status`),
  KEY `idx_date` (`booking_date`),
  KEY `idx_user` (`user_id`),
  KEY `idx_slot` (`slot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检预约与报告主单';

-- ---------- 线下缴费 ----------
CREATE TABLE `payment_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `app_id` BIGINT NOT NULL,
  `receivable` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '应收',
  `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '减免',
  `received` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '实收',
  `pay_method` VARCHAR(20) NOT NULL COMMENT 'CASH/WECHAT/ALIPAY/CARD/GROUP',
  `voucher_image` VARCHAR(255) DEFAULT NULL COMMENT '凭证图片相对路径',
  `refund_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `refund_reason` VARCHAR(200) DEFAULT NULL,
  `operator_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_app` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线下缴费登记';

-- ---------- 项目执行进度 ----------
CREATE TABLE `exam_progress` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `app_id` BIGINT NOT NULL,
  `item_id` BIGINT NOT NULL,
  `item_name` VARCHAR(100) NOT NULL,
  `dept` VARCHAR(50) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING/CHECKING/DONE/GIVEN_UP/SUPPLEMENT',
  `check_time` DATETIME DEFAULT NULL,
  `operator_id` BIGINT DEFAULT NULL,
  `remark` VARCHAR(200) DEFAULT NULL,
  `supplement_date` DATE DEFAULT NULL COMMENT '补检预约日期',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_app` (`app_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目执行进度（漏检/补检）';

-- ---------- 体检报告（多版本） ----------
CREATE TABLE `exam_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `app_id` BIGINT NOT NULL,
  `version_no` INT NOT NULL DEFAULT 1 COMMENT '版本号',
  `file_path` VARCHAR(255) NOT NULL COMMENT '存储相对路径（不对外暴露）',
  `file_name` VARCHAR(120) DEFAULT NULL COMMENT '原始文件名',
  `issue_date` DATE DEFAULT NULL COMMENT '签发日期',
  `doctor_name` VARCHAR(50) DEFAULT NULL COMMENT '总检医师',
  `is_latest` TINYINT NOT NULL DEFAULT 1 COMMENT '1 最新有效版',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ARCHIVED' COMMENT 'ARCHIVED/VOID',
  `uploader_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_app` (`app_id`,`is_latest`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检报告归档（多版本）';

-- ---------- 报告领取 ----------
CREATE TABLE `report_pickup` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `app_id` BIGINT NOT NULL,
  `report_id` BIGINT NOT NULL,
  `pickup_type` VARCHAR(20) NOT NULL COMMENT 'SELF/ONLINE/ENTRUST',
  `picker_name` VARCHAR(50) DEFAULT NULL COMMENT '领取人',
  `picker_id_card` VARCHAR(32) DEFAULT NULL COMMENT '领取人证件摘要',
  `picker_phone` VARCHAR(20) DEFAULT NULL,
  `relation` VARCHAR(30) DEFAULT NULL COMMENT '与体检人关系',
  `auth_file` VARCHAR(255) DEFAULT NULL COMMENT '授权书图片',
  `pickup_time` DATETIME DEFAULT NULL,
  `operator_id` BIGINT DEFAULT NULL,
  `remark` VARCHAR(200) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_app` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告领取/代领登记';

-- ---------- 创新点：拥挤度规则 ----------
CREATE TABLE `crowd_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `version_no` INT NOT NULL COMMENT '规则版本',
  `weights_json` VARCHAR(500) NOT NULL COMMENT '六项权重，合计 100',
  `thresholds_json` VARCHAR(200) NOT NULL COMMENT '阈值配置',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'ACTIVE/HISTORY/DRAFT',
  `effective_time` DATETIME DEFAULT NULL,
  `create_by` BIGINT DEFAULT NULL,
  `remark` VARCHAR(200) DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version` (`version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拥挤度规则版本';

CREATE TABLE `crowd_score` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `score_no` VARCHAR(40) NOT NULL COMMENT '计算编号',
  `app_id` BIGINT DEFAULT NULL,
  `slot_id` BIGINT NOT NULL,
  `rule_id` BIGINT NOT NULL,
  `rule_version` INT NOT NULL,
  `input_snapshot` TEXT COMMENT '输入快照 JSON',
  `detail_json` TEXT COMMENT '各项得分明细 JSON',
  `total_score` DECIMAL(6,2) NOT NULL COMMENT '综合拥挤度',
  `level` VARCHAR(20) NOT NULL COMMENT 'RECOMMEND/NORMAL/NOT_RECOMMEND',
  `main_factors` VARCHAR(300) DEFAULT NULL,
  `unmet_conditions` VARCHAR(300) DEFAULT NULL,
  `improve_tip` VARCHAR(300) DEFAULT NULL,
  `adjusted_score` DECIMAL(6,2) DEFAULT NULL COMMENT '人工调整后分数',
  `adjust_reason` VARCHAR(200) DEFAULT NULL,
  `adjust_by` BIGINT DEFAULT NULL,
  `adjust_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_score_no` (`score_no`),
  KEY `idx_app` (`app_id`),
  KEY `idx_slot` (`slot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拥挤度计算留痕';
