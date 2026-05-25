# Smart Agriculture

> 面向智慧农业场景的智能问答 Agent 系统后端，支持农业知识问答、种植档案管理、多轮对话、长期记忆、RAG 检索增强、流式输出和用户管理等能力。

`Smart Agriculture` 是一个面向农业生产服务场景的大模型应用项目，系统以“农业智能问答 + 种植档案 + 知识增强 + 个性化建议”为核心目标，帮助农户围绕作物种植、病虫害防治、施肥灌溉、农事管理等问题获得更及时、专业和可解释的智能建议。

本项目目前主要提供后端能力，适合作为智慧农业 AI Agent、大模型应用开发、RAG 检索增强问答、Spring Boot 后端工程化和农业智能服务系统的实践项目。

---

## 目录

* [项目简介](#项目简介)
* [项目亮点](#项目亮点)
* [功能模块](#功能模块)
* [技术栈](#技术栈)
* [系统架构](#系统架构)
* [项目结构](#项目结构)
* [环境要求](#环境要求)
* [快速开始](#快速开始)
* [配置说明](#配置说明)
* [接口说明](#接口说明)
* [核心业务流程](#核心业务流程)
* [安全建议](#安全建议)
* [后续优化方向](#后续优化方向)
* [项目说明](#项目说明)
* [许可证](#许可证)

---

## 项目简介

在农业生产过程中，农户常常面临病虫害识别不及时、农技指导不精准、种植数据难以沉淀、历史管理记录难以复用等问题。`Smart Agriculture` 尝试将大语言模型、RAG 知识库、长期记忆和智能体机制引入农业问答场景，构建一个能够持续理解用户种植背景并给出个性化建议的农业智能问答系统。

系统面向以下典型场景：

* 农户咨询作物病虫害防治方法；
* 农户询问施肥、灌溉、用药、田间管理建议；
* 系统根据用户作物品种、地块信息、施肥记录和病害历史生成更有针对性的回答；
* 系统结合农业知识库检索结果，生成更专业、可解释的农业治理建议；
* 用户通过连续对话方式完成农业问题咨询和农事记录管理。

---

## 项目亮点

* **农业场景定制**：围绕作物种植、病虫害防治、施肥灌溉和种植档案等农业生产问题设计。
* **RAG 检索增强**：结合农业知识库进行知识检索，降低大模型幻觉，提高回答专业性和可解释性。
* **长期记忆机制**：记录用户作物品种、地块信息、施肥记录、病害历史等种植档案，支持个性化问答。
* **多轮对话能力**：支持上下文连续理解，使系统能够围绕同一农业问题持续追问和补充建议。
* **流式输出体验**：支持 SSE 流式返回，前端可实现类似 ChatGPT 的实时回复效果。
* **智能体扩展能力**：可扩展不同农业角色，如病虫害专家、施肥顾问、种植管理助手等。
* **工程化后端实现**：基于 Spring Boot 构建，包含用户认证、业务接口、数据库访问、文件管理等基础后端能力。

---

## 功能模块

### 1. 用户认证与用户管理

* 用户注册；
* 用户登录；
* JWT Token 鉴权；
* 用户基础信息查询；
* 用户资料修改；
* 用户状态管理；
* 管理端用户管理能力。

### 2. 农业智能问答

* 支持普通问答；
* 支持流式问答；
* 支持多轮上下文对话；
* 支持农业场景提示词约束；
* 支持面向作物种植、病虫害防治、施肥灌溉等问题的智能回答。

### 3. 农业智能体管理

* 创建智能体；
* 查询智能体；
* 更新智能体信息；
* 支持为不同农业任务配置不同角色设定；
* 可扩展病虫害防治专家、作物种植顾问、施肥灌溉助手等专业 Agent。

### 4. 对话管理

* 创建对话；
* 查询对话历史；
* 分页查询对话记录；
* 删除对话；
* 清除上下文；
* 对话置顶或取消置顶；
* 查询用户对话数量。

### 5. 长期记忆与种植档案

系统可以将用户相关信息沉淀为长期记忆，例如：

* 作物品种；
* 地块信息；
* 播种 / 移栽时间；
* 施肥记录；
* 病害历史；
* 用药记录；
* 农事管理习惯。

长期记忆可用于增强后续问答的针对性，让系统从“一次性问答”升级为“持续陪伴式农业助手”。

### 6. RAG 知识增强

系统可接入农业知识库，知识内容可以包括：

* 作物种植技术；
* 病虫害防治方案；
* 农药使用规范；
* 施肥灌溉建议；
* 常见农业问答；
* 农业政策或农技资料。

RAG 基本流程：

```text
用户问题
  |
  v
问题向量化 / 关键词理解
  |
  v
农业知识库检索
  |
  v
召回相关知识片段
  |
  v
大模型结合知识片段生成回答
```

### 7. 语音交互能力

项目可扩展语音识别和语音合成能力，便于农户在田间通过语音方式进行咨询。

* 语音转文本；
* 文本转语音；
* WebSocket 实时通信；
* 移动端或小程序语音交互扩展。

### 8. 后台管理能力

* 管理员登录；
* 管理员注册；
* 用户状态管理；
* 文件上传与读取；
* Word 文档管理；
* 后续可扩展知识库文件管理和农业资料维护。

---

## 技术栈

| 分类    | 技术                                     |
| ----- | -------------------------------------- |
| 开发语言  | Java                                   |
| 后端框架  | Spring Boot                            |
| 安全认证  | Spring Security、JWT                    |
| 数据库   | MySQL                                  |
| 持久层   | MyBatis                                |
| 大模型能力 | DashScope / 阿里云百炼 / 通义千问，可替换为其他大模型 API |
| 实时输出  | SSE 流式响应                               |
| 实时通信  | WebSocket                              |
| 文件处理  | Apache POI、文件上传管理                      |
| 对象存储  | 阿里云 OSS，可选                             |
| 接口文档  | Knife4j / OpenAPI，可选                   |
| 构建工具  | Maven / Maven Wrapper                  |

---

## 系统架构

```text
用户 / 微信小程序 / Web 前端
        |
        | HTTP / SSE / WebSocket
        v
Spring Boot 后端服务
        |
        |-- 用户认证与权限控制
        |-- 农业智能体管理
        |-- 智能问答与流式输出
        |-- 对话历史管理
        |-- 长期记忆与种植档案
        |-- RAG 农业知识库检索
        |-- 文件与后台管理
        |
        v
业务服务层 Service
        |
        |-- 大模型调用模块
        |-- 长期记忆模块
        |-- 知识检索模块
        |-- 用户与对话管理模块
        |
        v
MyBatis Mapper
        |
        v
MySQL 数据库

外部能力：

阿里云 DashScope / 百炼大模型
阿里云 OSS 文件存储
农业知识库 / 向量数据库
WebSocket 语音服务
```

---

## 项目结构

当前仓库采用 Spring Boot 标准目录结构，主要代码位于 `src/main/java` 和 `src/main/resources` 下。

```text
smart-agriculture/
├── .mvn/                         # Maven Wrapper 配置
├── src/
│   ├── main/
│   │   ├── java/com/soultalk/
│   │   │   ├── aigc/              # 大模型、主模型、语音等 AI 能力封装
│   │   │   ├── aop/               # AOP 切面
│   │   │   ├── config/            # Spring Security、WebSocket、Web 配置
│   │   │   ├── context/           # 当前用户上下文
│   │   │   ├── controller/        # 控制器接口层
│   │   │   │   ├── request/       # 请求 / 响应对象
│   │   │   │   └── websocket/     # WebSocket 语音接口
│   │   │   ├── filter/            # JWT 认证过滤器
│   │   │   ├── handle/            # 统一异常处理或响应处理
│   │   │   ├── mapper/            # MyBatis Mapper 接口
│   │   │   ├── po/                # 持久化对象 / 数据对象
│   │   │   ├── properties/        # 配置属性类
│   │   │   ├── service/           # 业务服务接口与实现
│   │   │   ├── utils/             # 工具类
│   │   │   └── SoulTalkApplication.java
│   │   └── resources/
│   │       ├── com/soultalk/mapper/   # MyBatis XML 映射文件
│   │       ├── demo/                  # 示例资源
│   │       ├── application.yml
│   │       └── application.properties
│   └── test/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── LICENSE
└── README.md
```

> 说明：当前项目代码包名仍保留为 `com.soultalk`，后续可以根据项目定位统一重构为 `com.smartagriculture` 或 `com.agriagent`，以便与仓库名称和项目主题保持一致。

---

## 环境要求

请先准备：

* JDK 17 或更高版本；
* Maven 3.8+；
* MySQL 8.x 或兼容版本；
* 大模型 API Key，例如 DashScope / 阿里云百炼；
* 阿里云 OSS 配置，可选；
* 支持 SSE / WebSocket 的前端或接口测试工具，可选。

---

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/JasperChenJH/smart-agriculture.git
cd smart-agriculture
```

### 2. 创建数据库

```sql
CREATE DATABASE smart_agriculture DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

如果项目后续提供 SQL 初始化脚本，可按如下方式导入：

```bash
mysql -u root -p smart_agriculture < smart_agriculture.sql
```

### 3. 修改配置文件

建议使用环境变量管理数据库密码、API Key、OSS 密钥等敏感配置。

`application.yml` 示例：

```yaml
spring:
  application:
    name: smart-agriculture
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:smart_agriculture}?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  configuration:
    map-underscore-to-camel-case: true

agriculture:
  llm:
    api-key: ${DASHSCOPE_API_KEY:}
    workspace-id: ${DASHSCOPE_WORKSPACE_ID:}
  oss:
    endpoint: ${ALIYUN_OSS_ENDPOINT:}
    access-key-id: ${ALIYUN_OSS_ACCESS_KEY_ID:}
    access-key-secret: ${ALIYUN_OSS_ACCESS_KEY_SECRET:}
    bucket-name: ${ALIYUN_OSS_BUCKET_NAME:}
```

### 4. 配置环境变量

Linux / macOS：

```bash
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=smart_agriculture
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=你的数据库密码

export DASHSCOPE_API_KEY=你的DashScopeApiKey
export DASHSCOPE_WORKSPACE_ID=你的WorkspaceId

export ALIYUN_OSS_ENDPOINT=你的OSS地域节点
export ALIYUN_OSS_ACCESS_KEY_ID=你的AccessKeyId
export ALIYUN_OSS_ACCESS_KEY_SECRET=你的AccessKeySecret
export ALIYUN_OSS_BUCKET_NAME=你的Bucket名称
```

Windows PowerShell：

```powershell
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="smart_agriculture"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="你的数据库密码"

$env:DASHSCOPE_API_KEY="你的DashScopeApiKey"
$env:DASHSCOPE_WORKSPACE_ID="你的WorkspaceId"

$env:ALIYUN_OSS_ENDPOINT="你的OSS地域节点"
$env:ALIYUN_OSS_ACCESS_KEY_ID="你的AccessKeyId"
$env:ALIYUN_OSS_ACCESS_KEY_SECRET="你的AccessKeySecret"
$env:ALIYUN_OSS_BUCKET_NAME="你的Bucket名称"
```

### 5. 编译项目

使用 Maven：

```bash
mvn clean package
```

或使用 Maven Wrapper：

```bash
./mvnw clean package
```

Windows：

```powershell
.\mvnw.cmd clean package
```

### 6. 启动项目

```bash
mvn spring-boot:run
```

或运行打包后的 jar：

```bash
java -jar target/*.jar
```

默认访问地址：

```text
http://localhost:8080
```

接口文档可尝试访问：

```text
http://localhost:8080/doc.html
```

---

## 配置说明

### 数据库配置

MySQL 用于存储用户、智能体、对话、长期记忆索引、农业档案、后台管理等业务数据。

### 大模型配置

大模型用于生成农业问答结果，后续可根据需要切换为：

* 通义千问；
* DeepSeek；
* OpenAI 兼容接口；
* 智谱 GLM；
* 本地 Ollama 模型。

### RAG 配置

RAG 知识库可以接入：

* 阿里云百炼知识库；
* Elasticsearch；
* Milvus；
* Pinecone；
* MySQL 全文检索；
* 本地向量数据库。

推荐知识库内容：

```text
作物种植知识
病虫害防治手册
农药使用规范
施肥灌溉技术
农业常见问答
农技推广资料
```

---

## 接口说明

以下接口名称按当前项目模块和常见业务设计整理，具体参数以源码 Controller 为准。

### 1. 认证接口

| 请求方式 | 路径                    | 说明   |
| ---- | --------------------- | ---- |
| POST | `/auth/login`         | 用户登录 |
| POST | `/auth/register`      | 用户注册 |
| POST | `/auth/resetPassword` | 重置密码 |

### 2. 用户接口

| 请求方式 | 路径                      | 说明       |
| ---- | ----------------------- | -------- |
| GET  | `/user/info`            | 获取用户信息   |
| POST | `/user/updatePhoto`     | 修改头像     |
| POST | `/user/updateIntroduce` | 修改个人简介   |
| POST | `/user/updatePassword`  | 修改密码     |
| GET  | `/user/detail/info`     | 获取用户详细信息 |
| POST | `/user/detail/update`   | 修改用户详细信息 |

### 3. 智能体接口

| 请求方式 | 路径                     | 说明         |
| ---- | ---------------------- | ---------- |
| POST | `/agent/create`        | 创建农业智能体    |
| GET  | `/agent/select/all`    | 查询可见智能体    |
| GET  | `/agent/select/info`   | 查询智能体详情    |
| GET  | `/agent/select/like`   | 模糊搜索智能体    |
| POST | `/agent/update`        | 更新智能体      |
| GET  | `/agent/select/myself` | 查询个人创建的智能体 |

### 4. 对话接口

| 请求方式 | 路径                    | 说明        |
| ---- | --------------------- | --------- |
| POST | `/dia/create`         | 创建对话      |
| GET  | `/dia/getDia`         | 获取指定对话    |
| GET  | `/dia/getRangeDia`    | 分页查询对话    |
| GET  | `/dia/countDia`       | 查询对话数量    |
| POST | `/dia/question`       | 普通问答      |
| POST | `/dia/streamQuestion` | 流式问答      |
| POST | `/dia/remove/content` | 清除上下文     |
| POST | `/dia/remove/all`     | 删除对话      |
| POST | `/dia/update/level`   | 置顶 / 取消置顶 |

### 5. 主模型与长期记忆接口

| 请求方式 | 路径                     | 说明                |
| ---- | ---------------------- | ----------------- |
| POST | `/main/create`         | 创建主模型对话           |
| GET  | `/main/get`            | 获取主模型对话详情         |
| GET  | `/main/getAll`         | 获取全部主模型对话         |
| POST | `/main/ask`            | 主模型问答，支持 SSE 流式输出 |
| GET  | `/main/clear`          | 清除上下文             |
| GET  | `/main/clearMemory`    | 重置长期记忆            |
| GET  | `/main/refreshInfo`    | 同步用户信息到长期记忆       |
| GET  | `/main/getMemoryNodes` | 获取长期记忆片段          |

---

## 核心业务流程

### 农业问答流程

```text
用户提出农业问题
        |
        v
识别问题类型与上下文
        |
        v
读取用户种植档案与长期记忆
        |
        v
检索农业知识库
        |
        v
构造 Prompt
        |
        v
调用大模型生成回答
        |
        v
返回普通文本或 SSE 流式结果
```

### 种植档案增强问答流程

```text
用户补充作物品种、地块、施肥、病害历史等信息
        |
        v
系统同步到长期记忆
        |
        v
用户再次咨询病虫害或管理问题
        |
        v
模型结合长期记忆生成个性化建议
```

### RAG 知识增强流程

```text
用户问题
        |
        v
知识库检索
        |
        v
召回农业知识片段
        |
        v
大模型结合知识片段回答
        |
        v
输出可解释农业建议
```

---

## 安全建议

公开仓库中不要提交以下敏感信息：

* 数据库 IP、账号、密码；
* 大模型 API Key；
* 阿里云 AccessKey；
* OSS Bucket 私密配置；
* JWT 密钥；
* 生产环境服务器地址。

建议：

1. 将真实配置改为环境变量读取；
2. 将 `application-local.yml`、`.env` 等本地配置加入 `.gitignore`；
3. 提供 `application-example.yml` 作为示例配置；
4. 如果密钥已经提交到公开仓库，应立即到对应平台重置；
5. 生产环境限制 CORS 域名，不建议使用 `*`；
6. JWT 密钥使用复杂随机字符串并定期轮换。

推荐 `.gitignore`：

```gitignore
# local config
application-local.yml
application-dev.yml
application-secret.yml
.env
*.env

# build output
target/

# IDE
.idea/
.vscode/
*.iml

# logs
logs/
*.log

# uploaded files
upload/
uploads/
files/
```

---

## 后续优化方向

* [ ] 将项目包名由 `com.soultalk` 重构为农业项目相关命名；
* [ ] 补充数据库初始化 SQL；
* [ ] 补充接口请求示例；
* [ ] 完善农业知识库导入流程；
* [ ] 接入向量数据库，实现完整 RAG 检索；
* [ ] 增加作物病害图片识别接口；
* [ ] 接入 CNN / ResNet 病害识别模型；
* [ ] 增加种植档案可视化管理；
* [ ] 增加农事提醒与定时任务；
* [ ] 增加语音输入和语音播报；
* [ ] 增加微信小程序端运行说明；
* [ ] 增加 Docker Compose，一键启动 MySQL 与后端服务；
* [ ] 增加单元测试和接口测试。

---

## 项目说明

当前仓库已经具备 Spring Boot 后端项目基础结构，并包含 AI 能力封装、用户认证、智能体、对话、主模型、长期记忆、WebSocket 和后台管理等模块。后续重点可以围绕农业业务语义进一步改造，例如统一包名、接口命名、数据库表名和 Prompt 模板，使系统从通用智能对话项目进一步沉淀为面向智慧农业的智能 Agent 系统。

---

## 许可证

本项目基于 AGPL-3.0 License 开源，详情请查看 [LICENSE](./LICENSE)。
