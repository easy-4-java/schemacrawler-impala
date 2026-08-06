# schemacrawler-impala

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-21-orange)](https://github.com/easy-4-java/schemacrawler-impala) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Features & Status](#2-features--status)
- [3. Requirements & Compatibility](#3-requirements--compatibility)
- [4. Architecture & Modules](#4-architecture--modules)
- [5. Installation](#5-installation)
- [6. Quick Start](#6-quick-start)
- [7. Configuration](#7-configuration)
- [8. Core Usage / API](#8-core-usage--api)
- [9. Testing & Build](#9-testing--build)
- [10. Versioning & Branches](#10-versioning--branches)
- [11. Contributing & License](#11-contributing--license)

## 1. Project Overview

**schemacrawler-impala** is a [SchemaCrawler](https://www.schemacrawler.com) database plug-in that registers
**Apache Impala** as a supported database type. It is discovered through the
`META-INF/services/schemacrawler.tools.databaseconnector.DatabaseConnector` service loader entry.

| Is                                                           | Is not                                    |
| :----------------------------------------------------------- | :---------------------------------------- |
| A SchemaCrawler `DatabaseConnector` plug-in for Impala       | An Impala client SDK                      |
| Loads the Cloudera Impala JDBC driver (`com.cloudera.impala.jdbc41.Driver`) | A replacement for Impala Shell / Hue |
| Defaults to `jdbc:hive2://` URL style (Impala JDBC convention) | A full Impala metadata repository         |

Typical scenarios:

| Scenario                         | Description                                             |
| :------------------------------- | :------------------------------------------------------ |
| Impala schema introspection      | SchemaCrawler commands against an Impala daemon         |
| Metadata export                  | Schema diagrams/text reports for Impala tables          |
| CI documentation                 | Keep Impala schema docs in sync with the warehouse      |

## 2. Features & Status

| Capability                                      | Status      | Notes                                                                        |
| :---------------------------------------------- | :---------- | :--------------------------------------------------------------------------- |
| Database server type registration (`impala`)    | Implemented | `ImpalaDatabaseConnector`; SchemaCrawler reports it as "Apache Impala"       |
| JDBC URL recognition                            | Implemented | `supportsUrlPredicate()` matches `jdbc:hive2:.*` (Impala JDBC convention)    |
| Driver loading                                  | Implemented | Loads `com.cloudera.impala.jdbc41.Driver` at connector construction          |
| Connection defaults                             | Implemented | `schemacrawler-impala.config.properties`: host/port/database/url            |
| Help command                                    | Implemented | `--server=impala` help text with host/port/database options                  |
| Information-schema SQL views                    | Empty       | `impala.information_schema/` folder exists but contains no SQL files yet     |
| Unit tests                                      | Partial     | `TestBundledDistributions` asserts the registry knows `impala`; `ImpalaTest` is an integration test needing a live Impala |

Known gaps:

| Gap                                                                     | Impact                                                    |
| :---------------------------------------------------------------------- | :-------------------------------------------------------- |
| The Cloudera Impala JDBC driver is **not** a Maven dependency           | Applications must provide the driver jar on the classpath, otherwise `Class.forName` fails |
| `impala.information_schema/` is empty                                   | SchemaCrawler falls back to `DatabaseMetaData`-based introspection |
| The help text still says `--server=hive2` (copy-paste in `getHelpCommand()`) | Cosmetic; use `--server=impala`                       |

## 3. Requirements & Compatibility

| Requirement | Version            |
| :---------- | :----------------- |
| JDK         | 8+                 |
| Maven       | 3.0+ (wrapper included) |
| SchemaCrawler | 16.7.2           |
| Impala JDBC driver | Not bundled (e.g. Cloudera Impala JDBC 2.x/4.1) |

Version lines of the easy4j project:

| Branch        | JDK  | Version pattern | Notes                       |
| :------------ | :--- | :-------------- | :-------------------------- |
| `feature/1.0.x` | 8    | `1.0.x.*`       | This README, current branch |
| `feature/2.0.x` | 17   | `2.0.x.*`       | JDK 17 line                 |
| `feature/3.0.x` | 21   | `3.0.x.*`       | JDK 21 line                 |

## 4. Architecture & Modules

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
  Impala JDBC driver (com.cloudera.impala.jdbc41.Driver, provided by app)
        |
        v
  /impala.information_schema (empty) + config properties
```

Single-module Maven project (`jar` packaging):

| Package                          | Responsibility                                  |
| :------------------------------- | :---------------------------------------------- |
| `schemacrawler.server.impala`    | `ImpalaDatabaseConnector` (the plug-in entry)   |
| `META-INF/services`              | Service registration for SchemaCrawler          |
| `resources/impala.information_schema` | Metadata SQL views (currently empty)        |
| `resources/schemacrawler-impala.config.properties` | Connection defaults       |

## 5. Installation

Artifacts are published to the aliyun repository and GitHub Releases; they are **not** on Maven Central yet.

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>schemacrawler-impala</artifactId>
    <version>3.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

```groovy
implementation 'io.github.easy4j:schemacrawler-impala:3.0.x.x.20260630-SNAPSHOT'
```

Add your Impala JDBC driver jar (e.g. from Cloudera) to the classpath as well — it is intentionally not
bundled.

## 6. Quick Start

```bash
# List tables in the "default" database
./sc.sh -server=impala -database=default -host impala-host -port 21050 \
        -infolevel=standard -command=list
```

or programmatically:

```java
DatabaseConnectorRegistry registry = DatabaseConnectorRegistry.getDatabaseConnectorRegistry();
System.out.println(registry.hasDatabaseSystemIdentifier("impala")); // true

Connection connection = DriverManager.getConnection(
        "jdbc:hive2://impala-host:21050/default;auth=noSasl", "", "");
```

Expected result: `hasDatabaseSystemIdentifier("impala")` returns `true` and SchemaCrawler connects through
the Impala JDBC driver (the `;auth=noSasl` style is the Impala JDBC convention shown by the integration
test).

## 7. Configuration

Defaults live in `schemacrawler-impala.config.properties`:

| Property   | Default       | Meaning                   |
| :--------- | :------------ | :------------------------ |
| `host`     | `localhost`   | Impala daemon host        |
| `port`     | `10000`       | Impala JDBC port          |
| `database` | `default`     | Database name             |
| `url`      | `jdbc:hive2://${host}:${port}/${database}` | JDBC URL template |

Command-line options (`ImpalaDatabaseConnector.getHelpCommand()`): `-host`, `-port`, `-database`, `-user`,
`-password`.

## 8. Core Usage / API

| Type                                         | Role                                                             |
| :------------------------------------------- | :--------------------------------------------------------------- |
| `schemacrawler.server.impala.ImpalaDatabaseConnector` | Registers server type `impala`, loads the Impala JDBC driver, provides help and URL matching |

## 9. Testing & Build

```bash
./mvnw clean verify
```

- `TestBundledDistributions` verifies the plug-in is registered (`registry.hasDatabaseSystemIdentifier("impala")`).
- `ImpalaTest` (in `src/test`) is an integration test that requires a live Impala daemon; it is not part of
  the default unit-test run.
- JaCoCo is configured with a line-coverage rule of 90% (`haltOnFailure=false`).

## 10. Versioning & Branches

| Branch        | JDK  | Version pattern | Maintenance                          |
| :------------ | :--- | :-------------- | :----------------------------------- |
| `feature/1.0.x` | 8    | `1.0.x.*`       | Current branch                       |
| `feature/2.0.x` | 17   | `2.0.x.*`       | JDK 17 line                          |
| `feature/3.0.x` | 21   | `3.0.x.*`       | JDK 21 line                          |

Artifacts are distributed via the aliyun Maven repository and GitHub Releases. Use the branch matching your
JDK baseline.

## 11. Contributing & License

Contributions are welcome — especially the missing `impala.information_schema` SQL views and fixing the help
text. Please open an issue before larger changes.

This project is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
