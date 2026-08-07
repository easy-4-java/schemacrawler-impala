# schemacrawler-impala

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-17-orange)](https://github.com/easy-4-java/schemacrawler-impala) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

schemacrawler-impala 是 SchemaCrawler 的数据库插件，将 Apache Impala 注册为受支持的数据库类型。

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 功能与状态](#2-功能与状态)
- [3. 环境要求与兼容性](#3-环境要求与兼容性)
- [4. 架构与模块](#4-架构与模块)
- [5. 安装](#5-安装)
- [6. 快速开始](#6-快速开始)
- [7. 配置](#7-配置)
- [8. 核心用法 / API](#8-核心用法--api)
- [9. 测试与构建](#9-测试与构建)
- [10. 版本线与分支](#10-版本线与分支)
- [11. 贡献与许可](#11-贡献与许可)

## 1. 项目概述

**schemacrawler-impala** 是 [SchemaCrawler](https://www.schemacrawler.com) 的数据库插件，将 **Apache Impala** 注册为受支持的数据库类型。它通过 `META-INF/services/schemacrawler.tools.databaseconnector.DatabaseConnector` 服务加载机制被发现。

| 是                                                           | 不是                                    |
| :----------------------------------------------------------- | :-------------------------------------- |
| 面向 Impala 的 SchemaCrawler `DatabaseConnector` 插件        | Impala 客户端 SDK                       |
| 加载 Cloudera Impala JDBC 驱动（`com.cloudera.impala.jdbc41.Driver`） | Impala Shell / Hue 的替代品      |
| 默认采用 `jdbc:hive2://` URL 风格（Impala JDBC 惯例）        | 完整的 Impala 元数据仓库                |

典型场景：

| 场景                 | 说明                                                    |
| :------------------- | :------------------------------------------------------ |
| Impala 元数据探查    | 使用 SchemaCrawler 命令操作 Impala daemon               |
| 元数据导出           | 为 Impala 表生成模式图/文本报告                         |
| CI 文档同步          | 让 Impala 模式文档与数仓保持一致                        |

## 2. 功能与状态

| 能力                                            | 状态       | 说明                                                                          |
| :---------------------------------------------- | :--------- | :---------------------------------------------------------------------------- |
| 数据库类型注册（`impala`）                      | 已实现     | `ImpalaDatabaseConnector`；SchemaCrawler 中显示为 "Apache Impala"             |
| JDBC URL 识别                                   | 已实现     | `supportsUrlPredicate()` 匹配 `jdbc:hive2:.*`（Impala JDBC 惯例）             |
| 驱动加载                                        | 已实现     | 连接器构造时加载 `com.cloudera.impala.jdbc41.Driver`                          |
| 连接默认值                                      | 已实现     | `schemacrawler-impala.config.properties`：host/port/database/url             |
| 帮助命令                                        | 已实现     | `--server=impala` 帮助文本，含 host/port/database 选项                        |
| information_schema SQL 视图                     | 空         | `impala.information_schema/` 目录存在，但尚无 SQL 文件                        |
| 单元测试                                        | 部分       | `TestBundledDistributions` 断言注册表包含 `impala`；`ImpalaTest` 为需真实 Impala 的集成测试 |

已知缺口：

| 缺口                                                                      | 影响                                                    |
| :------------------------------------------------------------------------ | :------------------------------------------------------ |
| Cloudera Impala JDBC 驱动**不是** Maven 依赖                               | 应用必须自行提供驱动 jar 到 classpath，否则 `Class.forName` 失败 |
| `impala.information_schema/` 为空                                          | SchemaCrawler 回退到基于 `DatabaseMetaData` 的探查      |
| 帮助文本仍写着 `--server=hive2`（`getHelpCommand()` 中的复制残留）          | 仅为文案问题；实际应使用 `--server=impala`              |

## 3. 环境要求与兼容性

| 要求          | 版本              |
| :------------ | :---------------- |
| JDK           | 8+                |
| Maven         | 3.0+（已内置 wrapper） |
| SchemaCrawler | 16.7.2            |
| Impala JDBC 驱动 | 未内置（如 Cloudera Impala JDBC 2.x/4.1） |

easy4j 项目的版本线：

| 分支           | JDK  | 版本模式   | 说明                            |
| :------------- | :--- | :--------- | :------------------------------ |
| `feature/1.0.x` | 8    | `1.0.x.*`  | 本文档对应分支                   |
| `feature/2.0.x` | 17   | `2.0.x.*`  | JDK 17 版本线                   |
| `feature/3.0.x` | 21   | `3.0.x.*`  | JDK 21 版本线                   |

## 4. 架构与模块

```text
  SchemaCrawler (16.7.2)
        |
        v
  ServiceLoader -> ImpalaDatabaseConnector
        |
        v
  supportsUrlPredicate: jdbc:hive2:*
        |
        v
  Impala JDBC 驱动
  (com.cloudera.impala.jdbc41.Driver，由应用提供)
        |
        v
  /impala.information_schema (空) + 配置属性
```

单模块 Maven 项目（`jar` 打包）：

| 包                                  | 职责                                          |
| :---------------------------------- | :-------------------------------------------- |
| `schemacrawler.server.impala`       | `ImpalaDatabaseConnector`（插件入口）         |
| `META-INF/services`                 | SchemaCrawler 服务注册                        |
| `resources/impala.information_schema` | 元数据 SQL 视图（当前为空）                |
| `resources/schemacrawler-impala.config.properties` | 连接默认值                    |

## 5. 安装

制品发布在阿里云私服与 GitHub Releases，**尚未发布到 Maven Central**。

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>schemacrawler-impala</artifactId>
    <version>2.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

```groovy
implementation 'io.github.easy4j:schemacrawler-impala:2.0.x.x.20260630-SNAPSHOT'
```

请同时将 Impala JDBC 驱动 jar（例如从 Cloudera 获取）加入 classpath——该驱动有意不内置。

## 6. 快速开始

```bash
# 列出 "default" 库中的表
./sc.sh -server=impala -database=default -host impala-host -port 21050 \
        -infolevel=standard -command=list
```

或编程方式：

```java
DatabaseConnectorRegistry registry = DatabaseConnectorRegistry.getDatabaseConnectorRegistry();
System.out.println(registry.hasDatabaseSystemIdentifier("impala")); // true

Connection connection = DriverManager.getConnection(
        "jdbc:hive2://impala-host:21050/default;auth=noSasl", "", "");
```

预期结果：`hasDatabaseSystemIdentifier("impala")` 返回 `true`，SchemaCrawler 通过 Impala JDBC 驱动完成连接（`;auth=noSasl` 风格为集成测试展示的 Impala JDBC 惯例）。

## 7. 配置

默认值位于 `schemacrawler-impala.config.properties`：

| 属性       | 默认值     | 说明                  |
| :--------- | :--------- | :-------------------- |
| `host`     | `localhost`| Impala daemon 主机     |
| `port`     | `10000`    | Impala JDBC 端口      |
| `database` | `default`  | 数据库名               |
| `url`      | `jdbc:hive2://${host}:${port}/${database}` | JDBC URL 模板 |

命令行选项（`ImpalaDatabaseConnector.getHelpCommand()`）：`-host`、`-port`、`-database`、`-user`、`-password`。

## 8. 核心用法 / API

| 类型                                                 | 职责                                                             |
| :--------------------------------------------------- | :--------------------------------------------------------------- |
| `schemacrawler.server.impala.ImpalaDatabaseConnector`| 注册服务类型 `impala`、加载 Impala JDBC 驱动、提供帮助与 URL 匹配 |

## 9. 测试与构建

```bash
./mvnw clean verify
```

- `TestBundledDistributions` 验证插件已注册（`registry.hasDatabaseSystemIdentifier("impala")`）。
- `ImpalaTest`（位于 `src/test`）是集成测试，需要可用的 Impala daemon；不属于默认单元测试范围。
- 已配置 JaCoCo 行覆盖率 90% 门禁（`haltOnFailure=false`）。

## 10. 版本线与分支

| 分支           | JDK  | 版本模式   | 维护说明                          |
| :------------- | :--- | :--------- | :-------------------------------- |
| `feature/1.0.x` | 8    | `1.0.x.*`  | 当前分支                          |
| `feature/2.0.x` | 17   | `2.0.x.*`  | JDK 17 版本线                     |
| `feature/3.0.x` | 21   | `3.0.x.*`  | JDK 21 版本线                     |

制品通过阿里云 Maven 私服与 GitHub Releases 分发。请按 JDK 基线选择对应分支。

## 11. 贡献与许可

欢迎贡献——尤其是补齐 `impala.information_schema` SQL 视图与修正帮助文本。较大改动请先提交 issue 讨论。

本项目基于 [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) 许可。
