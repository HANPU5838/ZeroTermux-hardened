# ZeroTermux 加固版（ZeroTermux Hardened）

> ## ⚠️ 原始作者声明 / Attribution
>
> 本项目是 **第三方衍生构建**，不是官方发布。
>
> - **原作者与原项目：[hanxinhao000/ZeroTermux](https://github.com/hanxinhao000/ZeroTermux)**（作者 **hanxinhao000**）
> - 上游地址：<https://github.com/hanxinhao000/ZeroTermux>
> - 本项目（`HANPU5838/ZeroTermux-hardened`）仅在上游源码基础上做**构建工具链现代化、依赖升级与打包整理**，不改变项目归属。
> - 原项目采用 **GPLv3 only** 许可证（含若干例外条款），本项目遵循同一许可证，详见 [LICENSE.md](LICENSE.md)。
> - 上游项目相关的问题与反馈请提交到[原仓库](https://github.com/hanxinhao000/ZeroTermux)，**不要打扰原作者**。
> - 本项目维护者主页：**[hanpuhan.cn](https://hanpuhan.cn)**

---

## 📥 下载安装包（APK）

到 **[Releases](../../releases)** 页下载，均为公开直链，无需登录：

| 文件 | 版本 | 大小 | 说明 |
| --- | --- | --- | --- |
| `ZeroTermux-0.118.3.69-release_arm64-v8a.apk` | `0.118.3.69`（versionCode 308） | ≈76 MB | 主程序，**仅 arm64-v8a** |
| `Termux-API-0.53.0-zero.14.apk` | `0.53.0-zero.14`（versionCode 1016） | ≈7 MB | 配套的 Termux:API，无原生库（全架构通用） |

- **包名**：`com.termux`（主程序）/ `com.termux.api`（API 组件）
- **SHA-256**：
  - 主程序：`3a771663e086dffa7bf68bc5525915fff70960cb2bbf2b26df32d803bac12535`
  - API 组件：`ce7c7a2455737fade160622a8ecbfa73417e2371e3f7e28cb07497f3452ec12c`
- **签名**：与上游分发版**同一证书**（`phone.jks`），因此可以**直接覆盖安装**已有的 ZeroTermux/Termux，无需卸载，数据不丢。
- **系统要求**：Android 7.0+（minSdk 23），targetSdk 28（Termux 生态兼容性所需）。

### 安装提示

1. 允许浏览器/文件管理器「安装未知来源应用」；
2. 先装主程序，再装 Termux:API（两者签名一致，可共存更新）；
3. Termux:API 自带无障碍服务（`com.termux.api/.apis.TermuxAccessibilityService`），如需相关能力可到系统「设置 → 无障碍」中手动开启；
4. 首次启动会解压 bootstrap，请耐心等待，**不要中途杀进程**；
5. 若已装官方 Termux 且签名不同，需先卸载（数据会丢，请先备份 `~`）。

### 🔗 相关链接

| 用途 | 地址 |
| --- | --- |
| 维护者主页（个人网站） | **<https://hanpuhan.cn>** |
| 本仓库（加固版源码） | <https://github.com/HANPU5838/ZeroTermux-hardened> |
| 下载页（Releases） | <https://github.com/HANPU5838/ZeroTermux-hardened/releases> |
| 上游原项目（原作者） | <https://github.com/hanxinhao000/ZeroTermux> |

---

## 这个「加固版」做了什么

在上游 ZeroTermux 源码基础上，完成了一轮构建体系与依赖的现代化（对应源码版本 `0.118.3.69`）：

- **构建工具链**：Gradle **9.9.1**、AGP **9.5.1**、JDK 21、compileSdk **37**、NDK **r30**、Build-Tools **37.0.0**
- **依赖全面升级**：androidx（core 1.19.1 / appcompat 1.8.0 / material 1.14.0 / lifecycle 2.11.0）、Kotlin 协程 1.11、Gson **2.15**、Guava **33.8.0**、jsoup **1.24.1**、Glide **5.0.9**、libsu **6.0.0**、libaums **0.9.0**、commons-fileupload **1.6.0**、log4j 1.2.17 → **reload4j 1.2.27 + slf4j 2.0.20（修复 CVE）** 等
- **内置用户态**：bootstrap 升级到上游 **2026.11.08-r1**（每周滚动）
- **工程化**：引入 Gradle 版本目录 `gradle/libs.versions.toml`，依赖版本集中管理
- **CI**：GitHub Actions 托管 Runner 编译（`.github/workflows/build.yml`），可手动触发、可选 ABI
- **兼容性保留项**（有明确原因）：targetSdk 28、OkHttp 4.12、XXPermissions 11.5 等

完整变更清单见仓库内 [`升级说明-0.118.3.69.md`](升级说明-0.118.3.69.md)。
### 用 GitHub Actions 自行编译

仓库已内置工作流 `.github/workflows/build.yml`：**Actions → Build APK → Run workflow**，可选 `arch`（默认 `arm64`）。构建完成后在该次运行页面底部 **Artifacts** 下载 APK；推送 `v*` tag 会自动构建并挂到 Release。

```bash
gh workflow run build.yml -R HANPU5838/ZeroTermux-hardened -f arch=arm64
```


### 版本对照

| 项 | 值 |
| --- | --- |
| 仓库源码 / Release 安装包 | `0.118.3.69`（versionCode 308） |
| Termux:API | `0.53.0-zero.14`（versionCode 1016） |

> 源码版本与安装包版本保持一致；发布时推送 `v*` tag 会由 CI 自动构建并挂到对应 Release。

---

## 从源码构建

> 上游构建机为 x86_64 工具链（box64/NDK 环境），命令仅供熟悉 Android 构建者参考。

```bash
# 需要 JDK 21 + Android SDK（compileSdk 37）+ NDK r30
gradle :app:assembleRelease -Darch=arm64
```

签名配置见 `app/build.gradle`（使用仓库根的 `phone.jks`，与上游同一证书；可用环境变量 `KEY_ALIAS` / `KEY_PASSWORD` / `STORE_PASSWORD` 覆盖）。

---

## 许可证

本项目遵循上游的 **GPLv3 only** 许可证及例外条款，见 [LICENSE.md](LICENSE.md)。
`terminal-emulator` / `terminal-view` 模块另含 Apache-2.0 代码，详见许可证文件中的 Exceptions 说明。
上游版权归 **hanxinhao000** 及其贡献者所有。

---

## 免责声明

- 本项目为**非官方**第三方构建，与原作者无隶属关系；使用风险自负。
- 本项目只做构建工具链与依赖升级，**未新增业务功能**；是否收集数据请以源码为准自行审阅。
- 请勿将本项目的安装包问题反馈给上游作者。
- 若上游作者认为本仓库有不妥之处，可提出，会立即配合调整或下架。
