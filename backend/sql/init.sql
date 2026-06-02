-- LingNow System Initialization SQL
-- ---------------------------------------------------------

-- 0. Database Creation
-- ---------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `lingnow_erp` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `lingnow_erp`;

-- ---------------------------------------------------------
-- 1. Core System Tables (Foundational Infrastructure)
-- ---------------------------------------------------------

-- System User
CREATE TABLE IF NOT EXISTS `sys_user`
(
    `user_id`         bigint(20)   NOT NULL COMMENT '主键ID',
    `username`        varchar(64)  NOT NULL COMMENT '用户名',
    `password`        varchar(128) NOT NULL COMMENT '密码',
    `nickname`        varchar(64)  DEFAULT NULL COMMENT '昵称',
    `email`           varchar(64)  DEFAULT NULL COMMENT '邮箱',
    `phone`           varchar(32)  DEFAULT NULL COMMENT '手机号',
    `avatar`          varchar(255) DEFAULT NULL COMMENT '头像',
    `gender`          tinyint(1)   DEFAULT '2' COMMENT '性别 (0-女 1-男 2-其他)',
    `birthday`        date         DEFAULT NULL COMMENT '生日',
    `region`          varchar(128) DEFAULT NULL COMMENT '所在地区',
    `status`          tinyint(1)   DEFAULT '1' COMMENT '状态 (1-正常 0-禁用)',
    `dept_id`         varchar(64)  DEFAULT NULL COMMENT '部门ID',
    `create_time`     datetime     DEFAULT NULL,
    `update_time`     datetime     DEFAULT NULL,
    `create_by`       varchar(64)  DEFAULT NULL,
    `update_by`       varchar(64)  DEFAULT NULL,
    `del_flag`        tinyint(1)   DEFAULT '0' COMMENT '删除标记 (0-未删除 1-已删除)',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `idx_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户实体';

-- App User
CREATE TABLE IF NOT EXISTS `app_user`
(
    `user_id`     bigint(20)   NOT NULL COMMENT '主键ID',
    `username`    varchar(64)  NOT NULL COMMENT '用户名',
    `password`    varchar(128) NOT NULL COMMENT '密码',
    `phone`       varchar(32)  NOT NULL COMMENT '手机号',
    `nickname`    varchar(64)  DEFAULT NULL COMMENT '昵称',
    `avatar`      varchar(500) DEFAULT NULL COMMENT '头像',
    `status`      tinyint(1)   DEFAULT '1' COMMENT '状态 (1-正常 0-禁用)',
    `create_time` datetime     DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    `create_by`   varchar(64)  DEFAULT NULL,
    `update_by`   varchar(64)  DEFAULT NULL,
    `del_flag`    tinyint(1)   DEFAULT '0' COMMENT '删除标记 (0-未删除 1-已删除)',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `idx_app_user_username` (`username`),
    UNIQUE KEY `idx_app_user_phone` (`phone`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='App用户基础表';

-- App User Info
CREATE TABLE IF NOT EXISTS `app_user_info`
(
    `user_id`     bigint(20) NOT NULL COMMENT '用户ID',
    `gender`      tinyint(1)   DEFAULT '2' COMMENT '性别 (0-女 1-男 2-其他)',
    `birthday`    date         DEFAULT NULL COMMENT '生日',
    `region`      varchar(128) DEFAULT NULL COMMENT '所在地区',
    `signature`   varchar(255) DEFAULT NULL COMMENT '个性签名',
    `tags`        varchar(500) DEFAULT NULL COMMENT '标签JSON',
    `create_time` datetime     DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    `create_by`   varchar(64)  DEFAULT NULL,
    `update_by`   varchar(64)  DEFAULT NULL,
    `del_flag`    tinyint(1)   DEFAULT '0' COMMENT '删除标记 (0-未删除 1-已删除)',
    PRIMARY KEY (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='App用户扩展信息表';

-- App Social User
CREATE TABLE IF NOT EXISTS `app_social_user`
(
    `id`          bigint(20)  NOT NULL COMMENT '主键ID',
    `user_id`     bigint(20)  NOT NULL COMMENT '用户ID',
    `provider`    varchar(32) NOT NULL COMMENT '第三方平台',
    `open_id`     varchar(128) DEFAULT NULL COMMENT '平台OpenID',
    `union_id`    varchar(128) DEFAULT NULL COMMENT '平台UnionID',
    `nickname`    varchar(64)  DEFAULT NULL COMMENT '平台昵称',
    `avatar`      varchar(500) DEFAULT NULL COMMENT '平台头像',
    `create_time` datetime     DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    `create_by`   varchar(64)  DEFAULT NULL,
    `update_by`   varchar(64)  DEFAULT NULL,
    `del_flag`    tinyint(1)   DEFAULT '0' COMMENT '删除标记 (0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_app_social_user_id` (`user_id`),
    KEY `idx_app_social_open` (`provider`, `open_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='App社交账号绑定表';

-- System Role
CREATE TABLE IF NOT EXISTS `sys_role`
(
    `role_id`     bigint(20)  NOT NULL COMMENT '主键ID',
    `role_name`   varchar(64) NOT NULL COMMENT '角色名称',
    `role_key`    varchar(64) NOT NULL COMMENT '角色权限字符串',
    `sort_order`  int(11)      DEFAULT '0' COMMENT '显示顺序',
    `status`      tinyint(1)   DEFAULT '1' COMMENT '角色状态 (1正常 0停用)',
    `data_scope`  tinyint(1)   DEFAULT '1' COMMENT '数据范围',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime     DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    `create_by`   varchar(64)  DEFAULT NULL,
    `update_by`   varchar(64)  DEFAULT NULL,
    `del_flag`    tinyint(1)   DEFAULT '0',
    PRIMARY KEY (`role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统角色实体';

-- System Menu
CREATE TABLE IF NOT EXISTS `sys_menu`
(
    `menu_id`     bigint(20)  NOT NULL COMMENT '菜单ID',
    `parent_id`   bigint(20)   DEFAULT '0' COMMENT '父菜单ID',
    `menu_name`   varchar(64) NOT NULL COMMENT '菜单名称',
    `menu_type`   tinyint(1)   DEFAULT '0' COMMENT '菜单类型：0目录 1菜单 2按钮',
    `icon`        varchar(128) DEFAULT NULL COMMENT '菜单图标',
    `path`        varchar(255) DEFAULT NULL COMMENT '路由地址',
    `component`   varchar(255) DEFAULT NULL COMMENT '组件路径',
    `permission`  varchar(128) DEFAULT NULL COMMENT '权限标识',
    `sort_order`  int(11)      DEFAULT '0' COMMENT '显示顺序',
    `visible`     tinyint(1)   DEFAULT '1' COMMENT '是否可见：0隐藏 1显示',
    `status`      tinyint(1)   DEFAULT '1' COMMENT '状态：0禁用 1启用',
    `is_cache`    char(1)      DEFAULT 'N' COMMENT '是否缓存：Y缓存 N不缓存',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime     DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    `create_by`   varchar(64)  DEFAULT NULL,
    `update_by`   varchar(64)  DEFAULT NULL,
    `del_flag`    tinyint(1)   DEFAULT '0',
    PRIMARY KEY (`menu_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统菜单实体';

-- System File Config
CREATE TABLE IF NOT EXISTS `sys_file_config`
(
    `id`          bigint(20)  NOT NULL COMMENT '主键ID',
    `platform`    varchar(32) NOT NULL COMMENT '平台: LOCAL, MINIO, ALIYUN, TENCENT, QINIU, REST',
    `config_json` text COMMENT '配置信息(JSON)',
    `is_active`   tinyint(1)   DEFAULT '0' COMMENT '是否启用: 0-否, 1-是',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime     DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    `create_by`   varchar(64)  DEFAULT NULL,
    `update_by`   varchar(64)  DEFAULT NULL,
    `del_flag`    tinyint(1)   DEFAULT '0' COMMENT '删除标记 (0-未删除 1-已删除)',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='文件存储配置实体';

-- System File
CREATE TABLE IF NOT EXISTS `sys_file`
(
    `id`            bigint(20)   NOT NULL COMMENT '主键ID',
    `file_name`     varchar(255) DEFAULT NULL COMMENT '原始文件名',
    `file_path`     varchar(500) DEFAULT NULL COMMENT '文件存储路径',
    `file_url`      varchar(500) DEFAULT NULL COMMENT '文件访问URL',
    `file_size`     bigint(20)   DEFAULT NULL COMMENT '文件大小',
    `file_suffix`   varchar(32)  DEFAULT NULL COMMENT '文件后缀',
    `storage_type`  varchar(32)  DEFAULT NULL COMMENT '存储类型',
    `business_id`   bigint(20)   DEFAULT NULL COMMENT '业务ID',
    `business_type` varchar(64)  DEFAULT NULL COMMENT '业务类型',
    `create_time`   datetime     DEFAULT NULL,
    `update_time`   datetime     DEFAULT NULL,
    `create_by`     varchar(64)  DEFAULT NULL,
    `update_by`     varchar(64)  DEFAULT NULL,
    `del_flag`      tinyint(1)   DEFAULT '0',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='文件信息实体';

-- System Dictionary Type
CREATE TABLE IF NOT EXISTS `sys_dict_type`
(
    `dict_id`     bigint(20) NOT NULL COMMENT '字典主键',
    `dict_name`   varchar(100) DEFAULT '' COMMENT '字典名称',
    `dict_type`   varchar(100) DEFAULT '' COMMENT '字典类型',
    `status`      tinyint(1)   DEFAULT '1' COMMENT '状态（1正常 0停用）',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime     DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    `create_by`   varchar(64)  DEFAULT NULL,
    `update_by`   varchar(64)  DEFAULT NULL,
    `del_flag`    tinyint(1)   DEFAULT '0',
    PRIMARY KEY (`dict_id`),
    UNIQUE KEY `idx_dict_type` (`dict_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='字典类型表';

-- System Dictionary Data
CREATE TABLE IF NOT EXISTS `sys_dict_data`
(
    `dict_code`   bigint(20) NOT NULL COMMENT '字典编码',
    `dict_sort`   int(11)      DEFAULT '0' COMMENT '字典排序',
    `dict_label`  varchar(100) DEFAULT '' COMMENT '字典标签',
    `dict_value`  varchar(100) DEFAULT '' COMMENT '字典键值',
    `dict_type`   varchar(100) DEFAULT '' COMMENT '字典类型',
    `css_class`   varchar(100) DEFAULT NULL COMMENT '样式属性',
    `list_class`  varchar(100) DEFAULT NULL COMMENT '表格回显样式',
    `is_default`  char(1)      DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
    `status`      tinyint(1)   DEFAULT '1' COMMENT '状态（1正常 0停用）',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime     DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    `create_by`   varchar(64)  DEFAULT NULL,
    `update_by`   varchar(64)  DEFAULT NULL,
    `del_flag`    tinyint(1)   DEFAULT '0',
    PRIMARY KEY (`dict_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='字典数据表';

-- System Configuration
CREATE TABLE IF NOT EXISTS `sys_config`
(
    `config_id`    bigint(20) NOT NULL COMMENT '参数主键',
    `config_name`  varchar(100) DEFAULT '' COMMENT '参数名称',
    `config_key`   varchar(100) DEFAULT '' COMMENT '参数键名',
    `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
    `config_type`  char(1)      DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
    `remark`       varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time`  datetime     DEFAULT NULL,
    `update_time`  datetime     DEFAULT NULL,
    `create_by`    varchar(64)  DEFAULT NULL,
    `update_by`    varchar(64)  DEFAULT NULL,
    `del_flag`     tinyint(1)   DEFAULT '0',
    PRIMARY KEY (`config_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='参数配置表';

-- System Department
CREATE TABLE IF NOT EXISTS `sys_dept`
(
    `dept_id`       bigint(20) NOT NULL COMMENT '部门ID',
    `parent_id`     varchar(64)  DEFAULT '0' COMMENT '父部门ID',
    `ancestors`     varchar(200) DEFAULT '' COMMENT '祖级列表',
    `dept_name`     varchar(30)  DEFAULT '' COMMENT '部门名称',
    `order_num`     int(11)      DEFAULT '0' COMMENT '显示顺序',
    `leader`        varchar(20)  DEFAULT NULL COMMENT '负责人',
    `phone`         varchar(11)  DEFAULT NULL COMMENT '联系电话',
    `email`         varchar(50)  DEFAULT NULL COMMENT '邮箱',
    `category_code` varchar(64)  DEFAULT NULL COMMENT '类别编码',
    `region`        varchar(128) DEFAULT NULL COMMENT '地区',
    `status`        tinyint(1)   DEFAULT '1' COMMENT '部门状态（1正常 0停用）',
    `create_time`   datetime     DEFAULT NULL,
    `update_time`   datetime     DEFAULT NULL,
    `create_by`     varchar(64)  DEFAULT NULL,
    `update_by`     varchar(64)  DEFAULT NULL,
    `del_flag`      tinyint(1)   DEFAULT '0',
    PRIMARY KEY (`dept_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='部门表';

-- Role Menu Join
CREATE TABLE IF NOT EXISTS `sys_role_menu`
(
    `role_id` bigint(20) NOT NULL,
    `menu_id` bigint(20) NOT NULL,
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色和菜单关联';

-- User Role Join
CREATE TABLE IF NOT EXISTS `sys_user_role`
(
    `user_id` bigint(20) NOT NULL,
    `role_id` bigint(20) NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户和角色关联';

-- User Post Join
CREATE TABLE IF NOT EXISTS `sys_user_post`
(
    `user_id` bigint(20) NOT NULL,
    `post_id` bigint(20) NOT NULL,
    PRIMARY KEY (`user_id`, `post_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户和岗位关联';

-- Login Log
CREATE TABLE IF NOT EXISTS `sys_login_log`
(
    `info_id`        bigint(20) NOT NULL AUTO_INCREMENT COMMENT '访问ID',
    `user_name`      varchar(64)  DEFAULT NULL COMMENT '用户账号',
    `ipaddr`         varchar(128) DEFAULT NULL COMMENT '登录IP地址',
    `login_location` varchar(255) DEFAULT NULL COMMENT '登录地点',
    `browser`        varchar(64)  DEFAULT NULL COMMENT '浏览器类型',
    `os`             varchar(64)  DEFAULT NULL COMMENT '操作系统',
    `status`         tinyint(1)   DEFAULT '1' COMMENT '登录状态（1成功 0失败）',
    `msg`            varchar(1000) DEFAULT NULL COMMENT '提示消息',
    `login_time`     datetime     DEFAULT NULL COMMENT '访问时间',
    `create_time`    datetime     DEFAULT NULL,
    `update_time`    datetime     DEFAULT NULL,
    `create_by`      varchar(64)  DEFAULT NULL,
    `update_by`      varchar(64)  DEFAULT NULL,
    `del_flag`       tinyint(1)   DEFAULT '0',
    PRIMARY KEY (`info_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统访问记录';

-- System Operational Log
CREATE TABLE IF NOT EXISTS `sys_oper_log`
(
    `oper_id`        bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志主键',
    `title`          varchar(64)   DEFAULT NULL COMMENT '模块标题',
    `business_type`  int(11)       DEFAULT '0' COMMENT '业务类型',
    `method`         varchar(128)  DEFAULT NULL COMMENT '方法名称',
    `request_method` varchar(20)   DEFAULT NULL COMMENT '请求方式',
    `operator_type`  int(11)       DEFAULT '0' COMMENT '操作类别',
    `oper_name`      varchar(64)   DEFAULT NULL COMMENT '操作人员',
    `dept_name`      varchar(64)   DEFAULT NULL COMMENT '部门名称',
    `oper_url`       varchar(255)  DEFAULT NULL COMMENT '请求URL',
    `oper_ip`        varchar(128)  DEFAULT NULL COMMENT '主机地址',
    `oper_location`  varchar(255)  DEFAULT NULL COMMENT '操作地点',
    `oper_param`     varchar(2000) DEFAULT NULL COMMENT '请求参数',
    `json_result`    varchar(2000) DEFAULT NULL COMMENT '返回参数',
    `status`         tinyint(1)    DEFAULT '1' COMMENT '操作状态（1正常 0异常）',
    `error_msg`      varchar(2000) DEFAULT NULL COMMENT '错误消息',
    `oper_time`      datetime      DEFAULT NULL COMMENT '操作时间',
    `cost_time`      bigint(20)    DEFAULT '0' COMMENT '消耗时间',
    `create_time`    datetime      DEFAULT NULL,
    `update_time`    datetime      DEFAULT NULL,
    `create_by`      varchar(64)   DEFAULT NULL,
    `update_by`      varchar(64)   DEFAULT NULL,
    `del_flag`       tinyint(1)    DEFAULT '0',
    PRIMARY KEY (`oper_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='操作日志记录';

-- System Error Log
CREATE TABLE IF NOT EXISTS `sys_error_log`
(
    `id`             bigint(20) NOT NULL COMMENT '主键ID',
    `trace_id`       varchar(64)    DEFAULT NULL COMMENT '追踪ID',
    `user_id`        bigint(20)     DEFAULT NULL COMMENT '用户ID',
    `user_name`      varchar(64)    DEFAULT NULL COMMENT '用户名称',
    `request_method` varchar(20)    DEFAULT NULL COMMENT '请求方式',
    `request_url`    varchar(255)   DEFAULT NULL COMMENT '请求URL',
    `request_params` varchar(2000)  DEFAULT NULL COMMENT '请求参数',
    `ip`             varchar(128)   DEFAULT NULL COMMENT 'IP地址',
    `error_msg`      varchar(2000)  DEFAULT NULL COMMENT '错误信息',
    `error_stack`    longtext COMMENT '堆栈信息',
    `create_time`    datetime      DEFAULT NULL,
    `update_time`    datetime      DEFAULT NULL,
    `create_by`      varchar(64)   DEFAULT NULL,
    `update_by`      varchar(64)   DEFAULT NULL,
    `del_flag`       tinyint(1)    DEFAULT '0',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='错误日志记录';

-- System Slow SQL Log
CREATE TABLE IF NOT EXISTS `sys_slow_sql_log`
(
    `id`             bigint(20) NOT NULL COMMENT '主键ID',
    `trace_id`       varchar(64)   DEFAULT NULL COMMENT '追踪ID',
    `user_id`        bigint(20)    DEFAULT NULL COMMENT '用户ID',
    `user_name`      varchar(64)   DEFAULT NULL COMMENT '用户名称',
    `execution_time` bigint(20)    DEFAULT NULL COMMENT '执行时长(ms)',
    `sql_statement`  longtext COMMENT 'SQL语句',
    `create_time`    datetime     DEFAULT NULL,
    `update_time`    datetime     DEFAULT NULL,
    `create_by`      varchar(64)  DEFAULT NULL,
    `update_by`      varchar(64)  DEFAULT NULL,
    `del_flag`       tinyint(1)   DEFAULT '0',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='慢SQL日志记录';

-- System User Notification
CREATE TABLE IF NOT EXISTS `sys_user_notification`
(
    `id`          bigint(20) NOT NULL COMMENT '主键ID',
    `user_id`     bigint(20)    DEFAULT NULL COMMENT '接收用户ID',
    `title`       varchar(128)  DEFAULT NULL COMMENT '标题',
    `content`     varchar(1000) DEFAULT NULL COMMENT '内容',
    `type`        varchar(32)   DEFAULT NULL COMMENT '类型',
    `category`    varchar(32)   DEFAULT 'SYSTEM' COMMENT '分类',
    `is_read`     tinyint(1)    DEFAULT '0' COMMENT '是否已读',
    `biz_id`      bigint(20)    DEFAULT NULL COMMENT '业务ID',
    `biz_type`    varchar(64)   DEFAULT NULL COMMENT '业务类型',
    `action_type` varchar(32)   DEFAULT NULL COMMENT '动作类型',
    `action_url`  varchar(255)  DEFAULT NULL COMMENT '动作跳转地址',
    `priority`    int(11)       DEFAULT '0' COMMENT '优先级',
    `sender_id`   bigint(20)    DEFAULT NULL COMMENT '发送用户ID',
    `create_time` datetime      DEFAULT NULL,
    `update_time` datetime      DEFAULT NULL,
    `create_by`   varchar(64)   DEFAULT NULL,
    `update_by`   varchar(64)   DEFAULT NULL,
    `del_flag`    tinyint(1)    DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户系统通知';

-- System Social User
CREATE TABLE IF NOT EXISTS `sys_social_user`
(
    `id`          bigint(20) NOT NULL COMMENT '主键ID',
    `user_id`     bigint(20) NOT NULL COMMENT '用户ID',
    `provider`    varchar(32)  DEFAULT NULL COMMENT '第三方平台',
    `open_id`     varchar(128) DEFAULT NULL COMMENT '平台OpenID',
    `union_id`    varchar(128) DEFAULT NULL COMMENT '平台UnionID',
    `nickname`    varchar(128) DEFAULT NULL COMMENT '平台昵称',
    `avatar`      varchar(255) DEFAULT NULL COMMENT '平台头像',
    `create_time` datetime     DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    `create_by`   varchar(64)  DEFAULT NULL,
    `update_by`   varchar(64)  DEFAULT NULL,
    `del_flag`    tinyint(1)   DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_provider_openid` (`provider`, `open_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='社交账号绑定实体';

-- System Post
CREATE TABLE IF NOT EXISTS `sys_post`
(
    `post_id`     bigint(20)  NOT NULL COMMENT '岗位ID',
    `dept_id`     bigint(20)   DEFAULT NULL COMMENT '部门ID',
    `post_code`   varchar(64) NOT NULL COMMENT '岗位编码',
    `post_name`   varchar(64) NOT NULL COMMENT '岗位名称',
    `post_sort`   int(11)      DEFAULT '0' COMMENT '显示顺序',
    `status`      tinyint(1)   DEFAULT '1' COMMENT '状态（1正常 0停用）',
    `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime     DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    `create_by`   varchar(64)  DEFAULT NULL,
    `update_by`   varchar(64)  DEFAULT NULL,
    `del_flag`    tinyint(1)   DEFAULT '0',
    PRIMARY KEY (`post_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='岗位表';

-- System Notice
CREATE TABLE IF NOT EXISTS `sys_notice`
(
    `notice_id`      bigint(20)   NOT NULL COMMENT '公告ID',
    `notice_title`   varchar(128) NOT NULL COMMENT '公告标题',
    `notice_type`    char(1)      NOT NULL COMMENT '公告类型（1通知 2公告）',
    `notice_content` longtext COMMENT '公告内容',
    `status`         tinyint(1)   DEFAULT '1' COMMENT '公告状态（1正常 0关闭）',
    `remark`         varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time`    datetime     DEFAULT NULL,
    `update_time`    datetime     DEFAULT NULL,
    `create_by`      varchar(64)  DEFAULT NULL,
    `update_by`      varchar(64)  DEFAULT NULL,
    `del_flag`       tinyint(1)   DEFAULT '0',
    PRIMARY KEY (`notice_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='通知公告表';

-- System Scheduled Job
CREATE TABLE IF NOT EXISTS `sys_job`
(
    `job_id`          bigint(20)   NOT NULL COMMENT '任务ID',
    `job_name`        varchar(64)  NOT NULL COMMENT '任务名称',
    `job_group`       varchar(64)  NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
    `invoke_target`   varchar(500) NOT NULL COMMENT '调用目标字符串',
    `cron_expression` varchar(255) NOT NULL COMMENT 'Cron执行表达式',
    `misfire_policy`  varchar(32)  NOT NULL DEFAULT 'DO_NOTHING' COMMENT '错过执行策略',
    `concurrent`      char(1)      NOT NULL DEFAULT 'N' COMMENT '是否并发执行（Y允许 N禁止）',
    `status`          tinyint(1)   NOT NULL DEFAULT '0' COMMENT '任务状态（1正常 0暂停）',
    `remark`          varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time`     datetime              DEFAULT NULL,
    `update_time`     datetime              DEFAULT NULL,
    `create_by`       varchar(64)           DEFAULT NULL,
    `update_by`       varchar(64)           DEFAULT NULL,
    `del_flag`        tinyint(1)   NOT NULL DEFAULT '0',
    PRIMARY KEY (`job_id`),
    KEY `idx_job_status` (`status`),
    KEY `idx_job_group` (`job_group`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='定时任务调度表';

-- System Scheduled Job Log
CREATE TABLE IF NOT EXISTS `sys_job_log`
(
    `job_log_id`    bigint(20)   NOT NULL COMMENT '任务日志ID',
    `job_id`        bigint(20)            DEFAULT NULL COMMENT '任务ID',
    `job_name`      varchar(64)  NOT NULL COMMENT '任务名称',
    `job_group`     varchar(64)  NOT NULL COMMENT '任务组名',
    `invoke_target` varchar(500) NOT NULL COMMENT '调用目标字符串',
    `job_message`   varchar(500)          DEFAULT NULL COMMENT '日志信息',
    `status`        tinyint(1)   NOT NULL DEFAULT '1' COMMENT '执行状态（1成功 0失败）',
    `exception_info` longtext COMMENT '异常信息',
    `start_time`    datetime              DEFAULT NULL COMMENT '开始时间',
    `end_time`      datetime              DEFAULT NULL COMMENT '结束时间',
    `duration_ms`   bigint(20)            DEFAULT NULL COMMENT '执行耗时毫秒',
    `create_time`   datetime              DEFAULT NULL,
    `update_time`   datetime              DEFAULT NULL,
    `create_by`     varchar(64)           DEFAULT NULL,
    `update_by`     varchar(64)           DEFAULT NULL,
    `del_flag`      tinyint(1)   NOT NULL DEFAULT '0',
    PRIMARY KEY (`job_log_id`),
    KEY `idx_job_log_job_id` (`job_id`),
    KEY `idx_job_log_start_time` (`start_time`),
    KEY `idx_job_log_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='定时任务调度日志表';

-- Warm-Flow Definition
CREATE TABLE IF NOT EXISTS `flow_definition`
(
    `id`              bigint       NOT NULL COMMENT '主键id',
    `flow_code`       varchar(40)  NOT NULL COMMENT '流程编码',
    `flow_name`       varchar(100) NOT NULL COMMENT '流程名称',
    `model_value`     varchar(40)  NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模型',
    `category`        varchar(100)          DEFAULT NULL COMMENT '流程类别',
    `version`         varchar(20)  NOT NULL COMMENT '流程版本',
    `is_publish`      tinyint(1)   NOT NULL DEFAULT '0' COMMENT '是否发布',
    `form_custom`     char(1)               DEFAULT 'N' COMMENT '是否自定义表单',
    `form_path`       varchar(100)          DEFAULT NULL COMMENT '表单路径',
    `activity_status` tinyint(1)   NOT NULL DEFAULT '1' COMMENT '激活状态',
    `listener_type`   varchar(100)          DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)          DEFAULT NULL COMMENT '监听器路径',
    `ext`             varchar(500)          DEFAULT NULL COMMENT '扩展字段',
    `create_time`     datetime              DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)           DEFAULT '' COMMENT '创建人',
    `update_time`     datetime              DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)           DEFAULT '' COMMENT '更新人',
    `del_flag`        char(1)               DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)           DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='流程定义表';

-- Warm-Flow Node
CREATE TABLE IF NOT EXISTS `flow_node`
(
    `id`              bigint       NOT NULL COMMENT '主键id',
    `node_type`       tinyint(1)   NOT NULL COMMENT '节点类型',
    `definition_id`   bigint       NOT NULL COMMENT '流程定义id',
    `node_code`       varchar(100) NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100) DEFAULT NULL COMMENT '流程节点名称',
    `permission_flag` varchar(200) DEFAULT NULL COMMENT '权限标识',
    `node_ratio`      varchar(200) DEFAULT NULL COMMENT '流程签署比例值',
    `coordinate`      varchar(100) DEFAULT NULL COMMENT '坐标',
    `any_node_skip`   varchar(100) DEFAULT NULL COMMENT '任意结点跳转',
    `listener_type`   varchar(100) DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400) DEFAULT NULL COMMENT '监听器路径',
    `form_custom`     char(1)      DEFAULT 'N' COMMENT '是否自定义表单',
    `form_path`       varchar(100) DEFAULT NULL COMMENT '表单路径',
    `version`         varchar(20)  NOT NULL COMMENT '版本',
    `create_time`     datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)  DEFAULT '' COMMENT '创建人',
    `update_time`     datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)  DEFAULT '' COMMENT '更新人',
    `ext`             text COMMENT '节点扩展属性',
    `del_flag`        char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='流程节点表';

-- Warm-Flow Skip
CREATE TABLE IF NOT EXISTS `flow_skip`
(
    `id`             bigint       NOT NULL COMMENT '主键id',
    `definition_id`  bigint       NOT NULL COMMENT '流程定义id',
    `now_node_code`  varchar(100) NOT NULL COMMENT '当前节点编码',
    `now_node_type`  tinyint(1)   DEFAULT NULL COMMENT '当前节点类型',
    `next_node_code` varchar(100) NOT NULL COMMENT '下一个节点编码',
    `next_node_type` tinyint(1)   DEFAULT NULL COMMENT '下一个节点类型',
    `skip_name`      varchar(100) DEFAULT NULL COMMENT '跳转名称',
    `skip_type`      varchar(40)  DEFAULT NULL COMMENT '跳转类型',
    `skip_condition` varchar(200) DEFAULT NULL COMMENT '跳转条件',
    `coordinate`     varchar(100) DEFAULT NULL COMMENT '坐标',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`      varchar(64)  DEFAULT '' COMMENT '创建人',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`      varchar(64)  DEFAULT '' COMMENT '更新人',
    `del_flag`       char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`      varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='节点跳转关联表';

-- Warm-Flow Instance
CREATE TABLE IF NOT EXISTS `flow_instance`
(
    `id`              bigint      NOT NULL COMMENT '主键id',
    `definition_id`   bigint      NOT NULL COMMENT '流程定义id',
    `flow_name`       varchar(100)         DEFAULT NULL COMMENT '流程名称',
    `business_id`     varchar(80) NOT NULL COMMENT '业务id',
    `node_type`       tinyint(1)  NOT NULL COMMENT '节点类型',
    `node_code`       varchar(40) NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)         DEFAULT NULL COMMENT '流程节点名称',
    `variable`        text COMMENT '任务变量',
    `flow_status`     varchar(20) NOT NULL COMMENT '流程状态',
    `activity_status` tinyint(1)  NOT NULL DEFAULT '1' COMMENT '激活状态',
    `def_json`        text COMMENT '流程定义json',
    `create_time`     datetime             DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime             DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             varchar(500)         DEFAULT NULL COMMENT '扩展字段',
    `del_flag`        char(1)              DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)          DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`),
    KEY `idx_flow_instance_business` (`business_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='流程实例表';

-- Warm-Flow Task
CREATE TABLE IF NOT EXISTS `flow_task`
(
    `id`            bigint       NOT NULL COMMENT '主键id',
    `definition_id` bigint       NOT NULL COMMENT '流程定义id',
    `instance_id`   bigint       NOT NULL COMMENT '流程实例id',
    `flow_name`     varchar(100) DEFAULT NULL COMMENT '流程名称',
    `business_id`   varchar(80)  DEFAULT NULL COMMENT '业务id',
    `node_code`     varchar(100) NOT NULL COMMENT '节点编码',
    `node_name`     varchar(100) DEFAULT NULL COMMENT '节点名称',
    `node_type`     tinyint(1)   NOT NULL COMMENT '节点类型',
    `flow_status`   varchar(20)  NOT NULL DEFAULT '1' COMMENT '流程状态',
    `form_custom`   char(1)      DEFAULT 'N' COMMENT '是否自定义表单',
    `form_path`     varchar(100) DEFAULT NULL COMMENT '表单路径',
    `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`     varchar(64)  DEFAULT '' COMMENT '创建人',
    `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`     varchar(64)  DEFAULT '' COMMENT '更新人',
    `del_flag`      char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`     varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`),
    KEY `idx_flow_task_instance` (`instance_id`),
    KEY `idx_flow_task_business` (`business_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='待办任务表';

-- Warm-Flow History Task
CREATE TABLE IF NOT EXISTS `flow_his_task`
(
    `id`               bigint       NOT NULL COMMENT '主键id',
    `definition_id`    bigint       NOT NULL COMMENT '流程定义id',
    `instance_id`      bigint       NOT NULL COMMENT '流程实例id',
    `task_id`          bigint       NOT NULL COMMENT '任务id',
    `business_id`      varchar(80)  DEFAULT NULL COMMENT '业务id',
    `node_code`        varchar(100) DEFAULT NULL COMMENT '开始节点编码',
    `node_name`        varchar(100) DEFAULT NULL COMMENT '开始节点名称',
    `node_type`        tinyint(1)   DEFAULT NULL COMMENT '开始节点类型',
    `target_node_code` varchar(200) DEFAULT NULL COMMENT '目标节点编码',
    `target_node_name` varchar(200) DEFAULT NULL COMMENT '目标节点名称',
    `approver`         varchar(40)  DEFAULT NULL COMMENT '审批人',
    `cooperate_type`   tinyint(1)   NOT NULL DEFAULT '0' COMMENT '协作方式',
    `collaborator`     varchar(500) DEFAULT NULL COMMENT '协作人',
    `skip_type`        varchar(10)  NOT NULL COMMENT '流转类型',
    `flow_status`      varchar(20)  NOT NULL COMMENT '流程状态',
    `form_custom`      char(1)      DEFAULT 'N' COMMENT '是否自定义表单',
    `form_path`        varchar(100) DEFAULT NULL COMMENT '表单路径',
    `message`          varchar(500) DEFAULT NULL COMMENT '审批意见',
    `variable`         text COMMENT '任务变量',
    `ext`              text COMMENT '业务详情',
    `create_time`      datetime     DEFAULT NULL COMMENT '任务开始时间',
    `update_time`      datetime     DEFAULT NULL COMMENT '审批完成时间',
    `del_flag`         char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`        varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`),
    KEY `idx_flow_his_instance` (`instance_id`),
    KEY `idx_flow_his_approver` (`approver`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='历史任务记录表';

-- Warm-Flow User
CREATE TABLE IF NOT EXISTS `flow_user`
(
    `id`           bigint      NOT NULL COMMENT '主键id',
    `type`         char(1)     NOT NULL COMMENT '人员类型',
    `processed_by` varchar(80) DEFAULT NULL COMMENT '权限人',
    `associated`   bigint      NOT NULL COMMENT '任务表id',
    `create_time`  datetime    DEFAULT NULL COMMENT '创建时间',
    `create_by`    varchar(80) DEFAULT NULL COMMENT '创建人',
    `update_time`  datetime    DEFAULT NULL COMMENT '更新时间',
    `update_by`    varchar(64) DEFAULT '' COMMENT '更新人',
    `del_flag`     char(1)     DEFAULT '0' COMMENT '删除标志',
    `tenant_id`    varchar(40) DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`),
    KEY `user_processed_type` (`processed_by`, `type`),
    KEY `user_associated` (`associated`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='流程用户表';

-- ---------------------------------------------------------
-- 1. ERP Master Data Tables
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS `erp_product_category`
(
    `id`          bigint(20)  NOT NULL COMMENT '分类ID',
    `code`        varchar(64)  NOT NULL COMMENT '分类编码',
    `name`        varchar(128) NOT NULL COMMENT '分类名称',
    `parent_id`   bigint(20)            DEFAULT '0' COMMENT '父分类ID',
    `contact`     varchar(64)           DEFAULT NULL COMMENT '联系人',
    `phone`       varchar(32)           DEFAULT NULL COMMENT '联系电话',
    `address`     varchar(255)          DEFAULT NULL COMMENT '地址',
    `level_id`    bigint(20)            DEFAULT NULL COMMENT '代理等级ID',
    `account_type` varchar(32)          DEFAULT NULL COMMENT '账户类型',
    `opening_balance` decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '期初余额',
    `discount_rate` decimal(10, 4)      DEFAULT NULL COMMENT '折扣率',
    `sort_order`  int(11)               DEFAULT '0' COMMENT '显示顺序',
    `status`      tinyint(1)   NOT NULL DEFAULT '1' COMMENT '状态 (1启用 0停用)',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time` datetime              DEFAULT NULL,
    `update_time` datetime              DEFAULT NULL,
    `create_by`   varchar(64)           DEFAULT NULL,
    `update_by`   varchar(64)           DEFAULT NULL,
    `del_flag`    tinyint(1)   NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_code` (`code`, `del_flag`),
    KEY `idx_category_parent` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP商品分类';

CREATE TABLE IF NOT EXISTS `erp_unit`
(
    `id`          bigint(20)  NOT NULL COMMENT '单位ID',
    `code`        varchar(64)  NOT NULL COMMENT '单位编码',
    `name`        varchar(128) NOT NULL COMMENT '单位名称',
    `parent_id`   bigint(20)            DEFAULT '0' COMMENT '父级ID',
    `contact`     varchar(64)           DEFAULT NULL COMMENT '联系人',
    `phone`       varchar(32)           DEFAULT NULL COMMENT '联系电话',
    `address`     varchar(255)          DEFAULT NULL COMMENT '地址',
    `level_id`    bigint(20)            DEFAULT NULL COMMENT '代理等级ID',
    `account_type` varchar(32)          DEFAULT NULL COMMENT '账户类型',
    `opening_balance` decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '期初余额',
    `discount_rate` decimal(10, 4)      DEFAULT NULL COMMENT '折扣率',
    `sort_order`  int(11)               DEFAULT '0' COMMENT '显示顺序',
    `status`      tinyint(1)   NOT NULL DEFAULT '1' COMMENT '状态 (1启用 0停用)',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time` datetime              DEFAULT NULL,
    `update_time` datetime              DEFAULT NULL,
    `create_by`   varchar(64)           DEFAULT NULL,
    `update_by`   varchar(64)           DEFAULT NULL,
    `del_flag`    tinyint(1)   NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_unit_code` (`code`, `del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP单位';

CREATE TABLE IF NOT EXISTS `erp_product_brand`
(
    `id`          bigint(20)  NOT NULL COMMENT '品牌ID',
    `code`        varchar(64)  NOT NULL COMMENT '品牌编码',
    `name`        varchar(128) NOT NULL COMMENT '品牌名称',
    `parent_id`   bigint(20)            DEFAULT '0' COMMENT '父级ID',
    `contact`     varchar(64)           DEFAULT NULL COMMENT '联系人',
    `phone`       varchar(32)           DEFAULT NULL COMMENT '联系电话',
    `address`     varchar(255)          DEFAULT NULL COMMENT '地址',
    `level_id`    bigint(20)            DEFAULT NULL COMMENT '代理等级ID',
    `account_type` varchar(32)          DEFAULT NULL COMMENT '账户类型',
    `opening_balance` decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '期初余额',
    `discount_rate` decimal(10, 4)      DEFAULT NULL COMMENT '折扣率',
    `sort_order`  int(11)               DEFAULT '0' COMMENT '显示顺序',
    `status`      tinyint(1)   NOT NULL DEFAULT '1' COMMENT '状态 (1启用 0停用)',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time` datetime              DEFAULT NULL,
    `update_time` datetime              DEFAULT NULL,
    `create_by`   varchar(64)           DEFAULT NULL,
    `update_by`   varchar(64)           DEFAULT NULL,
    `del_flag`    tinyint(1)   NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_brand_code` (`code`, `del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP商品品牌';

CREATE TABLE IF NOT EXISTS `erp_product_attribute`
(
    `id`          bigint(20)  NOT NULL COMMENT '属性ID',
    `code`        varchar(64)  NOT NULL COMMENT '属性编码',
    `name`        varchar(128) NOT NULL COMMENT '属性名称',
    `parent_id`   bigint(20)            DEFAULT '0' COMMENT '父级ID',
    `contact`     varchar(64)           DEFAULT NULL COMMENT '联系人',
    `phone`       varchar(32)           DEFAULT NULL COMMENT '联系电话',
    `address`     varchar(255)          DEFAULT NULL COMMENT '地址',
    `level_id`    bigint(20)            DEFAULT NULL COMMENT '代理等级ID',
    `account_type` varchar(32)          DEFAULT NULL COMMENT '账户类型',
    `opening_balance` decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '期初余额',
    `discount_rate` decimal(10, 4)      DEFAULT NULL COMMENT '折扣率',
    `sort_order`  int(11)               DEFAULT '0' COMMENT '显示顺序',
    `status`      tinyint(1)   NOT NULL DEFAULT '1' COMMENT '状态 (1启用 0停用)',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time` datetime              DEFAULT NULL,
    `update_time` datetime              DEFAULT NULL,
    `create_by`   varchar(64)           DEFAULT NULL,
    `update_by`   varchar(64)           DEFAULT NULL,
    `del_flag`    tinyint(1)   NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_attribute_code` (`code`, `del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP商品属性';

CREATE TABLE IF NOT EXISTS `erp_customer`
(
    `id`          bigint(20)  NOT NULL COMMENT '客户ID',
    `code`        varchar(64)  NOT NULL COMMENT '客户编码',
    `name`        varchar(128) NOT NULL COMMENT '客户名称',
    `parent_id`   bigint(20)            DEFAULT '0' COMMENT '父级ID',
    `contact`     varchar(64)           DEFAULT NULL COMMENT '联系人',
    `phone`       varchar(32)           DEFAULT NULL COMMENT '联系电话',
    `address`     varchar(255)          DEFAULT NULL COMMENT '地址',
    `level_id`    bigint(20)            DEFAULT NULL COMMENT '代理等级ID',
    `account_type` varchar(32)          DEFAULT NULL COMMENT '账户类型',
    `opening_balance` decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '期初余额',
    `discount_rate` decimal(10, 4)      DEFAULT NULL COMMENT '折扣率',
    `sort_order`  int(11)               DEFAULT '0' COMMENT '显示顺序',
    `status`      tinyint(1)   NOT NULL DEFAULT '1' COMMENT '状态 (1启用 0停用)',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time` datetime              DEFAULT NULL,
    `update_time` datetime              DEFAULT NULL,
    `create_by`   varchar(64)           DEFAULT NULL,
    `update_by`   varchar(64)           DEFAULT NULL,
    `del_flag`    tinyint(1)   NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_code` (`code`, `del_flag`),
    KEY `idx_customer_level` (`level_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP客户';

CREATE TABLE IF NOT EXISTS `erp_supplier`
(
    `id`          bigint(20)  NOT NULL COMMENT '供应商ID',
    `code`        varchar(64)  NOT NULL COMMENT '供应商编码',
    `name`        varchar(128) NOT NULL COMMENT '供应商名称',
    `parent_id`   bigint(20)            DEFAULT '0' COMMENT '父级ID',
    `contact`     varchar(64)           DEFAULT NULL COMMENT '联系人',
    `phone`       varchar(32)           DEFAULT NULL COMMENT '联系电话',
    `address`     varchar(255)          DEFAULT NULL COMMENT '地址',
    `level_id`    bigint(20)            DEFAULT NULL COMMENT '代理等级ID',
    `account_type` varchar(32)          DEFAULT NULL COMMENT '账户类型',
    `opening_balance` decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '期初余额',
    `discount_rate` decimal(10, 4)      DEFAULT NULL COMMENT '折扣率',
    `sort_order`  int(11)               DEFAULT '0' COMMENT '显示顺序',
    `status`      tinyint(1)   NOT NULL DEFAULT '1' COMMENT '状态 (1启用 0停用)',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time` datetime              DEFAULT NULL,
    `update_time` datetime              DEFAULT NULL,
    `create_by`   varchar(64)           DEFAULT NULL,
    `update_by`   varchar(64)           DEFAULT NULL,
    `del_flag`    tinyint(1)   NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_supplier_code` (`code`, `del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP供应商';

CREATE TABLE IF NOT EXISTS `erp_warehouse`
(
    `id`          bigint(20)  NOT NULL COMMENT '仓库ID',
    `code`        varchar(64)  NOT NULL COMMENT '仓库编码',
    `name`        varchar(128) NOT NULL COMMENT '仓库名称',
    `parent_id`   bigint(20)            DEFAULT '0' COMMENT '父级ID',
    `contact`     varchar(64)           DEFAULT NULL COMMENT '联系人',
    `phone`       varchar(32)           DEFAULT NULL COMMENT '联系电话',
    `address`     varchar(255)          DEFAULT NULL COMMENT '地址',
    `level_id`    bigint(20)            DEFAULT NULL COMMENT '代理等级ID',
    `account_type` varchar(32)          DEFAULT NULL COMMENT '账户类型',
    `opening_balance` decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '期初余额',
    `discount_rate` decimal(10, 4)      DEFAULT NULL COMMENT '折扣率',
    `sort_order`  int(11)               DEFAULT '0' COMMENT '显示顺序',
    `status`      tinyint(1)   NOT NULL DEFAULT '1' COMMENT '状态 (1启用 0停用)',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time` datetime              DEFAULT NULL,
    `update_time` datetime              DEFAULT NULL,
    `create_by`   varchar(64)           DEFAULT NULL,
    `update_by`   varchar(64)           DEFAULT NULL,
    `del_flag`    tinyint(1)   NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_warehouse_code` (`code`, `del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP仓库';

CREATE TABLE IF NOT EXISTS `erp_account`
(
    `id`          bigint(20)    NOT NULL COMMENT '账户ID',
    `code`        varchar(64)    NOT NULL COMMENT '账户编码',
    `name`        varchar(128)   NOT NULL COMMENT '账户名称',
    `parent_id`   bigint(20)              DEFAULT '0' COMMENT '父级ID',
    `contact`     varchar(64)             DEFAULT NULL COMMENT '联系人',
    `phone`       varchar(32)             DEFAULT NULL COMMENT '联系电话',
    `address`     varchar(255)            DEFAULT NULL COMMENT '地址',
    `level_id`    bigint(20)              DEFAULT NULL COMMENT '代理等级ID',
    `account_type` varchar(32)            DEFAULT NULL COMMENT '账户类型',
    `opening_balance` decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '期初余额',
    `discount_rate` decimal(10, 4)        DEFAULT NULL COMMENT '折扣率',
    `sort_order`  int(11)                 DEFAULT '0' COMMENT '显示顺序',
    `status`      tinyint(1)     NOT NULL DEFAULT '1' COMMENT '状态 (1启用 0停用)',
    `remark`      varchar(500)            DEFAULT NULL COMMENT '备注',
    `create_time` datetime                DEFAULT NULL,
    `update_time` datetime                DEFAULT NULL,
    `create_by`   varchar(64)             DEFAULT NULL,
    `update_by`   varchar(64)             DEFAULT NULL,
    `del_flag`    tinyint(1)     NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_code` (`code`, `del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP资金账户';

CREATE TABLE IF NOT EXISTS `erp_agent_level`
(
    `id`          bigint(20)    NOT NULL COMMENT '代理等级ID',
    `code`        varchar(64)    NOT NULL COMMENT '等级编码',
    `name`        varchar(128)   NOT NULL COMMENT '等级名称',
    `parent_id`   bigint(20)              DEFAULT '0' COMMENT '父级ID',
    `contact`     varchar(64)             DEFAULT NULL COMMENT '联系人',
    `phone`       varchar(32)             DEFAULT NULL COMMENT '联系电话',
    `address`     varchar(255)            DEFAULT NULL COMMENT '地址',
    `level_id`    bigint(20)              DEFAULT NULL COMMENT '代理等级ID',
    `account_type` varchar(32)            DEFAULT NULL COMMENT '账户类型',
    `opening_balance` decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '期初余额',
    `discount_rate` decimal(10, 4) DEFAULT NULL COMMENT '折扣率',
    `sort_order`  int(11)                 DEFAULT '0' COMMENT '显示顺序',
    `status`      tinyint(1)     NOT NULL DEFAULT '1' COMMENT '状态 (1启用 0停用)',
    `remark`      varchar(500)            DEFAULT NULL COMMENT '备注',
    `create_time` datetime                DEFAULT NULL,
    `update_time` datetime                DEFAULT NULL,
    `create_by`   varchar(64)             DEFAULT NULL,
    `update_by`   varchar(64)             DEFAULT NULL,
    `del_flag`    tinyint(1)     NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_level_code` (`code`, `del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP代理等级';

CREATE TABLE IF NOT EXISTS `erp_product`
(
    `id`             bigint(20)    NOT NULL COMMENT '商品ID',
    `code`           varchar(64)    NOT NULL COMMENT '商品编号',
    `name`           varchar(128)   NOT NULL COMMENT '商品名称',
    `spec`           varchar(128)            DEFAULT NULL COMMENT '规格型号',
    `category_id`    bigint(20)              DEFAULT NULL COMMENT '商品分类ID',
    `brand_id`       bigint(20)              DEFAULT NULL COMMENT '品牌ID',
    `unit_id`        bigint(20)              DEFAULT NULL COMMENT '单位ID',
    `attribute_text` varchar(255)            DEFAULT NULL COMMENT '辅助属性',
    `barcode`        varchar(128)            DEFAULT NULL COMMENT '条码',
    `location`       varchar(128)            DEFAULT NULL COMMENT '货位',
    `purchase_price` decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '采购价',
    `sale_price`     decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '销售价',
    `retail_price`   decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '零售价',
    `min_stock`      decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '最低库存',
    `max_stock`      decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '最高库存',
    `image_url`      varchar(500)            DEFAULT NULL COMMENT '商品图片',
    `sort_order`     int(11)                 DEFAULT '0' COMMENT '显示顺序',
    `status`         tinyint(1)     NOT NULL DEFAULT '1' COMMENT '状态 (1启用 0停用)',
    `remark`         varchar(500)            DEFAULT NULL COMMENT '备注',
    `create_time`    datetime                DEFAULT NULL,
    `update_time`    datetime                DEFAULT NULL,
    `create_by`      varchar(64)             DEFAULT NULL,
    `update_by`      varchar(64)             DEFAULT NULL,
    `del_flag`       tinyint(1)     NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_code` (`code`, `del_flag`),
    KEY `idx_product_category` (`category_id`),
    KEY `idx_product_brand` (`brand_id`),
    KEY `idx_product_unit` (`unit_id`),
    KEY `idx_product_barcode` (`barcode`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP商品';

CREATE TABLE IF NOT EXISTS `erp_bill`
(
    `id`             bigint(20)     NOT NULL COMMENT '单据ID',
    `bill_no`        varchar(64)     NOT NULL COMMENT '单据编号',
    `bill_type`      varchar(32)     NOT NULL COMMENT '单据类型 SALE/PURCHASE',
    `bill_date`      date            NOT NULL COMMENT '单据日期',
    `partner_id`     bigint(20)      NOT NULL COMMENT '往来单位ID',
    `partner_type`   varchar(32)     NOT NULL COMMENT '往来单位类型 CUSTOMER/SUPPLIER',
    `warehouse_id`   bigint(20)      NOT NULL COMMENT '仓库ID',
    `account_id`     bigint(20)               DEFAULT NULL COMMENT '结算账户ID',
    `employee_id`    bigint(20)               DEFAULT NULL COMMENT '业务员ID',
    `total_qty`      decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '总数量',
    `total_amount`   decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '单据金额',
    `discount_amount` decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '优惠金额',
    `other_amount`   decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '其他费用',
    `payable_amount` decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '应收/应付金额',
    `paid_amount`    decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '实收/实付金额',
    `debt_amount`    decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '欠款金额',
    `audit_status`   tinyint(1)      NOT NULL DEFAULT '0' COMMENT '审核状态 0未审核 1已审核',
    `payment_status` varchar(32)     NOT NULL DEFAULT 'UNPAID' COMMENT '收付款状态',
    `approval_status` varchar(20)     NOT NULL DEFAULT 'NONE' COMMENT '审批状态',
    `approval_instance_id` bigint(20) DEFAULT NULL COMMENT '审批实例ID',
    `approval_submit_by` varchar(64)  DEFAULT NULL COMMENT '审批提交人',
    `approval_submit_time` datetime   DEFAULT NULL COMMENT '审批提交时间',
    `approval_finish_time` datetime   DEFAULT NULL COMMENT '审批完成时间',
    `audit_time`     datetime                 DEFAULT NULL COMMENT '审核时间',
    `audit_by`       varchar(64)              DEFAULT NULL COMMENT '审核人',
    `remark`         varchar(500)             DEFAULT NULL COMMENT '备注',
    `attachment_url` varchar(500)             DEFAULT NULL COMMENT '附件',
    `create_time`    datetime                 DEFAULT NULL,
    `update_time`    datetime                 DEFAULT NULL,
    `create_by`      varchar(64)              DEFAULT NULL,
    `update_by`      varchar(64)              DEFAULT NULL,
    `del_flag`       tinyint(1)      NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bill_no` (`bill_no`, `del_flag`),
    KEY `idx_bill_type_date` (`bill_type`, `bill_date`),
    KEY `idx_bill_partner` (`partner_type`, `partner_id`),
    KEY `idx_bill_audit` (`audit_status`),
    KEY `idx_bill_approval` (`approval_status`, `approval_instance_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP业务单据';

CREATE TABLE IF NOT EXISTS `erp_bill_item`
(
    `id`              bigint(20)     NOT NULL COMMENT '明细ID',
    `bill_id`         bigint(20)      NOT NULL COMMENT '单据ID',
    `product_id`      bigint(20)      NOT NULL COMMENT '商品ID',
    `product_code`    varchar(64)     NOT NULL COMMENT '商品编号',
    `product_name`    varchar(128)    NOT NULL COMMENT '商品名称',
    `spec`            varchar(128)             DEFAULT NULL COMMENT '规格型号',
    `unit_id`         bigint(20)               DEFAULT NULL COMMENT '单位ID',
    `warehouse_id`    bigint(20)      NOT NULL COMMENT '仓库ID',
    `qty`             decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '数量',
    `price`           decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '单价',
    `amount`          decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '金额',
    `discount_rate`   decimal(10, 4)  NOT NULL DEFAULT '100.0000' COMMENT '折扣率',
    `discount_amount` decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '优惠金额',
    `final_amount`    decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '折后金额',
    `remark`          varchar(500)             DEFAULT NULL COMMENT '备注',
    `create_time`     datetime                 DEFAULT NULL,
    `update_time`     datetime                 DEFAULT NULL,
    `create_by`       varchar(64)              DEFAULT NULL,
    `update_by`       varchar(64)              DEFAULT NULL,
    `del_flag`        tinyint(1)      NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_bill_item_bill` (`bill_id`),
    KEY `idx_bill_item_product` (`product_id`),
    KEY `idx_bill_item_warehouse` (`warehouse_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP业务单据明细';

CREATE TABLE IF NOT EXISTS `erp_stock_balance`
(
    `id`           bigint(20)     NOT NULL COMMENT '库存余额ID',
    `product_id`   bigint(20)     NOT NULL COMMENT '商品ID',
    `warehouse_id` bigint(20)     NOT NULL COMMENT '仓库ID',
    `qty`          decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '库存数量',
    `cost_amount`  decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '库存成本',
    `avg_cost`     decimal(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '平均成本',
    `create_time`  datetime               DEFAULT NULL,
    `update_time`  datetime               DEFAULT NULL,
    `create_by`    varchar(64)            DEFAULT NULL,
    `update_by`    varchar(64)            DEFAULT NULL,
    `del_flag`     tinyint(1)    NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_balance` (`product_id`, `warehouse_id`, `del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP库存余额';

CREATE TABLE IF NOT EXISTS `erp_stock_flow`
(
    `id`               bigint(20)     NOT NULL COMMENT '库存流水ID',
    `flow_no`          varchar(64)     NOT NULL COMMENT '流水编号',
    `source_bill_id`   bigint(20)      NOT NULL COMMENT '来源单据ID',
    `source_bill_no`   varchar(64)     NOT NULL COMMENT '来源单号',
    `source_bill_type` varchar(32)     NOT NULL COMMENT '来源类型',
    `product_id`       bigint(20)      NOT NULL COMMENT '商品ID',
    `warehouse_id`     bigint(20)      NOT NULL COMMENT '仓库ID',
    `direction`        varchar(16)     NOT NULL COMMENT '方向 IN/OUT',
    `qty`              decimal(18, 4)  NOT NULL COMMENT '数量',
    `price`            decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '单价',
    `amount`           decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '金额',
    `before_qty`       decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '变动前数量',
    `after_qty`        decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '变动后数量',
    `operate_time`     datetime        NOT NULL COMMENT '操作时间',
    `create_time`      datetime                 DEFAULT NULL,
    `update_time`      datetime                 DEFAULT NULL,
    `create_by`        varchar(64)              DEFAULT NULL,
    `update_by`        varchar(64)              DEFAULT NULL,
    `del_flag`         tinyint(1)      NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_flow_no` (`flow_no`),
    KEY `idx_stock_flow_source` (`source_bill_id`, `source_bill_type`),
    KEY `idx_stock_flow_product` (`product_id`, `warehouse_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP库存流水';

CREATE TABLE IF NOT EXISTS `erp_fund_flow`
(
    `id`               bigint(20)     NOT NULL COMMENT '资金流水ID',
    `flow_no`          varchar(64)     NOT NULL COMMENT '流水编号',
    `source_bill_id`   bigint(20)      NOT NULL COMMENT '来源单据ID',
    `source_bill_no`   varchar(64)     NOT NULL COMMENT '来源单号',
    `source_bill_type` varchar(32)     NOT NULL COMMENT '来源类型',
    `account_id`       bigint(20)      NOT NULL COMMENT '账户ID',
    `direction`        varchar(16)     NOT NULL COMMENT '方向 IN/OUT',
    `amount`           decimal(18, 4)  NOT NULL COMMENT '金额',
    `before_balance`   decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '变动前余额',
    `after_balance`    decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '变动后余额',
    `remark`           varchar(500)             DEFAULT NULL COMMENT '备注',
    `operate_time`     datetime        NOT NULL COMMENT '操作时间',
    `create_time`      datetime                 DEFAULT NULL,
    `update_time`      datetime                 DEFAULT NULL,
    `create_by`        varchar(64)              DEFAULT NULL,
    `update_by`        varchar(64)              DEFAULT NULL,
    `del_flag`         tinyint(1)      NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fund_flow_no` (`flow_no`),
    KEY `idx_fund_flow_source` (`source_bill_id`, `source_bill_type`),
    KEY `idx_fund_flow_account` (`account_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP资金流水';

CREATE TABLE IF NOT EXISTS `erp_partner_flow`
(
    `id`               bigint(20)     NOT NULL COMMENT '往来流水ID',
    `source_bill_id`   bigint(20)      NOT NULL COMMENT '来源单据ID',
    `source_bill_no`   varchar(64)     NOT NULL COMMENT '来源单号',
    `source_bill_type` varchar(32)     NOT NULL COMMENT '来源类型',
    `partner_id`       bigint(20)      NOT NULL COMMENT '往来单位ID',
    `partner_type`     varchar(32)     NOT NULL COMMENT '往来单位类型',
    `direction`        varchar(32)     NOT NULL COMMENT '方向 RECEIVABLE/PAYABLE/RECEIVE/PAY',
    `amount`           decimal(18, 4)  NOT NULL COMMENT '金额',
    `remark`           varchar(500)             DEFAULT NULL COMMENT '备注',
    `operate_time`     datetime        NOT NULL COMMENT '操作时间',
    `create_time`      datetime                 DEFAULT NULL,
    `update_time`      datetime                 DEFAULT NULL,
    `create_by`        varchar(64)              DEFAULT NULL,
    `update_by`        varchar(64)              DEFAULT NULL,
    `del_flag`         tinyint(1)      NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_partner_flow_source` (`source_bill_id`, `source_bill_type`),
    KEY `idx_partner_flow_partner` (`partner_type`, `partner_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP往来流水';

CREATE TABLE IF NOT EXISTS `erp_finance_bill`
(
    `id`             bigint(20)     NOT NULL COMMENT '财务单据ID',
    `bill_no`        varchar(64)     NOT NULL COMMENT '单据编号',
    `bill_type`      varchar(32)     NOT NULL COMMENT 'RECEIPT/PAYMENT',
    `bill_date`      date            NOT NULL COMMENT '单据日期',
    `partner_id`     bigint(20)      NOT NULL COMMENT '往来单位ID',
    `partner_type`   varchar(32)     NOT NULL COMMENT '往来单位类型',
    `account_id`     bigint(20)      NOT NULL COMMENT '账户ID',
    `amount`         decimal(18, 4)  NOT NULL COMMENT '金额',
    `audit_status`   tinyint(1)      NOT NULL DEFAULT '0' COMMENT '审核状态',
    `approval_status` varchar(20)     NOT NULL DEFAULT 'NONE' COMMENT '审批状态',
    `approval_instance_id` bigint(20) DEFAULT NULL COMMENT '审批实例ID',
    `approval_submit_by` varchar(64)  DEFAULT NULL COMMENT '审批提交人',
    `approval_submit_time` datetime   DEFAULT NULL COMMENT '审批提交时间',
    `approval_finish_time` datetime   DEFAULT NULL COMMENT '审批完成时间',
    `audit_time`     datetime                 DEFAULT NULL COMMENT '审核时间',
    `audit_by`       varchar(64)              DEFAULT NULL COMMENT '审核人',
    `remark`         varchar(500)             DEFAULT NULL COMMENT '备注',
    `create_time`    datetime                 DEFAULT NULL,
    `update_time`    datetime                 DEFAULT NULL,
    `create_by`      varchar(64)              DEFAULT NULL,
    `update_by`      varchar(64)              DEFAULT NULL,
    `del_flag`       tinyint(1)      NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_finance_bill_no` (`bill_no`, `del_flag`),
    KEY `idx_finance_bill_type_date` (`bill_type`, `bill_date`),
    KEY `idx_finance_bill_partner` (`partner_type`, `partner_id`),
    KEY `idx_finance_bill_approval` (`approval_status`, `approval_instance_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP财务单据';

CREATE TABLE IF NOT EXISTS `erp_stock_check`
(
    `id`                  bigint(20)     NOT NULL COMMENT '盘点单ID',
    `check_no`            varchar(64)     NOT NULL COMMENT '盘点单号',
    `check_date`          date            NOT NULL COMMENT '盘点日期',
    `warehouse_id`        bigint(20)      NOT NULL COMMENT '仓库ID',
    `total_profit_qty`    decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '盘盈数量',
    `total_loss_qty`      decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '盘亏数量',
    `total_profit_amount` decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '盘盈金额',
    `total_loss_amount`   decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '盘亏金额',
    `audit_status`        tinyint(1)      NOT NULL DEFAULT '0' COMMENT '审核状态',
    `approval_status`     varchar(20)     NOT NULL DEFAULT 'NONE' COMMENT '审批状态',
    `approval_instance_id` bigint(20)      DEFAULT NULL COMMENT '审批实例ID',
    `approval_submit_by`  varchar(64)      DEFAULT NULL COMMENT '审批提交人',
    `approval_submit_time` datetime        DEFAULT NULL COMMENT '审批提交时间',
    `approval_finish_time` datetime        DEFAULT NULL COMMENT '审批完成时间',
    `audit_time`          datetime                 DEFAULT NULL COMMENT '审核时间',
    `audit_by`            varchar(64)              DEFAULT NULL COMMENT '审核人',
    `remark`              varchar(500)             DEFAULT NULL COMMENT '备注',
    `create_time`         datetime                 DEFAULT NULL,
    `update_time`         datetime                 DEFAULT NULL,
    `create_by`           varchar(64)              DEFAULT NULL,
    `update_by`           varchar(64)              DEFAULT NULL,
    `del_flag`            tinyint(1)      NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_check_no` (`check_no`, `del_flag`),
    KEY `idx_stock_check_date` (`check_date`),
    KEY `idx_stock_check_warehouse` (`warehouse_id`),
    KEY `idx_stock_check_approval` (`approval_status`, `approval_instance_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP库存盘点';

CREATE TABLE IF NOT EXISTS `erp_stock_check_item`
(
    `id`           bigint(20)     NOT NULL COMMENT '盘点明细ID',
    `check_id`     bigint(20)     NOT NULL COMMENT '盘点单ID',
    `product_id`   bigint(20)     NOT NULL COMMENT '商品ID',
    `product_code` varchar(64)     NOT NULL COMMENT '商品编号',
    `product_name` varchar(128)    NOT NULL COMMENT '商品名称',
    `spec`         varchar(128)             DEFAULT NULL COMMENT '规格型号',
    `unit_id`      bigint(20)               DEFAULT NULL COMMENT '单位ID',
    `warehouse_id` bigint(20)      NOT NULL COMMENT '仓库ID',
    `book_qty`     decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '账面数量',
    `check_qty`    decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '盘点数量',
    `diff_qty`     decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '差异数量',
    `cost_price`   decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '成本单价',
    `diff_amount`  decimal(18, 4)  NOT NULL DEFAULT '0.0000' COMMENT '差异金额',
    `remark`       varchar(500)             DEFAULT NULL COMMENT '备注',
    `create_time`  datetime                 DEFAULT NULL,
    `update_time`  datetime                 DEFAULT NULL,
    `create_by`    varchar(64)              DEFAULT NULL,
    `update_by`    varchar(64)              DEFAULT NULL,
    `del_flag`     tinyint(1)      NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_stock_check_item_check` (`check_id`),
    KEY `idx_stock_check_item_product` (`product_id`, `warehouse_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP库存盘点明细';

CREATE TABLE IF NOT EXISTS `erp_bill_no_rule`
(
    `id`            bigint(20)    NOT NULL COMMENT '规则ID',
    `bill_type`     varchar(32)    NOT NULL COMMENT '单据类型',
    `bill_name`     varchar(64)    NOT NULL COMMENT '规则名称',
    `prefix`        varchar(32)    NOT NULL COMMENT '单号前缀',
    `date_pattern`  varchar(32)    NOT NULL DEFAULT 'yyyyMMdd' COMMENT '日期格式',
    `serial_length` int(11)        NOT NULL DEFAULT '4' COMMENT '流水长度',
    `next_serial`   bigint(20)     NOT NULL DEFAULT '1' COMMENT '下一流水号',
    `reset_cycle`   varchar(16)    NOT NULL DEFAULT 'DAY' COMMENT '重置周期 DAY/MONTH/NONE',
    `last_date_part` varchar(32)             DEFAULT NULL COMMENT '最近编号日期片段',
    `enabled`       tinyint(1)     NOT NULL DEFAULT '1' COMMENT '是否启用',
    `remark`        varchar(500)            DEFAULT NULL COMMENT '备注',
    `create_time`   datetime                DEFAULT NULL,
    `update_time`   datetime                DEFAULT NULL,
    `create_by`     varchar(64)             DEFAULT NULL,
    `update_by`     varchar(64)             DEFAULT NULL,
    `del_flag`      tinyint(1)     NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bill_no_rule_type` (`bill_type`, `del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP单号规则';

CREATE TABLE IF NOT EXISTS `erp_field_setting`
(
    `id`           bigint(20)   NOT NULL COMMENT '字段设置ID',
    `module_code`  varchar(64)   NOT NULL COMMENT '模块编码',
    `field_key`    varchar(64)   NOT NULL COMMENT '字段键',
    `field_label`  varchar(128)  NOT NULL COMMENT '字段名称',
    `visible`      tinyint(1)    NOT NULL DEFAULT '1' COMMENT '是否显示',
    `required`     tinyint(1)    NOT NULL DEFAULT '0' COMMENT '是否必填',
    `sort_order`   int(11)       NOT NULL DEFAULT '0' COMMENT '排序',
    `width`        int(11)                DEFAULT NULL COMMENT '显示宽度',
    `remark`       varchar(500)           DEFAULT NULL COMMENT '备注',
    `create_time`  datetime               DEFAULT NULL,
    `update_time`  datetime               DEFAULT NULL,
    `create_by`    varchar(64)            DEFAULT NULL,
    `update_by`    varchar(64)            DEFAULT NULL,
    `del_flag`     tinyint(1)    NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_field_setting` (`module_code`, `field_key`, `del_flag`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP字段设置';

CREATE TABLE IF NOT EXISTS `erp_print_template`
(
    `id`            bigint(20)   NOT NULL COMMENT '模板ID',
    `template_code` varchar(64)   NOT NULL COMMENT '模板编码',
    `template_name` varchar(128)  NOT NULL COMMENT '模板名称',
    `bill_type`     varchar(32)   NOT NULL COMMENT '单据类型',
    `paper_type`    varchar(32)   NOT NULL DEFAULT 'A4' COMMENT '纸张类型',
    `content_json`  longtext               DEFAULT NULL COMMENT '模板JSON',
    `is_default`    tinyint(1)    NOT NULL DEFAULT '0' COMMENT '是否默认',
    `status`        tinyint(1)    NOT NULL DEFAULT '1' COMMENT '状态',
    `remark`        varchar(500)           DEFAULT NULL COMMENT '备注',
    `create_time`   datetime               DEFAULT NULL,
    `update_time`   datetime               DEFAULT NULL,
    `create_by`     varchar(64)            DEFAULT NULL,
    `update_by`     varchar(64)            DEFAULT NULL,
    `del_flag`      tinyint(1)    NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_print_template_code` (`template_code`, `del_flag`),
    KEY `idx_print_template_bill_type` (`bill_type`, `is_default`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='ERP打印模板';

-- ---------------------------------------------------------
-- 2. Initial Seed Data (Admin Account & Roles)
-- ---------------------------------------------------------

-- Default Admin User
-- Note: Password is salted BCrypt
INSERT IGNORE INTO `sys_user`
(`user_id`, `username`, `password`, `nickname`, `status`, `create_time`, `del_flag`)
VALUES (1, 'admin', '$2a$10$a2pcS0rGCTLO.tR9UbvlnuXmHH5O/d/iXmSOENmr90Gvcd.plM9Au', '超级管理员', 1, NOW(), 0);

-- Default Super Admin Role
INSERT IGNORE INTO `sys_role`
(`role_id`, `role_name`, `role_key`, `sort_order`, `status`, `create_time`, `del_flag`)
VALUES (1, '超级管理员', 'admin', 1, 1, NOW(), 0);

-- Default Department
INSERT IGNORE INTO `sys_dept`
(`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `create_time`, `del_flag`)
VALUES (1, '0', '0', '总部', 1, NULL, NULL, NULL, 1, NOW(), 0);

-- Default Post
INSERT IGNORE INTO `sys_post`
(`post_id`, `dept_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_time`, `del_flag`)
VALUES (1, 1, 'admin', '管理员', 1, 1, NOW(), 0);

-- Assign User to Role
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
VALUES (1, 1);

-- Assign User to Post
INSERT IGNORE INTO `sys_user_post` (`user_id`, `post_id`)
VALUES (1, 1);

-- Default ERP bill number rules
INSERT IGNORE INTO `erp_bill_no_rule`
(`id`, `bill_type`, `bill_name`, `prefix`, `date_pattern`, `serial_length`, `next_serial`, `reset_cycle`, `enabled`, `remark`, `create_time`, `del_flag`)
VALUES
(30001, 'SALE', '销售单', 'XS', 'yyyyMMdd', 4, 1, 'DAY', 1, '系统默认销售单号规则', NOW(), 0),
(30002, 'SALE_RETURN', '销售退货单', 'XSTH', 'yyyyMMdd', 4, 1, 'DAY', 1, '系统默认销售退货单号规则', NOW(), 0),
(30003, 'PURCHASE', '进货单', 'JH', 'yyyyMMdd', 4, 1, 'DAY', 1, '系统默认进货单号规则', NOW(), 0),
(30004, 'PURCHASE_RETURN', '进货退货单', 'JHTH', 'yyyyMMdd', 4, 1, 'DAY', 1, '系统默认进货退货单号规则', NOW(), 0),
(30005, 'RECEIPT', '收款单', 'SK', 'yyyyMMdd', 4, 1, 'DAY', 1, '系统默认收款单号规则', NOW(), 0),
(30006, 'PAYMENT', '付款单', 'FK', 'yyyyMMdd', 4, 1, 'DAY', 1, '系统默认付款单号规则', NOW(), 0),
(30007, 'INCOME', '其他收入', 'QTSR', 'yyyyMMdd', 4, 1, 'DAY', 1, '系统默认其他收入单号规则', NOW(), 0),
(30008, 'EXPENSE', '其他支出', 'QTZC', 'yyyyMMdd', 4, 1, 'DAY', 1, '系统默认其他支出单号规则', NOW(), 0),
(30009, 'STOCK_CHECK', '库存盘点', 'PD', 'yyyyMMdd', 4, 1, 'DAY', 1, '系统默认库存盘点单号规则', NOW(), 0);

-- Default Menus
INSERT IGNORE INTO `sys_menu`
(`menu_id`, `parent_id`, `menu_name`, `menu_type`, `icon`, `path`, `component`, `permission`, `sort_order`, `visible`, `status`, `is_cache`, `create_time`, `del_flag`)
VALUES
(1000, 0, '数据看板', 1, 'DataAnalysis', '/dashboard', 'home/index', NULL, 1, 1, 1, 'N', NOW(), 0),
(1100, 0, '系统管理', 0, 'Setting', '/sys', 'Layout', NULL, 10, 1, 1, 'N', NOW(), 0),
(1110, 1100, '用户管理', 1, 'User', '/sys/user', 'sys/user/index', 'sys:user:list', 11, 1, 1, 'N', NOW(), 0),
(1120, 1100, '角色管理', 1, 'Avatar', '/sys/role', 'sys/role/index', 'sys:role:list', 12, 1, 1, 'N', NOW(), 0),
(1130, 1100, '菜单管理', 1, 'Menu', '/sys/menu', 'sys/menu/index', 'sys:menu:list', 13, 1, 1, 'N', NOW(), 0),
(1140, 1100, '文件管理', 1, 'FolderOpened', '/sys/file', 'sys/file/index', 'sys:file:list', 14, 1, 1, 'N', NOW(), 0),
(1200, 0, '基础设置', 0, 'Tools', '/system', 'Layout', NULL, 20, 1, 1, 'N', NOW(), 0),
(1210, 1200, '部门管理', 1, 'OfficeBuilding', '/system/dept', 'system/dept/index', 'system:dept:list', 21, 1, 1, 'N', NOW(), 0),
(1220, 1200, '岗位管理', 1, 'Postcard', '/system/post', 'system/post/index', 'system:post:list', 22, 1, 1, 'N', NOW(), 0),
(1230, 1200, '字典管理', 1, 'Collection', '/system/dict', 'system/dict/index', 'system:dict:list', 23, 1, 1, 'N', NOW(), 0),
(1240, 1200, '参数配置', 1, 'Operation', '/system/config', 'system/config/index', 'system:config:list', 24, 1, 1, 'N', NOW(), 0),
(1250, 1200, '通知公告', 1, 'Bell', '/system/notice', 'system/notice/index', 'system:notice:list', 25, 1, 1, 'N', NOW(), 0),
(1260, 1200, '职员管理', 1, 'UserFilled', '/system/staff', 'system/staff/index', 'system:staff:list', 26, 1, 1, 'N', NOW(), 0),
(1300, 0, '日志管理', 0, 'Document', '/sys/log', 'ParentView', NULL, 30, 1, 1, 'N', NOW(), 0),
(1310, 1300, '操作日志', 1, 'Tickets', '/sys/log/oper', 'sys/log/oper/index', 'sys:log:oper', 31, 1, 1, 'N', NOW(), 0),
(1320, 1300, '登录日志', 1, 'Key', '/sys/log/login', 'sys/log/login/index', 'sys:log:login', 32, 1, 1, 'N', NOW(), 0),
(1330, 1300, '错误日志', 1, 'Warning', '/sys/log/error', 'sys/log/error/index', 'sys:log:error', 33, 1, 1, 'N', NOW(), 0),
(1340, 1300, '慢SQL日志', 1, 'Timer', '/sys/log/slow-sql', 'sys/log/slowSql/index', 'sys:log:slowSql', 34, 1, 1, 'N', NOW(), 0),
(1400, 0, '系统监控', 0, 'Monitor', '/monitor', 'Layout', NULL, 40, 1, 1, 'N', NOW(), 0),
(1410, 1400, '服务监控', 1, 'DataAnalysis', '/monitor/admin', 'monitor/admin/index', 'monitor:admin:view', 41, 1, 1, 'N', NOW(), 0),
(1420, 1400, '缓存监控', 1, 'Cpu', '/monitor/cache', 'monitor/cache/index', 'monitor:cache:view', 42, 1, 1, 'N', NOW(), 0),
(1430, 1400, '在线用户', 1, 'Connection', '/monitor/online', 'monitor/online/index', 'monitor:online:view', 43, 1, 1, 'N', NOW(), 0),
(1440, 1400, '实时日志', 1, 'Document', '/monitor/log', 'monitor/log/index', 'monitor:log:view', 44, 1, 1, 'N', NOW(), 0),
(1450, 1400, '任务监控', 1, 'Timer', '/monitor/job', 'monitor/job/index', 'monitor:job:list', 45, 1, 1, 'N', NOW(), 0),
(1500, 0, '审批中心', 0, 'Stamp', '/erp/approval', 'Layout', NULL, 50, 1, 1, 'N', NOW(), 0),
(1510, 1500, '待我审批', 1, 'Checked', '/erp/approval/todo', 'erp/approval/todo', 'erp:approval:task', 51, 1, 1, 'N', NOW(), 0),
(1520, 1500, '我发起的', 1, 'Promotion', '/erp/approval/mine', 'erp/approval/mine', 'erp:approval:task', 52, 1, 1, 'N', NOW(), 0),
(1530, 1500, '已办审批', 1, 'Finished', '/erp/approval/done', 'erp/approval/done', 'erp:approval:task', 53, 1, 1, 'N', NOW(), 0),
(1540, 1500, '流程设计器', 1, 'Share', '/erp/workflow/designer', 'erp/approval/designer', 'erp:workflow:designer', 54, 1, 1, 'N', NOW(), 0),
(2000, 0, '商品', 0, 'Goods', '/erp/product', 'Layout', NULL, 100, 1, 1, 'N', NOW(), 0),
(2010, 2000, '商品分类', 1, 'FolderOpened', '/erp/product/category', 'erp/master/index', 'erp:product-category:list', 101, 1, 1, 'N', NOW(), 0),
(2020, 2000, '单位管理', 1, 'CollectionTag', '/erp/product/unit', 'erp/master/index', 'erp:unit:list', 102, 1, 1, 'N', NOW(), 0),
(2030, 2000, '商品品牌', 1, 'PriceTag', '/erp/product/brand', 'erp/master/index', 'erp:product-brand:list', 103, 1, 1, 'N', NOW(), 0),
(2040, 2000, '属性设置', 1, 'Operation', '/erp/product/attribute', 'erp/master/index', 'erp:product-attribute:list', 104, 1, 1, 'N', NOW(), 0),
(2050, 2000, '商品管理', 1, 'Goods', '/erp/product/list', 'erp/product/index', 'erp:product:list', 100, 1, 1, 'N', NOW(), 0),
(2100, 0, 'ERP设置', 0, 'Tools', '/erp/setting', 'Layout', NULL, 110, 1, 1, 'N', NOW(), 0),
(2110, 2100, '客户管理', 1, 'User', '/erp/setting/customer', 'erp/master/index', 'erp:customer:list', 111, 1, 1, 'N', NOW(), 0),
(2120, 2100, '供应商管理', 1, 'Van', '/erp/setting/supplier', 'erp/master/index', 'erp:supplier:list', 112, 1, 1, 'N', NOW(), 0),
(2130, 2100, '仓库管理', 1, 'House', '/erp/setting/warehouse', 'erp/master/index', 'erp:warehouse:list', 113, 1, 1, 'N', NOW(), 0),
(2140, 2100, '账户管理', 1, 'Wallet', '/erp/setting/account', 'erp/master/index', 'erp:account:list', 114, 1, 1, 'N', NOW(), 0),
(2150, 2100, '代理等级', 1, 'Medal', '/erp/setting/agent-level', 'erp/master/index', 'erp:agent-level:list', 115, 1, 1, 'N', NOW(), 0),
(2160, 2100, '单号规则', 1, 'Tickets', '/erp/setting/bill-no-rule', 'erp/config/billNoRule', 'erp:config:bill-no-rule:list', 116, 1, 1, 'N', NOW(), 0),
(2170, 2100, '字段设置', 1, 'SetUp', '/erp/setting/field-setting', 'erp/config/fieldSetting', 'erp:config:field-setting:list', 117, 1, 1, 'N', NOW(), 0),
(2180, 2100, '打印模板', 1, 'Printer', '/erp/setting/print-template', 'erp/config/printTemplate', 'erp:config:print-template:list', 118, 1, 1, 'N', NOW(), 0),
(2200, 0, '销售', 0, 'Sell', '/erp/sale', 'Layout', NULL, 120, 1, 1, 'N', NOW(), 0),
(2210, 2200, '销售单', 1, 'Document', '/erp/sale/list', 'erp/bill/index', 'erp:sale:list', 121, 1, 1, 'N', NOW(), 0),
(2220, 2200, '新增销售单', 1, 'Plus', '/erp/sale/add', 'erp/bill/form', 'erp:sale:add', 122, 1, 1, 'N', NOW(), 0),
(2230, 2200, '销售退货单', 1, 'Document', '/erp/sale-return/list', 'erp/bill/index', 'erp:sale-return:list', 123, 1, 1, 'N', NOW(), 0),
(2240, 2200, '新增销售退货', 1, 'Plus', '/erp/sale-return/add', 'erp/bill/form', 'erp:sale-return:add', 124, 1, 1, 'N', NOW(), 0),
(2300, 0, '进货', 0, 'ShoppingCart', '/erp/purchase', 'Layout', NULL, 130, 1, 1, 'N', NOW(), 0),
(2310, 2300, '进货单', 1, 'Document', '/erp/purchase/list', 'erp/bill/index', 'erp:purchase:list', 131, 1, 1, 'N', NOW(), 0),
(2320, 2300, '新增进货单', 1, 'Plus', '/erp/purchase/add', 'erp/bill/form', 'erp:purchase:add', 132, 1, 1, 'N', NOW(), 0),
(2330, 2300, '进货退货单', 1, 'Document', '/erp/purchase-return/list', 'erp/bill/index', 'erp:purchase-return:list', 133, 1, 1, 'N', NOW(), 0),
(2340, 2300, '新增进货退货', 1, 'Plus', '/erp/purchase-return/add', 'erp/bill/form', 'erp:purchase-return:add', 134, 1, 1, 'N', NOW(), 0),
(2400, 0, '库存', 0, 'Box', '/erp/stock', 'Layout', NULL, 140, 1, 1, 'N', NOW(), 0),
(2410, 2400, '库存查询', 1, 'Search', '/erp/stock/balance', 'erp/stock/balance', 'erp:stock:balance', 141, 1, 1, 'N', NOW(), 0),
(2420, 2400, '商品收发明细', 1, 'Tickets', '/erp/stock/flow', 'erp/stock/flow', 'erp:stock:flow', 142, 1, 1, 'N', NOW(), 0),
(2430, 2400, '库存盘点', 1, 'Checked', '/erp/stock/check', 'erp/stock/check', 'erp:stock-check:list', 143, 1, 1, 'N', NOW(), 0),
(2440, 2400, '新增库存盘点', 1, 'Plus', '/erp/stock/check-add', 'erp/stock/checkForm', 'erp:stock-check:add', 144, 1, 1, 'N', NOW(), 0),
(2450, 2400, '库存预警', 1, 'Warning', '/erp/stock/warning', 'erp/stock/warning', 'erp:stock:warning', 145, 1, 1, 'N', NOW(), 0),
(2500, 0, '财务', 0, 'Money', '/erp/finance', 'Layout', NULL, 150, 1, 1, 'N', NOW(), 0),
(2510, 2500, '收款单', 1, 'Wallet', '/erp/finance/receipt', 'erp/finance/form', 'erp:finance:receipt:list', 151, 1, 1, 'N', NOW(), 0),
(2520, 2500, '付款单', 1, 'WalletFilled', '/erp/finance/payment', 'erp/finance/form', 'erp:finance:payment:list', 152, 1, 1, 'N', NOW(), 0),
(2530, 2500, '其他收入', 1, 'CirclePlus', '/erp/finance/income', 'erp/finance/form', 'erp:finance:income:list', 153, 1, 1, 'N', NOW(), 0),
(2540, 2500, '其他支出', 1, 'Remove', '/erp/finance/expense', 'erp/finance/form', 'erp:finance:expense:list', 154, 1, 1, 'N', NOW(), 0),
(2550, 2500, '资金流水', 1, 'List', '/erp/finance/fund-flow', 'erp/finance/fundFlow', 'erp:finance:fund-flow', 155, 1, 1, 'N', NOW(), 0),
(2560, 2500, '往来流水', 1, 'List', '/erp/finance/partner-flow', 'erp/finance/partnerFlow', 'erp:finance:partner-flow', 156, 1, 1, 'N', NOW(), 0),
(2600, 0, '报表', 0, 'TrendCharts', '/erp/report', 'Layout', NULL, 160, 1, 1, 'N', NOW(), 0),
(2610, 2600, '销售统计', 1, 'DataLine', '/erp/report/sale-stat', 'erp/report/generic', 'erp:report:sale-stat', 161, 1, 1, 'N', NOW(), 0),
(2620, 2600, '销售明细', 1, 'DataLine', '/erp/report/sale-detail', 'erp/report/billDetail', 'erp:report:sale-detail', 162, 1, 1, 'N', NOW(), 0),
(2630, 2600, '销售利润表（按商品）', 1, 'DataAnalysis', '/erp/report/sale-profit-product', 'erp/report/generic', 'erp:report:profit', 163, 1, 1, 'N', NOW(), 0),
(2640, 2600, '销售利润表（按单据）', 1, 'DataAnalysis', '/erp/report/sale-profit-bill', 'erp/report/generic', 'erp:report:profit', 164, 1, 1, 'N', NOW(), 0),
(2650, 2600, '销售利润表（按客户）', 1, 'DataAnalysis', '/erp/report/sale-profit-customer', 'erp/report/generic', 'erp:report:profit', 165, 1, 1, 'N', NOW(), 0),
(2660, 2600, '销售分析', 1, 'TrendCharts', '/erp/report/sale-analysis', 'erp/report/generic', 'erp:report:sale-analysis', 166, 1, 1, 'N', NOW(), 0),
(2670, 2600, '经营利润', 1, 'Histogram', '/erp/report/business-profit', 'erp/report/generic', 'erp:report:business-profit', 167, 1, 1, 'N', NOW(), 0),
(2680, 2600, '商品热销榜', 1, 'Goods', '/erp/report/hot-products', 'erp/report/generic', 'erp:report:hot-products', 168, 1, 1, 'N', NOW(), 0),
(2690, 2600, '进货统计', 1, 'DataLine', '/erp/report/purchase-stat', 'erp/report/generic', 'erp:report:purchase-stat', 169, 1, 1, 'N', NOW(), 0),
(2700, 2600, '进货明细', 1, 'DataLine', '/erp/report/purchase-detail', 'erp/report/billDetail', 'erp:report:purchase-detail', 170, 1, 1, 'N', NOW(), 0),
(2710, 2600, '库存余额', 1, 'DataAnalysis', '/erp/report/stock-balance', 'erp/report/stockBalance', 'erp:report:stock-balance', 171, 1, 1, 'N', NOW(), 0),
(2720, 2600, '应收应付', 1, 'DataBoard', '/erp/report/partner-balance', 'erp/report/partnerBalance', 'erp:report:partner-balance', 172, 1, 1, 'N', NOW(), 0),
(2730, 2600, '账户余额', 1, 'CreditCard', '/erp/report/account-balance', 'erp/report/accountBalance', 'erp:report:account-balance', 173, 1, 1, 'N', NOW(), 0),
(2740, 2600, '员工业绩统计', 1, 'User', '/erp/report/employee-performance', 'erp/report/generic', 'erp:report:employee-performance', 174, 1, 1, 'N', NOW(), 0),
(2750, 2600, '员工业绩提成', 1, 'Money', '/erp/report/employee-commission', 'erp/report/generic', 'erp:report:employee-performance', 175, 1, 1, 'N', NOW(), 0),
(2760, 2600, '商品收发汇总表', 1, 'Tickets', '/erp/report/stock-summary', 'erp/report/generic', 'erp:report:stock-summary', 176, 1, 1, 'N', NOW(), 0),
(2770, 2600, '商品进销存变动统计', 1, 'Operation', '/erp/report/inventory-change', 'erp/report/generic', 'erp:report:inventory-change', 177, 1, 1, 'N', NOW(), 0),
(2780, 2600, '经营汇总', 1, 'Histogram', '/erp/report/summary', 'erp/report/summary', 'erp:report:summary', 178, 1, 1, 'N', NOW(), 0);

-- Default Button Permissions
INSERT IGNORE INTO `sys_menu`
(`menu_id`, `parent_id`, `menu_name`, `menu_type`, `icon`, `path`, `component`, `permission`, `sort_order`, `visible`, `status`, `is_cache`, `create_time`, `del_flag`)
VALUES
(1111, 1110, '用户新增', 2, NULL, NULL, NULL, 'sys:user:add', 1, 1, 1, 'N', NOW(), 0),
(1112, 1110, '用户编辑', 2, NULL, NULL, NULL, 'sys:user:edit', 2, 1, 1, 'N', NOW(), 0),
(1113, 1110, '用户删除', 2, NULL, NULL, NULL, 'sys:user:remove', 3, 1, 1, 'N', NOW(), 0),
(1114, 1110, '用户重置密码', 2, NULL, NULL, NULL, 'sys:user:resetPwd', 4, 1, 1, 'N', NOW(), 0),
(1115, 1110, '用户分配角色', 2, NULL, NULL, NULL, 'sys:user:authRole', 5, 1, 1, 'N', NOW(), 0),
(1121, 1120, '角色新增', 2, NULL, NULL, NULL, 'sys:role:add', 1, 1, 1, 'N', NOW(), 0),
(1122, 1120, '角色编辑', 2, NULL, NULL, NULL, 'sys:role:edit', 2, 1, 1, 'N', NOW(), 0),
(1123, 1120, '角色删除', 2, NULL, NULL, NULL, 'sys:role:remove', 3, 1, 1, 'N', NOW(), 0),
(1124, 1120, '角色分配用户', 2, NULL, NULL, NULL, 'sys:role:authUser', 4, 1, 1, 'N', NOW(), 0),
(1125, 1120, '角色查询', 2, NULL, NULL, NULL, 'sys:role:query', 5, 1, 1, 'N', NOW(), 0),
(1131, 1130, '菜单新增', 2, NULL, NULL, NULL, 'sys:menu:add', 1, 1, 1, 'N', NOW(), 0),
(1132, 1130, '菜单编辑', 2, NULL, NULL, NULL, 'sys:menu:edit', 2, 1, 1, 'N', NOW(), 0),
(1133, 1130, '菜单删除', 2, NULL, NULL, NULL, 'sys:menu:remove', 3, 1, 1, 'N', NOW(), 0),
(1141, 1140, '文件上传', 2, NULL, NULL, NULL, 'sys:file:upload', 1, 1, 1, 'N', NOW(), 0),
(1142, 1140, '文件删除', 2, NULL, NULL, NULL, 'sys:file:remove', 2, 1, 1, 'N', NOW(), 0),
(1143, 1140, '文件配置', 2, NULL, NULL, NULL, 'sys:file:config', 3, 1, 1, 'N', NOW(), 0),
(1211, 1210, '部门新增', 2, NULL, NULL, NULL, 'system:dept:add', 1, 1, 1, 'N', NOW(), 0),
(1212, 1210, '部门编辑', 2, NULL, NULL, NULL, 'system:dept:edit', 2, 1, 1, 'N', NOW(), 0),
(1213, 1210, '部门删除', 2, NULL, NULL, NULL, 'system:dept:remove', 3, 1, 1, 'N', NOW(), 0),
(1221, 1220, '岗位新增', 2, NULL, NULL, NULL, 'system:post:add', 1, 1, 1, 'N', NOW(), 0),
(1222, 1220, '岗位编辑', 2, NULL, NULL, NULL, 'system:post:edit', 2, 1, 1, 'N', NOW(), 0),
(1223, 1220, '岗位删除', 2, NULL, NULL, NULL, 'system:post:remove', 3, 1, 1, 'N', NOW(), 0),
(1231, 1230, '字典新增', 2, NULL, NULL, NULL, 'system:dict:add', 1, 1, 1, 'N', NOW(), 0),
(1232, 1230, '字典编辑', 2, NULL, NULL, NULL, 'system:dict:edit', 2, 1, 1, 'N', NOW(), 0),
(1233, 1230, '字典删除', 2, NULL, NULL, NULL, 'system:dict:remove', 3, 1, 1, 'N', NOW(), 0),
(1241, 1240, '参数新增', 2, NULL, NULL, NULL, 'system:config:add', 1, 1, 1, 'N', NOW(), 0),
(1242, 1240, '参数编辑', 2, NULL, NULL, NULL, 'system:config:edit', 2, 1, 1, 'N', NOW(), 0),
(1243, 1240, '参数删除', 2, NULL, NULL, NULL, 'system:config:remove', 3, 1, 1, 'N', NOW(), 0),
(1251, 1250, '公告新增', 2, NULL, NULL, NULL, 'system:notice:add', 1, 1, 1, 'N', NOW(), 0),
(1252, 1250, '公告编辑', 2, NULL, NULL, NULL, 'system:notice:edit', 2, 1, 1, 'N', NOW(), 0),
(1253, 1250, '公告删除', 2, NULL, NULL, NULL, 'system:notice:remove', 3, 1, 1, 'N', NOW(), 0),
(1261, 1260, '职员新增', 2, NULL, NULL, NULL, 'system:staff:add', 1, 1, 1, 'N', NOW(), 0),
(1262, 1260, '职员编辑', 2, NULL, NULL, NULL, 'system:staff:edit', 2, 1, 1, 'N', NOW(), 0),
(1263, 1260, '职员删除', 2, NULL, NULL, NULL, 'system:staff:remove', 3, 1, 1, 'N', NOW(), 0),
(1451, 1450, '任务新增', 2, NULL, NULL, NULL, 'monitor:job:add', 1, 1, 1, 'N', NOW(), 0),
(1452, 1450, '任务编辑', 2, NULL, NULL, NULL, 'monitor:job:edit', 2, 1, 1, 'N', NOW(), 0),
(1453, 1450, '任务删除', 2, NULL, NULL, NULL, 'monitor:job:remove', 3, 1, 1, 'N', NOW(), 0),
(1454, 1450, '任务启停', 2, NULL, NULL, NULL, 'monitor:job:changeStatus', 4, 1, 1, 'N', NOW(), 0),
(1455, 1450, '任务执行一次', 2, NULL, NULL, NULL, 'monitor:job:run', 5, 1, 1, 'N', NOW(), 0),
(1456, 1450, '任务日志查询', 2, NULL, NULL, NULL, 'monitor:job:log', 6, 1, 1, 'N', NOW(), 0),
(1457, 1450, '任务日志删除', 2, NULL, NULL, NULL, 'monitor:job:logRemove', 7, 1, 1, 'N', NOW(), 0),
(1458, 1450, '任务日志清空', 2, NULL, NULL, NULL, 'monitor:job:logClean', 8, 1, 1, 'N', NOW(), 0),
(1511, 1510, '审批提交', 2, NULL, NULL, NULL, 'erp:approval:submit', 1, 1, 1, 'N', NOW(), 0),
(1512, 1510, '审批通过', 2, NULL, NULL, NULL, 'erp:approval:approve', 2, 1, 1, 'N', NOW(), 0),
(1513, 1510, '审批驳回', 2, NULL, NULL, NULL, 'erp:approval:reject', 3, 1, 1, 'N', NOW(), 0),
(1514, 1510, '审批撤回', 2, NULL, NULL, NULL, 'erp:approval:revoke', 4, 1, 1, 'N', NOW(), 0),
(1515, 1510, '审批转交', 2, NULL, NULL, NULL, 'erp:approval:transfer', 5, 1, 1, 'N', NOW(), 0),
(1541, 1540, '流程定义', 2, NULL, NULL, NULL, 'erp:workflow:definition', 1, 1, 1, 'N', NOW(), 0),
(2011, 2010, '商品分类新增', 2, NULL, NULL, NULL, 'erp:product-category:add', 1, 1, 1, 'N', NOW(), 0),
(2012, 2010, '商品分类编辑', 2, NULL, NULL, NULL, 'erp:product-category:edit', 2, 1, 1, 'N', NOW(), 0),
(2013, 2010, '商品分类删除', 2, NULL, NULL, NULL, 'erp:product-category:remove', 3, 1, 1, 'N', NOW(), 0),
(2021, 2020, '单位新增', 2, NULL, NULL, NULL, 'erp:unit:add', 1, 1, 1, 'N', NOW(), 0),
(2022, 2020, '单位编辑', 2, NULL, NULL, NULL, 'erp:unit:edit', 2, 1, 1, 'N', NOW(), 0),
(2023, 2020, '单位删除', 2, NULL, NULL, NULL, 'erp:unit:remove', 3, 1, 1, 'N', NOW(), 0),
(2031, 2030, '品牌新增', 2, NULL, NULL, NULL, 'erp:product-brand:add', 1, 1, 1, 'N', NOW(), 0),
(2032, 2030, '品牌编辑', 2, NULL, NULL, NULL, 'erp:product-brand:edit', 2, 1, 1, 'N', NOW(), 0),
(2033, 2030, '品牌删除', 2, NULL, NULL, NULL, 'erp:product-brand:remove', 3, 1, 1, 'N', NOW(), 0),
(2041, 2040, '属性新增', 2, NULL, NULL, NULL, 'erp:product-attribute:add', 1, 1, 1, 'N', NOW(), 0),
(2042, 2040, '属性编辑', 2, NULL, NULL, NULL, 'erp:product-attribute:edit', 2, 1, 1, 'N', NOW(), 0),
(2043, 2040, '属性删除', 2, NULL, NULL, NULL, 'erp:product-attribute:remove', 3, 1, 1, 'N', NOW(), 0),
(2051, 2050, '商品新增', 2, NULL, NULL, NULL, 'erp:product:add', 1, 1, 1, 'N', NOW(), 0),
(2052, 2050, '商品编辑', 2, NULL, NULL, NULL, 'erp:product:edit', 2, 1, 1, 'N', NOW(), 0),
(2053, 2050, '商品删除', 2, NULL, NULL, NULL, 'erp:product:remove', 3, 1, 1, 'N', NOW(), 0),
(2054, 2050, '商品选项', 2, NULL, NULL, NULL, 'erp:product:options', 4, 1, 1, 'N', NOW(), 0),
(2055, 2050, '商品导入', 2, NULL, NULL, NULL, 'erp:product:import', 5, 1, 1, 'N', NOW(), 0),
(2056, 2050, '商品导出', 2, NULL, NULL, NULL, 'erp:product:export', 6, 1, 1, 'N', NOW(), 0),
(2111, 2110, '客户新增', 2, NULL, NULL, NULL, 'erp:customer:add', 1, 1, 1, 'N', NOW(), 0),
(2112, 2110, '客户编辑', 2, NULL, NULL, NULL, 'erp:customer:edit', 2, 1, 1, 'N', NOW(), 0),
(2113, 2110, '客户删除', 2, NULL, NULL, NULL, 'erp:customer:remove', 3, 1, 1, 'N', NOW(), 0),
(2121, 2120, '供应商新增', 2, NULL, NULL, NULL, 'erp:supplier:add', 1, 1, 1, 'N', NOW(), 0),
(2122, 2120, '供应商编辑', 2, NULL, NULL, NULL, 'erp:supplier:edit', 2, 1, 1, 'N', NOW(), 0),
(2123, 2120, '供应商删除', 2, NULL, NULL, NULL, 'erp:supplier:remove', 3, 1, 1, 'N', NOW(), 0),
(2131, 2130, '仓库新增', 2, NULL, NULL, NULL, 'erp:warehouse:add', 1, 1, 1, 'N', NOW(), 0),
(2132, 2130, '仓库编辑', 2, NULL, NULL, NULL, 'erp:warehouse:edit', 2, 1, 1, 'N', NOW(), 0),
(2133, 2130, '仓库删除', 2, NULL, NULL, NULL, 'erp:warehouse:remove', 3, 1, 1, 'N', NOW(), 0),
(2141, 2140, '账户新增', 2, NULL, NULL, NULL, 'erp:account:add', 1, 1, 1, 'N', NOW(), 0),
(2142, 2140, '账户编辑', 2, NULL, NULL, NULL, 'erp:account:edit', 2, 1, 1, 'N', NOW(), 0),
(2143, 2140, '账户删除', 2, NULL, NULL, NULL, 'erp:account:remove', 3, 1, 1, 'N', NOW(), 0),
(2151, 2150, '代理等级新增', 2, NULL, NULL, NULL, 'erp:agent-level:add', 1, 1, 1, 'N', NOW(), 0),
(2152, 2150, '代理等级编辑', 2, NULL, NULL, NULL, 'erp:agent-level:edit', 2, 1, 1, 'N', NOW(), 0),
(2153, 2150, '代理等级删除', 2, NULL, NULL, NULL, 'erp:agent-level:remove', 3, 1, 1, 'N', NOW(), 0),
(2161, 2160, '单号规则新增', 2, NULL, NULL, NULL, 'erp:config:bill-no-rule:add', 1, 1, 1, 'N', NOW(), 0),
(2162, 2160, '单号规则编辑', 2, NULL, NULL, NULL, 'erp:config:bill-no-rule:edit', 2, 1, 1, 'N', NOW(), 0),
(2163, 2160, '单号规则删除', 2, NULL, NULL, NULL, 'erp:config:bill-no-rule:remove', 3, 1, 1, 'N', NOW(), 0),
(2171, 2170, '字段设置新增', 2, NULL, NULL, NULL, 'erp:config:field-setting:add', 1, 1, 1, 'N', NOW(), 0),
(2172, 2170, '字段设置编辑', 2, NULL, NULL, NULL, 'erp:config:field-setting:edit', 2, 1, 1, 'N', NOW(), 0),
(2173, 2170, '字段设置删除', 2, NULL, NULL, NULL, 'erp:config:field-setting:remove', 3, 1, 1, 'N', NOW(), 0),
(2181, 2180, '打印模板新增', 2, NULL, NULL, NULL, 'erp:config:print-template:add', 1, 1, 1, 'N', NOW(), 0),
(2182, 2180, '打印模板编辑', 2, NULL, NULL, NULL, 'erp:config:print-template:edit', 2, 1, 1, 'N', NOW(), 0),
(2183, 2180, '打印模板删除', 2, NULL, NULL, NULL, 'erp:config:print-template:remove', 3, 1, 1, 'N', NOW(), 0),
(2184, 2100, 'ERP参数读取', 2, NULL, NULL, NULL, 'erp:config:params', 119, 1, 1, 'N', NOW(), 0),
(2211, 2210, '销售新增', 2, NULL, NULL, NULL, 'erp:sale:add', 1, 1, 1, 'N', NOW(), 0),
(2212, 2210, '销售编辑', 2, NULL, NULL, NULL, 'erp:sale:edit', 2, 1, 1, 'N', NOW(), 0),
(2213, 2210, '销售删除', 2, NULL, NULL, NULL, 'erp:sale:remove', 3, 1, 1, 'N', NOW(), 0),
(2214, 2210, '销售审核', 2, NULL, NULL, NULL, 'erp:sale:audit', 4, 1, 1, 'N', NOW(), 0),
(2215, 2210, '销售反审核', 2, NULL, NULL, NULL, 'erp:sale:unaudit', 5, 1, 1, 'N', NOW(), 0),
(2216, 2210, '销售导出', 2, NULL, NULL, NULL, 'erp:sale:export', 6, 1, 1, 'N', NOW(), 0),
(2217, 2210, '销售打印', 2, NULL, NULL, NULL, 'erp:sale:print', 7, 1, 1, 'N', NOW(), 0),
(2311, 2310, '进货新增', 2, NULL, NULL, NULL, 'erp:purchase:add', 1, 1, 1, 'N', NOW(), 0),
(2312, 2310, '进货编辑', 2, NULL, NULL, NULL, 'erp:purchase:edit', 2, 1, 1, 'N', NOW(), 0),
(2313, 2310, '进货删除', 2, NULL, NULL, NULL, 'erp:purchase:remove', 3, 1, 1, 'N', NOW(), 0),
(2314, 2310, '进货审核', 2, NULL, NULL, NULL, 'erp:purchase:audit', 4, 1, 1, 'N', NOW(), 0),
(2315, 2310, '进货反审核', 2, NULL, NULL, NULL, 'erp:purchase:unaudit', 5, 1, 1, 'N', NOW(), 0),
(2316, 2310, '进货导出', 2, NULL, NULL, NULL, 'erp:purchase:export', 6, 1, 1, 'N', NOW(), 0),
(2317, 2310, '进货打印', 2, NULL, NULL, NULL, 'erp:purchase:print', 7, 1, 1, 'N', NOW(), 0),
(2511, 2510, '收款新增', 2, NULL, NULL, NULL, 'erp:finance:receipt:add', 1, 1, 1, 'N', NOW(), 0),
(2512, 2510, '收款编辑', 2, NULL, NULL, NULL, 'erp:finance:receipt:edit', 2, 1, 1, 'N', NOW(), 0),
(2513, 2510, '收款删除', 2, NULL, NULL, NULL, 'erp:finance:receipt:remove', 3, 1, 1, 'N', NOW(), 0),
(2514, 2510, '收款审核', 2, NULL, NULL, NULL, 'erp:finance:receipt:audit', 4, 1, 1, 'N', NOW(), 0),
(2515, 2510, '收款反审核', 2, NULL, NULL, NULL, 'erp:finance:receipt:unaudit', 5, 1, 1, 'N', NOW(), 0),
(2521, 2520, '付款新增', 2, NULL, NULL, NULL, 'erp:finance:payment:add', 1, 1, 1, 'N', NOW(), 0),
(2522, 2520, '付款编辑', 2, NULL, NULL, NULL, 'erp:finance:payment:edit', 2, 1, 1, 'N', NOW(), 0),
(2523, 2520, '付款删除', 2, NULL, NULL, NULL, 'erp:finance:payment:remove', 3, 1, 1, 'N', NOW(), 0),
(2524, 2520, '付款审核', 2, NULL, NULL, NULL, 'erp:finance:payment:audit', 4, 1, 1, 'N', NOW(), 0),
(2525, 2520, '付款反审核', 2, NULL, NULL, NULL, 'erp:finance:payment:unaudit', 5, 1, 1, 'N', NOW(), 0),
(2231, 2230, '销售退货新增', 2, NULL, NULL, NULL, 'erp:sale-return:add', 1, 1, 1, 'N', NOW(), 0),
(2232, 2230, '销售退货编辑', 2, NULL, NULL, NULL, 'erp:sale-return:edit', 2, 1, 1, 'N', NOW(), 0),
(2233, 2230, '销售退货删除', 2, NULL, NULL, NULL, 'erp:sale-return:remove', 3, 1, 1, 'N', NOW(), 0),
(2234, 2230, '销售退货审核', 2, NULL, NULL, NULL, 'erp:sale-return:audit', 4, 1, 1, 'N', NOW(), 0),
(2235, 2230, '销售退货反审核', 2, NULL, NULL, NULL, 'erp:sale-return:unaudit', 5, 1, 1, 'N', NOW(), 0),
(2236, 2230, '销售退货导出', 2, NULL, NULL, NULL, 'erp:sale-return:export', 6, 1, 1, 'N', NOW(), 0),
(2237, 2230, '销售退货打印', 2, NULL, NULL, NULL, 'erp:sale-return:print', 7, 1, 1, 'N', NOW(), 0),
(2331, 2330, '进货退货新增', 2, NULL, NULL, NULL, 'erp:purchase-return:add', 1, 1, 1, 'N', NOW(), 0),
(2332, 2330, '进货退货编辑', 2, NULL, NULL, NULL, 'erp:purchase-return:edit', 2, 1, 1, 'N', NOW(), 0),
(2333, 2330, '进货退货删除', 2, NULL, NULL, NULL, 'erp:purchase-return:remove', 3, 1, 1, 'N', NOW(), 0),
(2334, 2330, '进货退货审核', 2, NULL, NULL, NULL, 'erp:purchase-return:audit', 4, 1, 1, 'N', NOW(), 0),
(2335, 2330, '进货退货反审核', 2, NULL, NULL, NULL, 'erp:purchase-return:unaudit', 5, 1, 1, 'N', NOW(), 0),
(2336, 2330, '进货退货导出', 2, NULL, NULL, NULL, 'erp:purchase-return:export', 6, 1, 1, 'N', NOW(), 0),
(2337, 2330, '进货退货打印', 2, NULL, NULL, NULL, 'erp:purchase-return:print', 7, 1, 1, 'N', NOW(), 0),
(2431, 2430, '盘点新增', 2, NULL, NULL, NULL, 'erp:stock-check:add', 1, 1, 1, 'N', NOW(), 0),
(2432, 2430, '盘点编辑', 2, NULL, NULL, NULL, 'erp:stock-check:edit', 2, 1, 1, 'N', NOW(), 0),
(2433, 2430, '盘点删除', 2, NULL, NULL, NULL, 'erp:stock-check:remove', 3, 1, 1, 'N', NOW(), 0),
(2434, 2430, '盘点审核', 2, NULL, NULL, NULL, 'erp:stock-check:audit', 4, 1, 1, 'N', NOW(), 0),
(2435, 2430, '盘点反审核', 2, NULL, NULL, NULL, 'erp:stock-check:unaudit', 5, 1, 1, 'N', NOW(), 0),
(2531, 2530, '其他收入新增', 2, NULL, NULL, NULL, 'erp:finance:income:add', 1, 1, 1, 'N', NOW(), 0),
(2532, 2530, '其他收入编辑', 2, NULL, NULL, NULL, 'erp:finance:income:edit', 2, 1, 1, 'N', NOW(), 0),
(2533, 2530, '其他收入删除', 2, NULL, NULL, NULL, 'erp:finance:income:remove', 3, 1, 1, 'N', NOW(), 0),
(2534, 2530, '其他收入审核', 2, NULL, NULL, NULL, 'erp:finance:income:audit', 4, 1, 1, 'N', NOW(), 0),
(2535, 2530, '其他收入反审核', 2, NULL, NULL, NULL, 'erp:finance:income:unaudit', 5, 1, 1, 'N', NOW(), 0),
(2541, 2540, '其他支出新增', 2, NULL, NULL, NULL, 'erp:finance:expense:add', 1, 1, 1, 'N', NOW(), 0),
(2542, 2540, '其他支出编辑', 2, NULL, NULL, NULL, 'erp:finance:expense:edit', 2, 1, 1, 'N', NOW(), 0),
(2543, 2540, '其他支出删除', 2, NULL, NULL, NULL, 'erp:finance:expense:remove', 3, 1, 1, 'N', NOW(), 0),
(2544, 2540, '其他支出审核', 2, NULL, NULL, NULL, 'erp:finance:expense:audit', 4, 1, 1, 'N', NOW(), 0),
(2545, 2540, '其他支出反审核', 2, NULL, NULL, NULL, 'erp:finance:expense:unaudit', 5, 1, 1, 'N', NOW(), 0);

-- Assign Super Admin Role to All Menus
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu`;

-- Default Dictionaries
INSERT IGNORE INTO `sys_dict_type`
(`dict_id`, `dict_name`, `dict_type`, `status`, `create_time`, `del_flag`)
VALUES
(1, '正常禁用状态', 'sys_normal_disable', 1, NOW(), 0),
(2, '系统是否', 'sys_yes_no', 1, NOW(), 0),
(3, '菜单类型', 'sys_menu_type', 1, NOW(), 0),
(4, '用户性别', 'sys_user_sex', 1, NOW(), 0),
(5, '通用状态', 'sys_common_status', 1, NOW(), 0),
(6, '操作类型', 'sys_oper_type', 1, NOW(), 0),
(7, '文件存储类型', 'sys_file_storage_type', 1, NOW(), 0),
(8, '通知类型', 'sys_notice_type', 1, NOW(), 0),
(9, '通知状态', 'sys_notice_status', 1, NOW(), 0),
(10, '数据权限范围', 'sys_data_scope', 1, NOW(), 0),
(11, '定时任务状态', 'sys_job_status', 1, NOW(), 0),
(12, '定时任务并发策略', 'sys_job_concurrent', 1, NOW(), 0),
(13, '定时任务错过策略', 'sys_job_misfire_policy', 1, NOW(), 0);

INSERT IGNORE INTO `sys_dict_data`
(`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_time`, `del_flag`)
VALUES
(101, 1, '正常', '1', 'sys_normal_disable', 'success', 'Y', 1, NOW(), 0),
(102, 2, '禁用', '0', 'sys_normal_disable', 'danger', 'N', 1, NOW(), 0),
(201, 1, '是', 'Y', 'sys_yes_no', 'success', 'N', 1, NOW(), 0),
(202, 2, '否', 'N', 'sys_yes_no', 'info', 'Y', 1, NOW(), 0),
(301, 1, '目录', '0', 'sys_menu_type', 'primary', 'N', 1, NOW(), 0),
(302, 2, '菜单', '1', 'sys_menu_type', 'success', 'Y', 1, NOW(), 0),
(303, 3, '按钮', '2', 'sys_menu_type', 'warning', 'N', 1, NOW(), 0),
(401, 1, '女', '0', 'sys_user_sex', 'info', 'N', 1, NOW(), 0),
(402, 2, '男', '1', 'sys_user_sex', 'info', 'N', 1, NOW(), 0),
(403, 3, '其他', '2', 'sys_user_sex', 'info', 'Y', 1, NOW(), 0),
(501, 1, '成功', '1', 'sys_common_status', 'success', 'Y', 1, NOW(), 0),
(502, 2, '失败', '0', 'sys_common_status', 'danger', 'N', 1, NOW(), 0),
(601, 1, '其他', '0', 'sys_oper_type', 'info', 'Y', 1, NOW(), 0),
(602, 2, '新增', '1', 'sys_oper_type', 'success', 'N', 1, NOW(), 0),
(603, 3, '修改', '2', 'sys_oper_type', 'warning', 'N', 1, NOW(), 0),
(604, 4, '删除', '3', 'sys_oper_type', 'danger', 'N', 1, NOW(), 0),
(701, 1, '本地', 'LOCAL', 'sys_file_storage_type', 'success', 'Y', 1, NOW(), 0),
(702, 2, 'MinIO', 'MINIO', 'sys_file_storage_type', 'primary', 'N', 1, NOW(), 0),
(703, 3, '阿里云', 'ALIYUN', 'sys_file_storage_type', 'warning', 'N', 1, NOW(), 0),
(704, 4, '腾讯云', 'TENCENT', 'sys_file_storage_type', 'warning', 'N', 1, NOW(), 0),
(705, 5, '七牛云', 'QINIU', 'sys_file_storage_type', 'warning', 'N', 1, NOW(), 0),
(801, 1, '通知', '1', 'sys_notice_type', 'primary', 'Y', 1, NOW(), 0),
(802, 2, '公告', '2', 'sys_notice_type', 'success', 'N', 1, NOW(), 0),
(901, 1, '正常', '1', 'sys_notice_status', 'success', 'Y', 1, NOW(), 0),
(902, 2, '关闭', '0', 'sys_notice_status', 'danger', 'N', 1, NOW(), 0),
(1001, 1, '全部数据权限', '1', 'sys_data_scope', 'primary', 'Y', 1, NOW(), 0),
(1002, 2, '自定数据权限', '2', 'sys_data_scope', 'warning', 'N', 1, NOW(), 0),
(1003, 3, '本部门数据权限', '3', 'sys_data_scope', 'success', 'N', 1, NOW(), 0),
(1004, 4, '本部门及以下数据权限', '4', 'sys_data_scope', 'success', 'N', 1, NOW(), 0),
(1005, 5, '仅本人数据权限', '5', 'sys_data_scope', 'info', 'N', 1, NOW(), 0),
(1101, 1, '运行中', '1', 'sys_job_status', 'success', 'Y', 1, NOW(), 0),
(1102, 2, '已暂停', '0', 'sys_job_status', 'info', 'N', 1, NOW(), 0),
(1201, 1, '允许并发', 'Y', 'sys_job_concurrent', 'success', 'N', 1, NOW(), 0),
(1202, 2, '禁止并发', 'N', 'sys_job_concurrent', 'warning', 'Y', 1, NOW(), 0),
(1301, 1, '忽略错过执行', 'IGNORE', 'sys_job_misfire_policy', 'info', 'N', 1, NOW(), 0),
(1302, 2, '立即执行一次', 'FIRE_ONCE', 'sys_job_misfire_policy', 'warning', 'N', 1, NOW(), 0),
(1303, 3, '错过不补偿', 'DO_NOTHING', 'sys_job_misfire_policy', 'success', 'Y', 1, NOW(), 0);

-- Default Config
INSERT IGNORE INTO `sys_config`
(`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_time`, `del_flag`)
VALUES
(1, '系统名称', 'sys.name', 'LingNow ERP', 'Y', '系统显示名称', NOW(), 0),
(20, 'ERP允许负库存', 'erp.allowNegativeStock', 'N', 'Y', 'Y允许，N不允许；当前库存审核按此参数控制', NOW(), 0),
(21, 'ERP审核后只读', 'erp.auditReadonly', 'Y', 'Y', 'Y表示已审核单据必须反审核后才能修改或删除', NOW(), 0),
(22, 'ERP数量精度', 'erp.qtyPrecision', '2', 'Y', 'ERP数量显示和录入精度', NOW(), 0),
(23, 'ERP金额精度', 'erp.amountPrecision', '2', 'Y', 'ERP金额显示和录入精度', NOW(), 0);
