# LingNow 匹配服务系统

## 项目介绍

LingNow 是一个高质量、高扩展性的匹配服务系统，采用**多模块单体架构**设计，虽然是单体应用，但通过合理的模块划分为未来微服务化预留了空间。

系统支持**两个独立部署的应用**：

- **lingnow-app** (用户端): 提供面向C端用户的API，端口 `6061`
- **lingnow-admin** (管理端): 提供后台管理API，端口 `6060`

## 技术栈

### 后端核心
- **框架**: Spring Boot 3.2.1
- **Java版本**: JDK 17
- **构建工具**: Maven 3.8+
- **ORM**: MyBatis-Plus 3.5.5
- **数据库**: MySQL 8.0
- **缓存**: Redis 7.x (Redisson)
- **安全**: Sa-Token 1.37.0 (轻量级权限认证)
- **API文档**: Knife4j 4.4.0
- **工具库**: Lombok, Hutool, Apache Commons

### 架构特点
1. **多模块设计**: common → core → app/admin 的依赖关系
2. **两个独立JAR**: 用户端和管理端可独立部署
3. **代码复用**: 核心业务逻辑通过 `lingnow-core` 模块共享
4. **高扩展性**: 应用策略模式、工厂模式等设计模式

## 项目结构

```
lingnow/backend/
├── pom.xml                    # 父项目POM
├── lingnow-common/            # 通用模块 (工具类、配置、异常处理)
├── lingnow-core/              # 核心业务模块 (用户、匹配、消息)
├── lingnow-app/               # 用户端应用 (端口: 6061)
└── lingnow-admin/             # 管理端应用 (端口: 6060)
```

### 模块说明

#### lingnow-common
提供基础设施支持：
- `BaseEntity`: 基础实体类
- `Result/PageResult`: 统一返回结果封装
- `GlobalExceptionHandler`: 全局异常处理
- `RedisUtil`: Redis工具类
- `MyBatisPlusConfig`: MyBatis-Plus配置

#### lingnow-core
核心业务逻辑：

- `sysUser`: 用户管理模块
- `match`: 匹配引擎模块（待实现）
- `message`: 消息通信模块（待实现）
- `recommend`: 推荐系统模块（待实现）

#### lingnow-app
用户端应用：
- `AuthController`: 用户注册、登录、登出
- `UserController`: 用户信息管理

#### lingnow-admin
管理端应用：
- `AdminUserController`: C端用户管理

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.x

### 数据库初始化

1. 创建数据库并导入完整表结构及核心数据：
```bash
mysql -u root -p lingnow_base < sql/init.sql
```

初始化脚本会创建基座管理员账号，用于首次登录后继续配置系统资料。

### 启动 Redis
```bash
redis-server
```

### 编译项目
```bash
cd backend
mvn clean compile
```

### 启动用户端应用
```bash
cd lingnow-app
mvn spring-boot:run
```

访问：
- API文档: http://localhost:6061/doc.html

### 启动管理端应用（新终端）
```bash
cd lingnow-admin
mvn spring-boot:run
```

访问：
- API文档: http://localhost:6060/doc.html

## 开发指南

### 配置文件
- 开发环境: `application-dev.yml` (默认激活)
- 生产环境: `application-prod.yml`

修改配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lingnow_base
    username: root
    password: your_password
    
  data:
    redis:
      host: localhost
      port: 6379
```

### API 调试

登录成功后返回结果包含 `token`，后续请求需携带 token：

#### 获取用户信息（需要登录）
```bash
curl http://localhost:6061/sysUser/info \
  -H "satoken: your_token_here"
```

## 打包部署

### 打包
```bash
# 打包所有模块
mvn clean package -DskipTests

# 打包后的JAR文件位置：
# lingnow-app/target/lingnow-app.jar
# lingnow-admin/target/lingnow-admin.jar
```

### 运行JAR
```bash
# 启动用户端
java -jar lingnow-app/target/lingnow-app.jar

# 启动管理端
java -jar lingnow-admin/target/lingnow-admin.jar
```

## 后续开发

当前框架已完成基础架构和用户模块，后续需要实现：

1. **匹配引擎模块** - 实现匹配算法和队列管理
2. **消息通信模块** - WebSocket实时消息功能
3. **推荐系统模块** - 用户推荐算法
4. **管理端功能** - 系统管理、权限控制(RBAC)

## License

MIT License
